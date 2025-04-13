package de.inovex.aosptraining.graphics

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AtomicValueAndLatchTest {
    @Test
    fun simpleIntegerAsValue() {
        val a = AtomicValueAndLatch<Int>()
        a.setValue(23)
        assertEquals(23, a.waitAndGet())
        // You can also call it a second time and it returns the same result
        assertEquals(23, a.waitAndGet())
    }

    @Test
    fun nullAsValue() {
        val a = AtomicValueAndLatch<Int?>()
        a.setValue(null)
        assertEquals(null, a.waitAndGet())
    }

    @Test
    fun error() {
        val a = AtomicValueAndLatch<Int>()
        a.setError(IOException(""))
        assertThrows(IOException::class.java) { a.waitAndGet() }
    }

    @Test
    fun testWaitAndGetWithTimeout() {
        val a = AtomicValueAndLatch<Int?>()
        assertEquals(null, a.waitAndGet(1, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testWaitAndGetWithValue() {
        val a = AtomicValueAndLatch<Int?>()
        a.setValue(42)
        assertEquals(42, a.waitAndGet(1, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testWaitAndGetWithError() {
        val a = AtomicValueAndLatch<Int?>()
        a.setError(IOException(""))
        assertThrows(IOException::class.java) { a.waitAndGet(1, TimeUnit.MILLISECONDS) }
    }
}