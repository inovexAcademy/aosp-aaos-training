package de.inovex.aosptraining.graphics

import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.util.Range
import android.view.Surface
import java.io.Closeable
import java.lang.Integer.min
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class Camera(
    private val cameraManager: CameraManager,
    private val id: String
) : Closeable {
    private val executor = newSingleThreadExecutor()
    private val state = AtomicReference<StateConfigured>(null)
    var callback : Callback? = null

    fun configure(surfaces: Array<Surface>) {
        if (state.get() != null)
            return

        val cameraDeviceLatch = AtomicValueAndLatch<CameraDevice>()

        val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(c: CameraDevice) {
                cameraDeviceLatch.setValue(c)
            }

            override fun onDisconnected(p0: CameraDevice) {
                cameraDeviceLatch.setError(RuntimeException("onDisconnected"))
            }

            override fun onError(p0: CameraDevice, p1: Int) {
                cameraDeviceLatch.setError(RuntimeException("onError"))
            }

            override fun onClosed(camera: CameraDevice) {
                state.get()?.deviceClosedLatch?.countDown()
            }
        }

        try {
            cameraManager.openCamera(id, executor, cameraDeviceStateCallback)
        } catch (e: SecurityException) {
            throw RuntimeException(e)
        }
        val cameraDevice = cameraDeviceLatch.waitAndGet()

        val captureSessionLatch = AtomicValueAndLatch<CameraCaptureSession>()

        val cameraSessionStateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(p0: CameraCaptureSession) {
                captureSessionLatch.setValue(p0)
            }

            override fun onConfigureFailed(p0: CameraCaptureSession) {
                captureSessionLatch.setError(RuntimeException("onConfigureFailed"))
            }

            override fun onClosed(session: CameraCaptureSession) {
                state.get()?.sessionClosedLatch?.countDown()
            }
        }

        val outputConfigurations = surfaces.map { OutputConfiguration(it) }
        val config = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            outputConfigurations, executor, cameraSessionStateCallback
        )
        cameraDevice.createCaptureSession(config)

        val captureSession = captureSessionLatch.waitAndGet()
        // TODO how to report error as toast?

        state.set(StateConfigured(cameraDevice, captureSession, surfaces))
    }

    fun start(enableSurfaces : Array<Boolean>) {
        start(enableSurfaces, 30)
    }

    fun start(enableSurfaces : Array<Boolean>, fps: Int) {
        val s = state.get() ?: return
        if (s.runningState.get() != null)
            return

        val captureRequest = s.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        var counter = 0
        for (i in 0 until min(enableSurfaces.size, s.surfaces.size)) {
            if (enableSurfaces[i]) {
                captureRequest.addTarget(s.surfaces[i])
                counter++
            }
        }
        if (counter == 0)
            throw IllegalArgumentException("At least one surface must be enabled")

        captureRequest.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range.create(fps, fps))

        s.captureSession.setSingleRepeatingRequest(
            captureRequest.build(),
            executor, object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    callback?.onCaptureCompleted(result)
                }

                override fun onCaptureSequenceCompleted(
                    session: CameraCaptureSession,
                    sequenceId: Int,
                    frameNumber: Long
                ) {
                    state.get()?.runningState?.get()?.captureRequestClosed?.countDown()
                }
            })


        s.runningState.set(StateRunning())
    }

    fun stop() {
        val s = state.get() ?: return
        val stateRunning = s.runningState.get() ?: return

        s.captureSession.stopRepeating()
        stateRunning.captureRequestClosed.await()

        s.runningState.set(null)
    }

    fun deconfigure() {
        val s = state.get() ?: return

        stop()
        assert(s.runningState.get() == null)

        s.captureSession.close()
        // After closing the session, we have to wait for the callback.
        s.sessionClosedLatch.await()

        s.cameraDevice.close()
        // After closing the device, we have to wait for the callback.
        s.deviceClosedLatch.await()

        state.set(null)
    }

    fun isConfigured(): Boolean {
        return state.get() != null
    }

    fun isRunning(): Boolean {
        val s = state.get() ?: return false
        return s.runningState.get() != null
    }

    override fun close() {
        deconfigure()
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    class StateConfigured(
        val cameraDevice: CameraDevice,
        val captureSession: CameraCaptureSession,
        val surfaces: Array<Surface>,
    ) {
        // Needed for correct shutdown
        val sessionClosedLatch = CountDownLatch(1)
        val deviceClosedLatch = CountDownLatch(1)
        val runningState = AtomicReference<StateRunning>(null)
    }

    class StateRunning {
        val captureRequestClosed = CountDownLatch(1)
    }

    interface Callback {
        fun onCaptureCompleted(result : TotalCaptureResult)
    }
}



