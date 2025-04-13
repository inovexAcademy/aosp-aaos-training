package de.inovex.aosptraining.graphics

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.*

/**
 * Activity to visually test the GlesImageWriter class
 *
 * Class to showcase and test the draw pattern of the GlesImageWriter.
 */
class GlesImageWriterTestActivity : Activity() {
    companion object {
        private const val TAG = "GlesImageWriterTestActivity"
    }

    private lateinit var surfaceView: SurfaceView
    private var timer: Timer? = null
    private var glesImageWriter: GlesImageWriter? = null
    private var counter = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gles_image_writer_test)
        surfaceView = findViewById(R.id.surfaceViewGlesTest)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(p0: SurfaceHolder) {
                Log.d(TAG, "surfaceCreated()")

                val imageWriter = GlesImageWriter(p0.surface)
                imageWriter.setup()
                counter = 0
                glesImageWriter = imageWriter
                Log.d(TAG, "GlesImageWriter created")

                timer = Timer()
                timer?.schedule(object : TimerTask() {
                    override fun run() {
                        draw()
                    }
                }, 100, 100)
            }

            override fun surfaceChanged(p0: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d(TAG, "surfaceChanged(): format=$format width=$width height=$height")
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                Log.d(TAG, "surfaceDestroyed()")
                glesImageWriter?.close()
                glesImageWriter = null
            }
        })
    }

    fun draw() {
        Trace.beginSection("draw")
        val writer = glesImageWriter ?: return
        Log.d(TAG, "draw(): counter=$counter")
        writer.makeCurrent()
        generateSurfaceFrame(surfaceView.width, surfaceView.height, counter)
        writer.setPresentationTime(SystemClock.elapsedRealtimeNanos()) // TODO
        writer.swapBuffers()
        counter++
        Trace.endSection()
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }
}
