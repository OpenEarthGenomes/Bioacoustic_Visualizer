package com.bioacoustic.visualizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import com.bioacoustic.visualizer.core.render.FilamentPointCloudRenderer
import com.bioacoustic.visualizer.core.stream.VisualDataStreamer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {
    private val audioAnalyzer = AudioAnalyzer(44100, 1024)
    private var renderer: FilamentPointCloudRenderer? = null
    private val streamer = VisualDataStreamer(audioAnalyzer)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                scope.launch {
                    delay(500) // One UI 8 stabilitási várakozás
                    renderer = FilamentPointCloudRenderer(surfaceView)
                    if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
                    } else {
                        startApp()
                    }
                }
            }
            override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, height: Int) {}
            override fun surfaceDestroyed(h: SurfaceHolder) { renderer?.release() }
        })
    }

    private fun startApp() {
        audioAnalyzer.startAnalysis(scope)
        scope.launch {
            streamer.getVisualData().collectLatest { points ->
                renderer?.updatePoints(points)
                renderer?.render(System.nanoTime())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        renderer?.release()
    }
}

