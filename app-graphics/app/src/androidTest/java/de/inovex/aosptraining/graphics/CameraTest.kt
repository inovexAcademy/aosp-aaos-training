package de.inovex.aosptraining.graphics

import android.app.Service
import android.graphics.ImageFormat
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.permission.PermissionRequester
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CameraTest {
    private val cameraManager: CameraManager
    private val handlerThread = HandlerThread("CameraTest")
    private var handler: Handler
    private var id: String?

    init {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        cameraManager = appContext.getSystemService(Service.CAMERA_SERVICE) as CameraManager

        handlerThread.start()
        handler = Handler(handlerThread.looper)

        id = getBackFacingCamera(cameraManager)
    }

    private fun createImageReader(): ImageReader {
        //  return ImageReader.newInstance(640, 480, ImageFormat.PRIVATE, 3)
        return ImageReader.newInstance(640, 480, ImageFormat.PRIVATE, 3)
    }

    @Before
    fun requestCameraPermission() {
        PermissionRequester().apply {
            addPermissions("android.permission.CAMERA")
            requestPermissions()
        }
    }

    @Test
    fun testCameraUtil() {
        val cameraId = getBackFacingCamera(cameraManager);
        assertNotNull("There should be a back facing camera", cameraId)

        assertTrue(cameraId!!.isNotEmpty())
        assertNotNull("At least one size should be valid", getValidOutputSizes(cameraManager, cameraId))
    }

    @Test
    fun basic() {
        createImageReader().use { imageReader ->
            val countDownLatch = CountDownLatch(1)
            imageReader.setOnImageAvailableListener({
                countDownLatch.countDown()
            }, handler)

            Camera(cameraManager, id!!).use { camera ->

                assertFalse(camera.isConfigured())
                assertFalse(camera.isRunning())
                camera.configure(arrayOf(imageReader.surface))

                assertTrue(camera.isConfigured())
                assertFalse(camera.isRunning())
                camera.start(arrayOf(true))

                assertTrue(camera.isConfigured())
                assertTrue(camera.isRunning())

                assertEquals(countDownLatch.await(3, TimeUnit.SECONDS), true)

                camera.stop()
                assertTrue(camera.isConfigured())
                assertFalse(camera.isRunning())

                camera.deconfigure()
                assertFalse(camera.isConfigured())
                assertFalse(camera.isRunning())
                imageReader.close()
            }
        }
    }

    @Test
    fun simpleStressTestWithConfigureAndStart() {
        createImageReader().use { imageReader ->
            for (i in 1..10) {
                Camera(cameraManager, id!!).use { camera ->
                    camera.configure(arrayOf(imageReader.surface))
                    camera.start(arrayOf(true))
                    camera.stop()
                    camera.deconfigure()
                }
            }
        }
    }

    @Test
    fun simpleStressTestWithConfigure() {
        createImageReader().use { imageReader ->
            for (i in 1..50) {
                Camera(cameraManager, id!!).use { camera ->
                    camera.configure(arrayOf(imageReader.surface))
                    camera.deconfigure()
                }
            }
        }
    }
}