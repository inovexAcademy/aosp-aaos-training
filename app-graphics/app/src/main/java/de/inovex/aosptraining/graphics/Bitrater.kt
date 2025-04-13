package de.inovex.aosptraining.graphics

/**
 * BitRater – to measure the bitrate of a video stream
 *
 * This class calculates the bitrate based the code buffer sizes. It counts all buffers from
 * a key frame to the next. This is more stable than relying on the wall clock time, because
 * of jitter in buffer generation.
 */
class BitRater {
    private var bytes = 0
    private var frames = 0
    var currentbitrateInKBits = -1.0f
        private set

    fun update(size_of_frame: Int, isKeyFrame: Boolean) {
        if (isKeyFrame) {
            currentbitrateInKBits = bytes / 1024f

            bytes = 0
            frames = 0
        }
        bytes += size_of_frame
        frames += 1
    }
}
