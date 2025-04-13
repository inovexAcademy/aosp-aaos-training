package de.inovex.aosptraining.graphics

import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlesImageWriterTest {
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
    fun testWithImageReader() {
        val width = 400
        val height = 300
        val format = PixelFormat.RGBA_8888
        ImageReader.newInstance(width, height, format, 3).use { imageReader ->
            GlesImageWriter(imageReader.surface).use { glesImageWriter ->
                val imageLatch = AtomicValueAndLatch<Image>()
                imageReader.setOnImageAvailableListener({ imageReader ->
                    val image = imageReader.acquireLatestImage()
                    if (image != null) {
                        imageLatch.setValue(image)
                    } else {
                        imageLatch.setError(RuntimeException("Image is null"))
                    }
                }, handler)

                glesImageWriter.setup()
                glesImageWriter.makeCurrent()
                GLES20.glClearColor(0f, 0.25f, 0.75f, 1.0f) // red, green, blue, alpha
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                val timestamp = 422123L
                glesImageWriter.setPresentationTime(timestamp)
                glesImageWriter.swapBuffers()

                imageLatch.waitAndGet().use { image ->
                    assertEquals(image.width, width)
                    assertEquals(image.height, height)
                    assertEquals(image.format, format)
                    assertEquals(image.timestamp, timestamp)
                    assertEquals(image.planes.size, 1)
                    val byteBuffer = image.planes[0].buffer
                    val pixelStride = image.planes[0].pixelStride
                    val rowStride = image.planes[0].rowStride
                    // Pixel format: RGBA (red, green, blue, alpha)
                    for (y in 0 until image.height) {
                        for (x in 0 until image.width) {
                            val pos = y * rowStride + x * pixelStride
                            assertEquals(0.toUByte(), byteBuffer.get(pos + 0).toUByte()) // red
                            assertEquals(64.toUByte(), byteBuffer.get(pos + 1).toUByte()) // green
                            assertEquals(191.toUByte(), byteBuffer.get(pos + 2).toUByte()) // blue
                            assertEquals(255.toUByte(), byteBuffer.get(pos + 3).toUByte()) // alpha
                        }
                    }
                }
            }
        }
    }
}
