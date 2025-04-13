package de.inovex.aosptraining.graphics

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.hardware.camera2.TotalCaptureResult
import android.media.MediaCodec
import android.media.MediaMuxer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT

class CircularBufferActivity : Activity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 21
    }

    private lateinit var cameraManager: CameraManager

    private lateinit var surfaceView: SurfaceView
    private var surface: Surface? = null
    private lateinit var textViewVideoSize: TextView
    private lateinit var textViewFramerate: TextView
    private lateinit var textViewBitrate: TextView
    private lateinit var textViewTotalBufferSize: TextView
    private lateinit var textViewCircularBufferState: TextView
    private lateinit var textViewByteBufferPool: TextView
    private lateinit var textViewDot: TextView
    private lateinit var buttonRecording: Button
    private lateinit var buttonPlayVideo: Button
    private lateinit var handler: Handler
    private lateinit var videoPath: String
    private var emulatorKeyFrameFixUp: EmulatorKeyFrameFixUp? = null
    private var cameraSize : Size? = null
    private val fps = 30
    private val bitrate = 8_000_000 // in bits/sec
    private val iFrameIntervalInSecs = 1
    private var stateRunning: StateRunning? = null
    private var pool = ByteBufferPool(fps)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circular_buffer_activity)
        surfaceView = findViewById(R.id.surfaceView)
        surfaceView.holder.addCallback(surfaceHolderCallback)
        textViewVideoSize = findViewById(R.id.videoSize)
        textViewFramerate = findViewById(R.id.framerate)
        textViewBitrate = findViewById(R.id.bitrate)
        textViewTotalBufferSize = findViewById(R.id.totalBufferSize)
        textViewCircularBufferState = findViewById(R.id.circuleBufferState)
        textViewByteBufferPool = findViewById(R.id.byteBufferPool)
        textViewDot = findViewById(R.id.textViewDot)
        buttonRecording = findViewById(R.id.buttonRecording)
        buttonPlayVideo = findViewById(R.id.buttonPlayVideo)
        buttonPlayVideo.isEnabled = false
        videoPath = filesDir.absolutePath.toString() + "/video.mp4"
        actionBar?.hide()
        Log.v(TAG, "Path to video: $videoPath")

        handler = Handler(mainLooper)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        if (Build.DEVICE.equals("generic_x86_64"))
            emulatorKeyFrameFixUp = EmulatorKeyFrameFixUp(fps, iFrameIntervalInSecs)

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)

        val cameraId = getBackFacingCamera(cameraManager)
        if (cameraId != null)
            Toast.makeText(this, "no camera available", LENGTH_SHORT).show()

        val validSize = getValidOutputSizes(cameraManager, cameraId!!)
        if (validSize == null)
            Toast.makeText(this, "no valid size", LENGTH_SHORT).show()
        else
            textViewVideoSize.text = resources.getString(R.string.videoSize, validSize.width, validSize.height)

        cameraSize = validSize!!
    }

    private val surfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(p0: SurfaceHolder, format: Int, width: Int, height: Int) {
            Log.d(TAG, "surfaceChanged(): format=$format width=$width height=$height")
        }

        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
            Log.d(TAG, "surfaceCreated()")
            surfaceHolder.setFixedSize(cameraSize!!.width, cameraSize!!.height)
            surface = surfaceHolder.surface
            startCameraAndEncoderIfPossible()
        }

        override fun surfaceDestroyed(p0: SurfaceHolder) {
            Log.d(TAG, "surfaceDestroyed()")
            surface = null
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        updateButtonTextOnStateChange()
        startCameraAndEncoderIfPossible()
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()
        stopCameraAndEncoder()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            assert(permissions.size == 1)
            assert(permissions[0] == Manifest.permission.CAMERA)
            assert(grantResults.size == 1)
            if (grantResults[0] == PackageManager.PERMISSION_DENIED)
                Toast.makeText(this, "Camera permission not granted!", LENGTH_SHORT).show()
            else
                startCameraAndEncoderIfPossible()
        }
    }

    private fun updateStats() {
        assertRunOnMainThread()
        var fps = 0f
        var totalBufferSizeInMIB = 0f
        var bitrateInKbits = 0f
        var circularBufferState = ""
        var byteBufferPoolBufferSizeInMIB = 0f
        var byteBufferPoolCountBuffers = 0
        var byteBufferPoolBuffersInUse = 0
        stateRunning?.let { stateRunning ->

            fps = stateRunning.frameRater.fps
            stateRunning.circularBuffer.calcStats().let { stat ->
                totalBufferSizeInMIB = stat.totalBufferSizeInBytes.toFloat() / 1024 / 1024
            }
            bitrateInKbits = stateRunning.bitRater.currentbitrateInKBits
            circularBufferState = stateRunning.circularBuffer.getState().toString()
            stateRunning.circularBuffer.pool.getStat().let { stat ->
                byteBufferPoolBufferSizeInMIB = stat.bufferSizeInBytes.toFloat() / 1024 / 1024
                byteBufferPoolCountBuffers = stat.bufferCount
                byteBufferPoolBuffersInUse = stat.bufferCount - stat.freeBuffers
            }
        }

        textViewFramerate.text = resources.getString(R.string.framerate, fps)
        textViewTotalBufferSize.text =
            resources.getString(R.string.totalBufferSize, totalBufferSizeInMIB)
        textViewBitrate.text = resources.getString(R.string.bitrate, bitrateInKbits)
        textViewCircularBufferState.text =
            resources.getString(R.string.circularBufferState, circularBufferState)
        textViewByteBufferPool.text = resources.getString(
            R.string.byteBufferBool,
            byteBufferPoolBufferSizeInMIB,
            byteBufferPoolBuffersInUse,
            byteBufferPoolCountBuffers
        )

        stateRunning?.let {
            handler.postDelayed(this::updateStats, 1000)
        }
    }

    // The apps needs the permission and the surface to start the camera
    fun startCameraAndEncoderIfPossible() {
        assertRunOnMainThread()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            return

        surface?.let {
            startCameraAndEncoder()
        }
    }

    private fun startCameraAndEncoder() {
        assertRunOnMainThread()
        val cameraId = getBackFacingCamera(cameraManager)

        val encoder = Encoder.createEncoder(cameraSize!!.width, cameraSize!!.height, fps, bitrate)
            ?: throw java.lang.RuntimeException("Cannot create encoder")
        encoder.callback = object : Encoder.Callback {
            override fun onOutputBufferAvailable(
                encoder: Encoder,
                index: Int,
                info: MediaCodec.BufferInfo
            ) {
                stateRunning?.let { stateRunning ->
                    emulatorKeyFrameFixUp?.setKeyFrameInInfo(info)

                    encoder.getOutputBuffer(index)?.let { buffer ->
                        stateRunning.circularBuffer.add(buffer, info)
                        encoder.releaseOutputBuffer(index)
                    }

                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        val isKeyFrame = (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                        stateRunning.bitRater.update(info.size, isKeyFrame)
                    }
                }
            }
        }

        // Reuse the ByteBufferPool
        val circularBuffer = CircularBuffer(
            calcNeededFrames(fps, 1, 5),
            object : CircularBuffer.Callback {
                override fun onBuffer(buffer: CircularBuffer.Buffer) {
                    // This callback my not run on the main UI thread
                    // And Therefore this code is actually racy. Accessing the state variable
                    // is not protected with a mutex
                    stateRunning?.let { stateRunning ->
                        stateRunning.stateRecording?.let { stateRecording ->
                            val index = stateRecording.videoTrackIndex
                            stateRecording.muxer.writeSampleData(index, buffer.buf, buffer.info)
                        }
                    }
                }

                override fun onStateChanged(state: CircularBuffer.State) {
                    runOnUiThread { onCircularBufferStateChange(state) }
                }
            }, pool
        )

        val camera = Camera(cameraManager, cameraId!!)
        camera.callback = object : Camera.Callback {
            override fun onCaptureCompleted(result: TotalCaptureResult) {
                stateRunning?.frameRater?.addNewFrame()
            }
        }

        camera.deconfigure()
        camera.configure(arrayOf(surface!!, encoder.surface))
        camera.start(arrayOf(true, true), fps)

        stateRunning = StateRunning(camera, encoder, circularBuffer)

        // Execute for the first time. It will reschedule itself.
        updateStats()
    }

    private fun stopCameraAndEncoder() {
        assertRunOnMainThread()
        stopMuxing() // If necessary
        stateRunning?.let { stateRunning ->
            assert(stateRunning.stateRecording == null)
            stateRunning.camera.stop()
            stateRunning.camera.deconfigure()
            stateRunning.encoder.close()
            stateRunning.circularBuffer.close()
            handler.removeCallbacks(this::updateStats)
        }
        stateRunning = null
    }

    fun onClickRecording(@Suppress("UNUSED_PARAMETER") v: View) {
        stateRunning?.let { stateRunning ->
            if (stateRunning.stateRecording == null) {
                startMuxing()
            } else {
                stopMuxing()
            }
        }
    }

    fun onClickPlayVideo(@Suppress("UNUSED_PARAMETER") v: View) {
        val bundle = Bundle()
        bundle.putString("path", videoPath)
        val intent = Intent(this, PlaybackActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    private fun startMuxing() {
        assertRunOnMainThread()
        stateRunning?.let { stateRunning ->
            if (stateRunning.stateRecording != null)
                return
            val muxer = MediaMuxer(videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            // You have to pass in the MediaFormat from the Encoder. You cannot built it yourself.
            // See https://stackoverflow.com/a/66592437
            val videoTrackIndex = muxer.addTrack(stateRunning.encoder.mediaFormat!!)
            muxer.start()
            stateRunning.stateRecording = StateRecording(muxer, videoTrackIndex)
            stateRunning.circularBuffer.startMuxing()
            updateButtonTextOnStateChange()
        }
    }

    private fun stopMuxing() {
        assertRunOnMainThread()
        stateRunning?.let { stateRunning ->
            stateRunning.stateRecording?.let { stateRecording ->
                stateRunning.circularBuffer.stopMuxing()
                stateRecording.muxer.stop()
                stateRecording.muxer.release()
                stateRunning.stateRecording = null
                buttonPlayVideo.isEnabled = true
                updateButtonTextOnStateChange()
            }
        }
    }

    private fun assertRunOnMainThread() {
        assert(mainLooper.isCurrentThread)
    }

    private fun updateButtonTextOnStateChange() {
        assertRunOnMainThread()

        var isRecording = false
        stateRunning?.let { stateRunning ->
            isRecording = stateRunning.stateRecording != null
        }

        if (isRecording) {
            buttonRecording.text = resources.getString(R.string.stopRecording)
            textViewDot.setTextColor(Color.parseColor("#FF0000"))
        } else {
            buttonRecording.text = resources.getString(R.string.startRecording)
            textViewDot.setTextColor(Color.parseColor("#222222"))
        }
    }

    fun onCircularBufferStateChange(state: CircularBuffer.State) {
        assertRunOnMainThread()
        textViewCircularBufferState.text =
            resources.getString(R.string.circularBufferState, state.toString())
    }

    class StateRunning(
        val camera: Camera,
        val encoder: Encoder,
        val circularBuffer: CircularBuffer
    ) {
        val frameRater = FrameRater()
        val bitRater = BitRater()
        var stateRecording: StateRecording? = null
    }

    class StateRecording(val muxer: MediaMuxer, val videoTrackIndex: Int)
}
