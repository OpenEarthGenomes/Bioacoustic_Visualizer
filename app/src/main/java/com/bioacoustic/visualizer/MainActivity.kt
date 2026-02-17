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
    private val audioAnalyzer = AudioAnalyzer(sampleRate = 44100, bufferSize = 1024)
    private var renderer: FilamentPointCloudRenderer? = null
    private val streamer = VisualDataStreamer(audioAnalyzer)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.bioacoustic.visualizer.R.layout.activity_main)
        
        val surfaceView = findViewById<SurfaceView>(com.bioacoustic.visualizer.R.id.surfaceView)

        // ANDROID 16 SAFE CALLBACK
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // Késleltetett indítás, hogy a One UI 8 grafikai motorja beálljon
                scope.launch {
                    delay(500) 
                    try {
                        renderer = FilamentPointCloudRenderer(surfaceView)
                        checkPermissionsAndStart()
                    } catch (e: Exception) {
                        // Megakadályozza az azonnali összeomlást
                    }
                }
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                renderer?.release()
            }
        })
    }

    private fun checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            startEverything()
        }
    }

    private fun startEverything() {
        // Csak akkor indul a mikrofon, ha minden más már stabil
        scope.launch {
            audioAnalyzer.startAnalysis(this)
            streamer.getVisualData().collectLatest { points ->
                renderer?.updatePoints(points)
                renderer?.render(System.nanoTime())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer.stop()
        renderer?.release()
        scope.cancel()
    }
}
