package de.inovex.aosptraining.graphics

import android.media.MediaCodec
import android.util.Log

/**
 * Fixup for the h.264 encoder of the Android emulator
 *
 * Somehow the h.264 encoder of the emulator only marks the first frame as a key
 * frame. All subsequent frames have not the key frame attribute, even so every 30
 * frames a big frame is generated. This big frame is mostly a key frame.
 */
class EmulatorKeyFrameFixUp(private val fps: Int, private val iFrameIntervalInSecs: Int) {
    companion object {
        const val TAG = "EmulatorKeyFrameFixUp"
    }
    private var counter = 0L

    fun setKeyFrameInInfo(info: MediaCodec.BufferInfo) {
        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0)
            return // Ignore codec config frames

        if (counter % (fps * iFrameIntervalInSecs) == 0L) {
            val newFlags = info.flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
            info.set(info.offset, info.size, info.presentationTimeUs, newFlags)
            Log.w(TAG, "Setting keyframe on frame: counter=$counter size=${info.size}")
        }

        counter++
    }
}
