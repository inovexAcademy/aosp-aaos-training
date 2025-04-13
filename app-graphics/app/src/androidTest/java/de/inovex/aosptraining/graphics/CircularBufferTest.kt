package de.inovex.aosptraining.graphics

import android.media.MediaCodec
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CircularBufferTest {
    @Test
    fun testStatistic() {
        CircularBuffer(30, object : CircularBuffer.Callback {}).use { circularBuffer ->
            val info = MediaCodec.BufferInfo()
            info.set(0, 5, 0, MediaCodec.BUFFER_FLAG_KEY_FRAME)
            circularBuffer.add(ByteBuffer.allocate(5), info)
            assertEquals(5, circularBuffer.calcStats().totalBufferSizeInBytes)
            assertEquals(1, circularBuffer.calcStats().bufferCount)

            val info2 = MediaCodec.BufferInfo()
            info.set(0, 10, 100, 0)
            circularBuffer.add(ByteBuffer.allocate(10), info2)
            assertEquals(15, circularBuffer.calcStats().totalBufferSizeInBytes)
            assertEquals(2, circularBuffer.calcStats().bufferCount)
        }
    }

    class BufferGenerator {
        private val keyFrameIntervalInFrames = 10 // how many frames before the next key frame
        private var counter = 0

        fun next(): CircularBuffer.Buffer {
            val info = MediaCodec.BufferInfo()
            var flags = 0
            var bufferSize = 400
            val timeInUs = counter * 10_000L // Advance 10ms

            if (counter % keyFrameIntervalInFrames == 0) {
                flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                bufferSize = 1300 // Keyframes are bigger than p-frames
            }
            info.set(0, bufferSize, timeInUs, flags)

            counter++

            return CircularBuffer.Buffer(ByteBuffer.allocate(bufferSize), info)
        }
    }

    @Test
    fun testRemoving() {
        val gen = BufferGenerator()

        CircularBuffer(30, object : CircularBuffer.Callback {}).use { circularBuffer ->
            assertEquals(0, circularBuffer.calcStats().bufferCount)

            for (i in 0 until 30)
                circularBuffer.add(gen.next())
            assertEquals(30, circularBuffer.calcStats().bufferCount)

            // Adding a single additional buffer, removes the first keyframe and all following
            // partial frames. This adds a new keyframe:
            circularBuffer.add(gen.next())

            assertEquals(21, circularBuffer.calcStats().bufferCount)

            // Add the additional nine partial frames
            for (i in 0 until 9)
                circularBuffer.add(gen.next())

            // Now the queue is full again
            assertEquals(30, circularBuffer.calcStats().bufferCount)
        }
    }

    @Test
    fun testMuxing() {
        val gen = BufferGenerator()

        val bufferPipe = SynchronousQueue<CircularBuffer.Buffer>()

        CircularBuffer(30, object : CircularBuffer.Callback {
            override fun onBuffer(buffer: CircularBuffer.Buffer) {
                // Waits until the receiver pulls the element out of the pipe
                bufferPipe.put(buffer)
            }
        }).use { circularBuffer ->
            // Insert buffers: two keyframes and 18 partial frames
            for (i in 0 until 20)
                circularBuffer.add(gen.next())
            assertEquals(20, circularBuffer.calcStats().bufferCount)
            assertEquals(CircularBuffer.State.BUFFERING, circularBuffer.getState())

            // This starts a background thread to drain the buffer
            circularBuffer.startMuxing()
            assertEquals(CircularBuffer.State.DRAINING, circularBuffer.getState())

            // Wait for the first buffer. It's a key frame
            bufferPipe.poll(1, TimeUnit.SECONDS)!!.let { buffer ->
                assertEquals(MediaCodec.BUFFER_FLAG_KEY_FRAME, buffer.info.flags)
                assertEquals(0L, buffer.info.presentationTimeUs)
                assertEquals(1300, buffer.info.size)
            }

            // Wait for the second buffer in the queue. It's a partial frame
            bufferPipe.poll(1, TimeUnit.SECONDS)!!.let { buffer ->
                assertEquals(0, buffer.info.flags)
                assertEquals(10_000L, buffer.info.presentationTimeUs)
                assertEquals(400, buffer.info.size)
            }

            // Drain additional 8 buffers
            for (i in 1 until 8)
                bufferPipe.poll(1, TimeUnit.SECONDS)!!

            // Now only 10 buffers are left in the queue.
            // TODO: this is racy
            assertEquals(10, circularBuffer.calcStats().bufferCount)

            // While the thread is draining the internal queue, it's possible to add new buffers
            assertEquals(CircularBuffer.State.DRAINING, circularBuffer.getState())
            for (i in 0 until 10)
                circularBuffer.add(gen.next())
            assertEquals(20, circularBuffer.calcStats().bufferCount)

            // Now drain all buffers from the queue, so the circular buffer switches into
            // the pass through mode
            for (i in 9..29) {
                assertEquals(10_000L * i, bufferPipe.poll(1, TimeUnit.SECONDS)!!.info.presentationTimeUs)
            }
            assertEquals(null, bufferPipe.peek())
            assertEquals(CircularBuffer.State.PASS_TROUGH, circularBuffer.getState())
        }
    }

    @Test
    fun testStopMuxingInPassThroughMode() {
        val gen = BufferGenerator()

        val bufferPipe = SynchronousQueue<CircularBuffer.Buffer>()

        CircularBuffer(30, object : CircularBuffer.Callback {
            override fun onBuffer(buffer: CircularBuffer.Buffer) {
                // Waits until the receiver pulls the element out of the pipe
                bufferPipe.put(buffer)
            }
        }).use { circularBuffer ->
            for (c in 0..10) { // Do this multiple times on the same object. Little stress test.
                for (i in 0 until 10)
                    circularBuffer.add(gen.next())

                assertEquals(CircularBuffer.State.BUFFERING, circularBuffer.getState())

                // This starts a background thread to drain the buffer
                circularBuffer.startMuxing()
                assertEquals(CircularBuffer.State.DRAINING, circularBuffer.getState())

                // Drain all buffers from the queue
                for (i in 0 until 10) {
                    bufferPipe.take()
                }
                // Busying waiting for the thread to finish
                while (true) {
                    Thread.sleep(100)
                    if (circularBuffer.getState() == CircularBuffer.State.PASS_TROUGH)
                        break
                }

                // Now stop the muxing process
                circularBuffer.stopMuxing()
                assertEquals(CircularBuffer.State.BUFFERING, circularBuffer.getState())
            }
        }
    }

    @Test
    fun testStopMuxingInDrainingMode() {
        val gen = BufferGenerator()

        val bufferPipe = SynchronousQueue<CircularBuffer.Buffer>()
        CircularBuffer(30, object : CircularBuffer.Callback {
            override fun onBuffer(buffer: CircularBuffer.Buffer) {
                // Waits until the receiver pulls the element out of the pipe
                bufferPipe.put(buffer)
            }
        }).use { circularBuffer ->
            for (i in 0 until 5)
                circularBuffer.add(gen.next())

            for (c in 0..10) { // Do this multiple times on the same object. Little stress test.
                for (i in 0 until 10)
                    circularBuffer.add(gen.next())
                assertEquals(15, circularBuffer.calcStats().bufferCount)

                assertEquals(CircularBuffer.State.BUFFERING, circularBuffer.getState())

                // This starts a background thread to drain the buffer
                circularBuffer.startMuxing()
                assertEquals(CircularBuffer.State.DRAINING, circularBuffer.getState())

                // Drain ten buffers
                for (i in 0 until 9) {
                    bufferPipe.take()
                }
                assertEquals(CircularBuffer.State.DRAINING, circularBuffer.getState())
                // There are still five buffers.
                // TODO This code actually needs a peekable Synchronous Queue to avoid this busy looping
                // If the code is too fast, there are still 6 buffers in the queue. So have a
                // busy loop.
                while (true) {
                    // There to busy wait. Otherwise the bufferCount is maybe still 6.
                    if (circularBuffer.calcStats().bufferCount == 5)
                        break
                    Thread.sleep(100)
                }

                // Now stop the muxing
                val t = Thread {
                    Thread.sleep(200)
                    // The internal draining thread still blocks on our pipe. Get one additional
                    // to let it make forward progress. Otherwise the call to stopMuxing() will
                    // never return.
                    bufferPipe.take()
                }
                t.start()
                circularBuffer.stopMuxing()
                t.join()

                assertEquals(5, circularBuffer.calcStats().bufferCount)
                assertEquals(CircularBuffer.State.BUFFERING, circularBuffer.getState())
            }
        }
    }

    @Test
    fun testPartialFramesAreRemovedAfterStoppingInModeDraining() {
        // When the Muxing is stopped and the buffer is in draining there are maybe still
        // partial frames in the queue. These must be removed, because the reference/key frame
        // was already written out. These partial frames are not useful for the next muxer.
        val gen = BufferGenerator()

        val bufferPipe = SynchronousQueue<CircularBuffer.Buffer>()
        CircularBuffer(30, object : CircularBuffer.Callback {
            override fun onBuffer(buffer: CircularBuffer.Buffer) {
                // Waits until the receiver pulls the element out of the pipe
                bufferPipe.put(buffer)
            }
        }).use { circularBuffer ->
            // Add 15 elements to the buffer. The first and tenth frame is a key frame
            for (i in 0 until 15)
                circularBuffer.add(gen.next())
            assertEquals(15, circularBuffer.calcStats().bufferCount)

            // Now drain five frames from the buffer
            circularBuffer.startMuxing()

            // First take 4 buffers
            for (i in 0 until 4) {
                bufferPipe.take()
            }
            // Then wait until the internal thread as removed the fifth buffer.
            while (true) {
                if (circularBuffer.calcStats().bufferCount == 10)
                    break
                Thread.sleep(100)
            }
            // Start a thread to take the last buffer
            val t = Thread {
                Thread.sleep(200)
                bufferPipe.take()
            }
            t.start()
            circularBuffer.stopMuxing()
            t.join()
            // We only have drained five buffers from the queue, but the stopMuxing() has removed
            // additional five partial frames from the queue.
            assertEquals(5, circularBuffer.calcStats().bufferCount)

            // Now add new frames from the encoder to the queue.
            // The next five frames are partial frames. These are added to the queue, because
            // the corresponding keyframe is *still* in the queue.
            for (i in 0 until 5)
                circularBuffer.add(gen.next())
            assertEquals(10, circularBuffer.calcStats().bufferCount)
        }
    }

    @Test
    fun testPartialFramesAreRemovedAfterStoppingInModePassThrough() {
        // When the muxing is stopped and the buffer is in pass trough mode the encoder may first
        // push additional partial frames. These must not be saved in the queue. The ciruclar
        // buffer has to wait for the next key frame.
        val gen = BufferGenerator()

        val bufferPipe = SynchronousQueue<CircularBuffer.Buffer>()
        CircularBuffer(30, object : CircularBuffer.Callback {
            override fun onBuffer(buffer: CircularBuffer.Buffer) {
                // Waits until the receiver pulls the element out of the pipe
                bufferPipe.put(buffer)
            }
        }).use { circularBuffer ->
            // Add 15 elements to the buffer. The first and tenth frame is a key frame
            for (i in 0 until 15)
                circularBuffer.add(gen.next())

            // Now drain all elements from the queue to go into Pass trough Mode
            circularBuffer.startMuxing()
            for (i in 0 until 15)
                bufferPipe.take()

            assertEquals(0, circularBuffer.calcStats().bufferCount)
            // TODO This is racy
            assertEquals(CircularBuffer.State.PASS_TROUGH, circularBuffer.getState())

            circularBuffer.stopMuxing()
            assertEquals(CircularBuffer.State.BUFFERING, circularBuffer.getState())

            // Now add additional buffers from the encoder. The next five frames are partial frames
            // These should not be added to the queue, because the reference/key frame is not in
            // the queue
            for (i in 0 until 5)
                circularBuffer.add(gen.next())
            assertEquals(0, circularBuffer.calcStats().bufferCount)

            // The next then frames are should be saved, because the first frame is a key frame.
            for (i in 0 until 10)
                circularBuffer.add(gen.next())
            assertEquals(10, circularBuffer.calcStats().bufferCount)
        }
    }

    @Test
    fun testCloseReturnsBuffersToPool() {
        val gen = BufferGenerator()
        val pool = ByteBufferPool(30)

        CircularBuffer(30, object : CircularBuffer.Callback {}, pool).use { circularBuffer ->
            for (i in 0 until 10)
                circularBuffer.add(gen.next())

            pool.getStat().let { stat ->
                assertEquals(0, stat.freeBuffers)
                assertEquals(10, stat.bufferCount)
            }
        }

        pool.getStat().let { stat ->
            assertEquals(10, stat.freeBuffers)
            assertEquals(10, stat.bufferCount)
        }
    }

    @Test
    fun testRound() {
        assertEquals(10, roundUpTo(9, 5))
        assertEquals(10, roundUpTo(10, 5))
        assertEquals(15, roundUpTo(11, 5))
        assertEquals(15, roundUpTo(12, 5))
        assertEquals(15, roundUpTo(13, 5))
        assertEquals(15, roundUpTo(14, 5))
        assertEquals(15, roundUpTo(15, 5))
        assertEquals(20, roundUpTo(16, 5))
    }
}
