package de.inovex.aosptraining.graphics

import android.media.MediaCodec
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EncoderTest {
    companion object {
        private val handlerThread = HandlerThread("CameraTest")
        private var handler: Handler? = null

        @BeforeClass
        @JvmStatic
        fun setup() {
            handlerThread.start()
            handler = Handler(handlerThread.looper)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            handler = null
            handlerThread.quit()
        }
    }

    @Test
    fun findEncoder() {
        assertNotEquals(
            de.inovex.aosptraining.graphics.findEncoder(
                1920,
                1080,
                30,
                6_000_000
            ), null)
        assertNotEquals(
            de.inovex.aosptraining.graphics.findEncoder(
                640,
                480,
                30,
                700_000
            ), null)
    }

    @Test
    fun createEncoder() {
        val width = 640
        val height = 480
        Encoder.createEncoder(width, height, 30, 700_000)!!.use { encoder ->
            GlesImageWriter(encoder.surface).use { glesImageWriter ->
                val queue = ConcurrentLinkedDeque<Pair<Int, MediaCodec.BufferInfo>>()
                val firstBufferLatch = CountDownLatch(1)
                val secondBufferLatch = CountDownLatch(1)
                glesImageWriter.setup()
                encoder.callback = object : Encoder.Callback {
                    override fun onOutputBufferAvailable(
                        encoder: Encoder,
                        index: Int,
                        info: MediaCodec.BufferInfo
                    ) {
                        queue.add(Pair(index, info))
                        if (firstBufferLatch.count == 1L)
                            firstBufferLatch.countDown()
                        else
                            secondBufferLatch.countDown()
                    }
                }

                // Draw a single frame
                glesImageWriter.makeCurrent()
                GLES20.glClearColor(0f, 0.25f, 0.75f, 1.0f) // red, green, blue, alpha
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                val timestampInNs = 422123L
                glesImageWriter.setPresentationTime(timestampInNs)
                glesImageWriter.swapBuffers()

                // Wait for two buffers from the encoder. The CODEC_CONFIG and
                // the first frame.

                // Check the CODEC_CONFIG
                assertTrue(firstBufferLatch.await(5, TimeUnit.SECONDS))
                queue.pop().let { (index, bufferInfo) ->
                    // Difference between Pixel2 and Emulator for the presentationTimeUs
                    // The emulator uses the timestamp of the first frame. The Pixel2
                    // uses the value zero.
                    assertNotEquals(0, bufferInfo.size)
                    assertTrue(bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0)
                    val byteBuffer = encoder.getOutputBuffer(index)
                    assertNotEquals(null, byteBuffer)
                    encoder.releaseOutputBuffer(index)
                }

                // Check first key frame
                assertTrue(secondBufferLatch.await(5, TimeUnit.SECONDS))
                queue.pop()!!.let { (index, bufferInfo) ->
                    assertEquals(422L, bufferInfo.presentationTimeUs)
                    assertNotEquals(0, bufferInfo.size)
                    assertTrue(bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0)
                    val byteBuffer = encoder.getOutputBuffer(index)
                    assertNotEquals(null, byteBuffer)
                    encoder.releaseOutputBuffer(index)
                }

                // After the CODEC_CONFIG, the media format must be set
                assertNotEquals(null, encoder.mediaFormat)
            }
        }
    }
}