package de.inovex.aosptraining.graphics

import android.util.Log
import java.nio.ByteBuffer
import java.util.*

/**
 * Just another ByteBuffer pool
 *
 * Notes
 * - Implementation is thread safe!
 */
class ByteBufferPool(private val maxFreeBuffers: Int = 30) {
    private val lock = Object()
    private val freeBuffers = LinkedList<ByteBuffer>()
    private val freeBuffersIdentityHashMap = HashSet<Int>()
    private val allBuffers = LinkedHashMap<Int, ByteBuffer>()
    private var stat = Stat()

    init {
        if (maxFreeBuffers <= 0)
            throw IllegalArgumentException("maxFreeBuffers cannot be negative or null!")
    }

    fun retrieveBuffer(size: Int): ByteBuffer {
        synchronized(lock) {
            // First try to satisfy the request from the free Buffers
            if (freeBuffers.size > 0) {
                for (buf in freeBuffers)
                    if (buf.remaining() >= size) {
                        // Buffer found
                        freeBuffers.remove(buf)
                        freeBuffersIdentityHashMap.remove(System.identityHashCode(buf))
                        stat.freeBuffers--
                        return buf
                    }
            }
            // There are no free buffers with this size. Allocated a new one
            return allocLocked(size)
        }
    }

    fun returnBuffer(byteBuffer: ByteBuffer) {
        // The common case is freeing of multiple buffers at once. So this function can
        // safely take the overhead of creating a unnecessary list.
        returnBuffers(listOf(byteBuffer))
    }

    fun returnBuffers(byteBuffers: List<ByteBuffer>) {
        synchronized(lock) {
            for (byteBuffer in byteBuffers) {
                val identityHashCode = System.identityHashCode(byteBuffer)
                if (freeBuffersIdentityHashMap.contains(identityHashCode))
                    throw IllegalArgumentException("Double free detected!")
                if (!allBuffers.containsKey(identityHashCode))
                    throw IllegalArgumentException("ByteBuffer was not allocated!")
                byteBuffer.clear()
                freeBuffers.addLast(byteBuffer)
                freeBuffersIdentityHashMap.add(identityHashCode)
                stat.freeBuffers += 1
            }

            sortFreeBuffersLocked()
            trimFreeBuffersLocked()
        }
    }

    private fun sortFreeBuffersLocked() {
        freeBuffers.sortWith { p0, p1 ->
            p0!!.capacity() - p1!!.capacity()
        }
    }

    private fun trimFreeBuffersLocked() {
        // Free the smallest buffer first
        while (freeBuffers.size > maxFreeBuffers) {
            freeBuffers.pollFirst()?.let { byteBuffer ->
                stat.freeBuffers--
                stat.bufferSizeInBytes -= byteBuffer.capacity()
                stat.bufferCount--
                val identityHashCode = System.identityHashCode(byteBuffer)

                freeBuffersIdentityHashMap.remove(identityHashCode)
            }
        }
        assert(freeBuffersIdentityHashMap.size == freeBuffers.size)
    }

    fun dumpFreeBuffers() {
        synchronized(lock) {
            Log.d("pool", "FreeBuffers:")
            for ((i, buf) in freeBuffers.withIndex()) {
                Log.d("pool", " $i: $buf")
            }
        }
    }

    private fun allocLocked(size: Int): ByteBuffer {
        val byteBuffer = ByteBuffer.allocate(size)
        val identityHashCode = System.identityHashCode(byteBuffer)
        allBuffers[identityHashCode] = byteBuffer
        stat.bufferCount += 1
        stat.bufferSizeInBytes += size
        return byteBuffer
    }

    fun getStat(): Stat {
        synchronized(lock) {
            return stat.copy()
        }
    }

    data class Stat(
        var bufferCount: Int = 0,
        var bufferSizeInBytes: Int = 0,
        var freeBuffers: Int = 0  // TODO is actually the length of the free list
    )
}