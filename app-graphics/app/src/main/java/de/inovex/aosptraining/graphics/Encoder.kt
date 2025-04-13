package de.inovex.aosptraining.graphics

import android.media.*
import android.os.Bundle
import android.view.Surface
import java.io.Closeable
import java.lang.RuntimeException
import java.nio.ByteBuffer

class Encoder private constructor(private val codec : MediaCodec) : Closeable {
    var callback : Callback? = null
    var mediaFormat : MediaFormat? = null
        private set
    var surface : Surface
        private set
    init {
        codec.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(p0: MediaCodec, index: Int) {
                throw RuntimeException("Should not be called")
            }

            override fun onOutputBufferAvailable(
                p0: MediaCodec,
                index: Int,
                info: MediaCodec.BufferInfo
            ) {
                callback?.onOutputBufferAvailable(this@Encoder, index, info)
            }

            override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {
                throw RuntimeException("Not implemented")
            }

            override fun onOutputFormatChanged(c: MediaCodec, m: MediaFormat) {
                mediaFormat = m
                callback?.onOutputFormatChanged(this@Encoder, m)
            }
        })

        surface = codec.createInputSurface()
        codec.start()
    }

    companion object {
        fun createEncoder(width: Int, height : Int, fps : Int, bitrate: Int) : Encoder? {
            val (name, videoFormat) = findEncoder(width, height, fps, bitrate) ?: return null

            val codec = MediaCodec.createByCodecName(name)

            val config = Bundle()
            config.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrate)
            codec.setParameters(config)

            codec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            return Encoder(codec)
        }
    }

    fun getOutputBuffer(index : Int) : ByteBuffer? {
        return codec.getOutputBuffer(index)
    }

    fun releaseOutputBuffer(index: Int) {
        codec.releaseOutputBuffer(index, true)
    }

    override fun close() {
        codec.stop()
        codec.reset()
    }

    interface Callback {
        fun onOutputBufferAvailable(encoder: Encoder, index: Int, info: MediaCodec.BufferInfo) {}
        fun onOutputFormatChanged(encoder: Encoder, mediaFormat: MediaFormat) {}
    }
}

fun findEncoder(width : Int, height : Int, fps : Int, bitrate: Int) : Pair<String, MediaFormat>? {
    val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    val videoFormat = MediaFormat.createVideoFormat(
        MediaFormat.MIMETYPE_VIDEO_AVC,  // h.264
        width,
        height,
    )
    videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
    videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
    videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)

    val encoderString = codecs.findEncoderForFormat(videoFormat)
    if (encoderString.isEmpty())
        return null
    return Pair(encoderString, videoFormat)
}