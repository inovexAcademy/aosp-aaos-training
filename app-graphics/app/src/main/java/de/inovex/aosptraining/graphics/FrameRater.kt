package de.inovex.aosptraining.graphics

import android.os.SystemClock

class FrameRater {
    private var frames = 0
    private var lastTime = -1L
    var fps = 0f
        private set

    fun addNewFrame() {
        val t = SystemClock.elapsedRealtimeNanos()
        if (lastTime == -1L) {
            lastTime = t
            frames = 0
        } else {
            frames += 1
            val delta = t - lastTime
            if (delta > 1_000_000_000) {
                fps = frames.toFloat() * 1_000_000_000 / delta
                lastTime = t
                frames = 0
            }
        }
    }
}