package de.inovex.aosptraining.graphics

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.util.*

@RunWith(AndroidJUnit4::class)
class ByteBufferPoolTest {
    @Test
    fun deallocArgumentException() {
        val pool = ByteBufferPool()
        assertThrows(IllegalArgumentException::class.java) {
            pool.returnBuffer(ByteBuffer.allocate(5))
        }
    }

    @Test
    fun doubleFree() {
        val pool = ByteBufferPool()
        val buf = pool.retrieveBuffer(50)
        pool.returnBuffer(buf)
        assertThrows(IllegalArgumentException::class.java) {
            pool.returnBuffer(buf)
        }
    }

    @Test
    fun testRetrieveAndReturn() {
        val pool = ByteBufferPool()
        val buf0 = pool.retrieveBuffer(100)
        pool.getStat().let { stat ->
            assertEquals(1, stat.bufferCount)
            assertEquals(100, stat.bufferSizeInBytes)
            assertEquals(0, stat.freeBuffers)
        }

        pool.retrieveBuffer(50)
        pool.getStat().let { stat ->
            assertEquals(2, stat.bufferCount)
            assertEquals(150, stat.bufferSizeInBytes)
            assertEquals(0, stat.freeBuffers)
        }

        pool.returnBuffer(buf0)
        assertEquals(1, pool.getStat().freeBuffers)

        // There is only one free buffer. The cache *must* return this buffer
        val buf3 = pool.retrieveBuffer(90)
        assertEquals(buf0, buf3)
        assertEquals(0, pool.getStat().freeBuffers)
    }

    @Test
    fun testRetrieveReturnsTheSmallestBuffer() {
        val pool = ByteBufferPool()
        val bufferBig = pool.retrieveBuffer(100)
        val bufferSmall = pool.retrieveBuffer(50)
        pool.getStat().let { stat ->
            assertEquals(2, stat.bufferCount)
            assertEquals(150, stat.bufferSizeInBytes)
        }
        pool.returnBuffer(bufferBig)
        pool.returnBuffer(bufferSmall)

        val buf0 = pool.retrieveBuffer(40)
        assertEquals(buf0, bufferSmall)
        pool.returnBuffer(buf0)

        val buf1 = pool.retrieveBuffer(90)
        assertEquals(buf1, bufferBig)
        pool.returnBuffer(buf1)

        pool.dumpFreeBuffers()
    }

    @Test
    fun testReturnMultipleBuffers() {
        val pool = ByteBufferPool()
        val buf1 = pool.retrieveBuffer(50)
        pool.retrieveBuffer(51)
        val buf3 = pool.retrieveBuffer(52)
        pool.getStat().let { stat ->
            assertEquals(0, stat.freeBuffers)
            assertEquals(3, stat.bufferCount)
            assertEquals(153, stat.bufferSizeInBytes)
        }

        pool.returnBuffers(listOf(buf1, buf3))
        pool.getStat().let { stat ->
            assertEquals(2, stat.freeBuffers)
            assertEquals(3, stat.bufferCount)
            assertEquals(153, stat.bufferSizeInBytes)
        }
        pool.dumpFreeBuffers()
    }

    @Test
    fun testTrim() {
        val pool = ByteBufferPool(5)
        val buffers = LinkedList<ByteBuffer>()
        for (i in 0 until 10) {
            buffers.add(pool.retrieveBuffer(100))
        }
        pool.getStat().let { stat ->
            assertEquals(0, stat.freeBuffers)
            assertEquals(10, stat.bufferCount)
            assertEquals(1_000, stat.bufferSizeInBytes)
        }

        // Free five buffers
        for (i in 0..4) {
            pool.returnBuffer(buffers[i])
        }
        pool.getStat().let { stat ->
            assertEquals(5, stat.freeBuffers)
            assertEquals(10, stat.bufferCount)
            assertEquals(1_000, stat.bufferSizeInBytes)
        }

        // Free one additional buffer. Now trim gets invoked
        pool.returnBuffer(buffers[5])
        pool.getStat().let { stat ->
            assertEquals(5, stat.freeBuffers)
            assertEquals(9, stat.bufferCount)
            assertEquals(900, stat.bufferSizeInBytes)
        }
    }
}