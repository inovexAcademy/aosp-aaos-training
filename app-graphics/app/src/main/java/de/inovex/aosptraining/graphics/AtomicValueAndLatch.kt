package de.inovex.aosptraining.graphics

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Combination of a AtomicReference and a CountDownLatch
 *
 * Very useful for testing callback driven APIs.
 *
 * TODO:
 * - Handle InterruptException
 */
class AtomicValueAndLatch<T> {
    // TODO Mostly the AtomicReference is not needed. A simple variable should be sufficient
    private val value = AtomicReference<T>(null)
    private val error = AtomicReference<Throwable>(null)
    private var isError = false
    private var latch = CountDownLatch(1)

    fun setValue(v: T) {
        value.set(v)
        isError = false
        latch.countDown()
    }

    fun setError(e: Throwable) {
        error.set(e)
        isError = true
        latch.countDown()
    }

    fun waitAndGet(): T {
        latch.await()
        if (isError)
            throw error.get()
        return value.get()
    }

    fun waitAndGet(timeout: Long, timeunit: TimeUnit): T? {
        val countIsZero = latch.await(timeout, timeunit)
        if (!countIsZero)
            return null
        if (isError)
            throw error.get()
        return value.get()
    }
}