package com.bioacoustic.visualizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import com.bioacoustic.visualizer.core.render.FilamentPointCloudRenderer
import com.bioacoustic.visualizer.core.stream.VisualDataStreamer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import com.bioacoustic.visualizer.R

class MainActivity : AppCompatActivity() {
    private val audioAnalyzer = AudioAnalyzer(sampleRate = 44100, bufferSize = 1024)
    private lateinit var renderer: FilamentPointCloudRenderer
    private val streamer = VisualDataStreamer(audioAnalyzer)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ez a sor keresi az R.layout-ot
        setContentView(R.layout.activity_main)
        
        // Ez a sor keresi az R.id-t
        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)

        renderer = FilamentPointCloudRenderer(surfaceView)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            startEverything()
        }
    }

    private fun startEverything() {
        audioAnalyzer.startAnalysis(scope)
        scope.launch {
            streamer.getVisualData().collectLatest { points ->
                renderer.updatePoints(points)
                renderer.render(System.nanoTime())
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startEverything()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer.stop()
        renderer.release()
        scope.cancel()
    }
}
