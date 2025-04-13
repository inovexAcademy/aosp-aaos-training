package de.inovex.aosptraining.graphics

import android.app.Activity
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.core.net.toUri

class PlaybackActivity : Activity() {
    private lateinit var videoView: VideoView
    private lateinit var textViewDuration: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)
        videoView = findViewById(R.id.videoView)
        textViewDuration = findViewById(R.id.textViewDuration)

        // Otherwise the back button does not work, the the videoView has the focus.
        // It's a common problem, but no solution from the internet has worked
        // https://stackoverflow.com/q/36442678
        videoView.setOnKeyListener { _, _, event ->
            if (event?.keyCode == KeyEvent.KEYCODE_BACK) {
                Log.d("TAG", "Force finish of the activity")
                this.finish()
                true
            } else {
                false
            }
        }

        // Enable the back button when the MediaControl is visible
        // See https://stackoverflow.com/a/61338702
        val mediaController = MediaController(this)
        mediaController.addOnUnhandledKeyEventListener { _, event ->
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                this@PlaybackActivity.finish()
                true
            } else {
                false
            }
        }
        videoView.setMediaController(mediaController)
        val path = intent.extras?.getString("path")
        var durationInSecs = 0f
        if (path == null || path == "") {
            val uri = ("android.resource://" + packageName + "/" + R.raw.video).toUri()
            durationInSecs = getTimeOfVideoInMilliSecsFormUri(uri).toFloat() / 1000
            videoView.setVideoURI(uri)
        } else {
            durationInSecs = getTimeOfVideoInMilliSecsFromPath(path).toFloat() / 1000
            videoView.setVideoPath(path)
        }
        textViewDuration.text = resources.getString(R.string.duration, durationInSecs)
        videoView.start()

    }

    private fun getTimeOfVideoInMilliSecsFormUri(uri: Uri): Long {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(this, uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            return (time ?: "-1").toLong()
        }
    }

    private fun getTimeOfVideoInMilliSecsFromPath(path: String): Long {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(path)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            return (time ?: "-1").toLong()
        }
    }
}