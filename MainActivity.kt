package com.yourname.downloader

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val urlInput = findViewById<EditText>(R.id.urlInput)
        val status = findViewById<TextView>(R.id.status)
        val btn = findViewById<Button>(R.id.downloadBtn)

        // 1) One-time engine init
        scope.launch {
            try {
                YoutubeDL.getInstance().init(application)
                FFmpeg.getInstance().init(application)
                withContext(Dispatchers.Main) { status.text = "Engine ready" }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { status.text = "Init failed: ${e.message}" }
            }
        }

        // 2) "Live" update on launch — nightly channel
        scope.launch {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(
                    application,
                    YoutubeDL.UpdateChannel.NIGHTLY
                )
                withContext(Dispatchers.Main) { status.text = "Engine updated (nightly)" }
            } catch (e: Exception) {
                // Non-fatal: keep using current binary
                withContext(Dispatchers.Main) { status.text = "Update skipped: ${e.message}" }
            }
        }

        // 3) Download on button tap
        btn.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isEmpty()) return@setOnClickListener
            scope.launch {
                try {
                    withContext(Dispatchers.Main) { status.text = "Downloading…" }
                    val outDir = getExternalFilesDir(null)!!.absolutePath
                    val req = YoutubeDLRequest(url).apply {
                        addOption("-f", "bv*+ba/b")
                        addOption("--merge-output-format", "mp4")
                        addOption("-o", "$outDir/%(title)s.%(ext)s")
                    }
                    val response = YoutubeDL.getInstance().execute(req)
                    withContext(Dispatchers.Main) {
                        status.text = "Done: ${response.out}"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { status.text = "Error: ${e.message}" }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
