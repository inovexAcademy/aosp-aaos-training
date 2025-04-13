package de.inovex.aosptraining.graphics;

import android.media.MediaCodec
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CircularBuffer for video frames
 *
 * This is a special kind of ring buffer. It supports two modes. While the ring buffer is in
 * the mode buffering, it records and stores the video frames. If the buffer is full, old
 * frames, one keyframe and all following partial frames, are dropped. The user can start
 * the muxing process at any time. The ring then switches into the mode draining. In that mode
 * it writes all existing frames in the buffer to the muxer and also accepts new frames from the
 * encoder. After that it switches into the mode pass through. The ringer buffer is empty and all
 * new frames from the encoder are directly forwarded to the muxer.
 *
 * Internal invariants:
 * - The first buffer in the queue is always a key frame.
 */
class CircularBuffer(
    private val maxFrameCount: Int,
    private val callback: Callback,
    val pool: ByteBufferPool = ByteBufferPool()
) : Closeable {
    private val queue = LinkedList<Buffer>()
    private val lock = Object()
    private var state = State.BUFFERING
    private var thread: Thread? = null
    private var stopThread = AtomicBoolean(false)

    init {
        if (maxFrameCount <= 0)
            throw IllegalArgumentException("maxFrameCount cannot be zero or negative")
    }

    enum class State {
        BUFFERING,
        DRAINING,
        PASS_TROUGH,
    }

    private fun copyToQueueLocked(buffer: Buffer) {
        // TODO This queue cleanup does not work correctly, when the buffers arrive faster
        // than the muxer can write them. Not at real problem in my simple usecase.
        if (queue.size >= maxFrameCount) {
            // The queue is already full. Drop some old frames
            // Invariant: The first frame in the queue is always a key frame.
            assert(queue.peekFirst()!!.info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0)

            // Remove the first(=oldest) frame and all following non-key frames
            val byteBuffersToReturn = LinkedList<ByteBuffer>()
            byteBuffersToReturn.add(queue.removeFirst().buf) // Remove the oldest key frame
            // Remove all folliwing non-key frames until the next key frame.
            while (queue.size != 0) {
                if (queue.peekFirst()!!.info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0)
                    break
                byteBuffersToReturn.add(queue.removeFirst().buf)

            }
            pool.returnBuffers(byteBuffersToReturn)
        }
        val size = buffer.buf.remaining()
        val allocSize = roundUpTo(size, 1024)
        val newBuf = pool.retrieveBuffer(allocSize)
        newBuf.put(buffer.buf)  // This is the expensive copy operation
        newBuf.flip()
        assert(newBuf.position() == 0)
        assert(newBuf.remaining() == size)
        queue.addLast(Buffer(newBuf, buffer.info))
    }

    fun add(buf: ByteBuffer, info: MediaCodec.BufferInfo) {
        add(Buffer(buf, info))
    }

    // Function may block a bit longer, if data is directly forward to muxer
    fun add(buffer: Buffer) {
        if (buffer.info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0)
            return

        synchronized(lock) {
            if (state == State.BUFFERING) {
                if (queue.size == 0 && buffer.info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME == 0)
                    return // Drop partial frame if there is no corresponding key frame
                copyToQueueLocked(buffer)
                return
            } else if (state == State.DRAINING) {
                copyToQueueLocked(buffer)
                return
            }
            // The queue is in the pass trough mode. So the queue is empty and we should
            // directly forward the buffer to the muxer.
            assert(queue.size == 0)
        }
        callback.onBuffer(buffer)
    }

    // Start writing to the muxer.
    fun startMuxing() {
        synchronized(lock) {
            if (state != State.BUFFERING)
                return

            stopThread.set(false)
            thread = Thread(this::threadDrainBuffers)
            thread?.start()
            updateStateLocked(State.DRAINING)
        }
    }

    private fun threadDrainBuffers() {
        while (true) {
            var buffer: Buffer? = null
            // Acquire the lock and look at the queue state.
            synchronized(lock) {
                assert(state == State.DRAINING)

                if (stopThread.get())
                    return

                if (queue.size == 0) {
                    // Success. The thread has processed all buffers in the queue.
                    // Now switch the state into the PASS_THROUGH mode.
                    updateStateLocked(State.PASS_TROUGH)
                    return
                }
                // Take a single from the queue and forward it to the muxer
                buffer = queue.pollFirst()
            }
            // The lock is released. So the encoder can add buffers to the queue while the muxer
            // writes out the current buffer.
            callback.onBuffer(buffer!!)
            pool.returnBuffer(buffer!!.buf)
        }
    }

    fun stopMuxing() {
        // TODO This function is racy when other state changing functions are called in the
        // meantime. The class actually needs two nested locks to avoid that.
        synchronized(lock) {
            if (state == State.BUFFERING) {
                // In this state there is nothing to do. No thread was created.
                return
            }
            // In the state draining or pass trough we have to stop the thread
            // TODO why is stopping in the Pass trough mode needed?
            stopThread.set(true)
        }
        // Release the lock here, so the thread can make forward progress
        thread!!.join()
        synchronized(lock) {
            thread = null
            // When muxing is stopped it maybe be in the middle of partial frames.
            // These are invalid for the next muxing process, because the reference/key frame
            // is missing. Therefore remove all non-key frames at the front of the queue.
            // -> This keeps the invaraint: The first frame in the key is always a key-frame.
            while (queue.size > 0) {
                if (queue.peekFirst()!!.info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0)
                    break
                pool.returnBuffer(queue.removeFirst().buf)
            }
            updateStateLocked(State.BUFFERING)
        }
    }

    fun getState(): State {
        synchronized(lock) {
            return state
        }
    }

    fun calcStats(): Stats {
        synchronized(lock) {
            var totalBufferSize = 0L
            for (data in queue)
                totalBufferSize += data.buf.remaining()
            return Stats(totalBufferSize, queue.size)
        }
    }

    private fun updateStateLocked(s: State) {
        state = s
        callback.onStateChanged(state)
    }

    override fun close() {
        stopMuxing()
        synchronized(lock) {
            // Return all buffers to the pool
            pool.returnBuffers(queue.map { buffer -> buffer.buf })
            queue.clear()
        }
    }

    interface Callback {
        fun onStateChanged(state: State) {}
        fun onBuffer(buffer: Buffer) {}
    }

    class Buffer(val buf: ByteBuffer, val info: MediaCodec.BufferInfo)

    data class Stats(val totalBufferSizeInBytes: Long, val bufferCount: Int)
}

fun calcNeededFrames(fps: Int, iFrameIntervalInSecs: Int, minBufferedSecs: Int): Int {
    return iFrameIntervalInSecs * fps * (minBufferedSecs + 1)
}

fun roundUpTo(n: Int, base: Int): Int {
    assert(base > 0)
    assert(n >= 0)
    return kotlin.math.ceil(n.toDouble() / base).toInt() * base
}

