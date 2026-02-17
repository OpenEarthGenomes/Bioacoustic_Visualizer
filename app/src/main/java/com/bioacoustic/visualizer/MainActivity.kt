package com.bioacoustic.visualizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import com.bioacoustic.visualizer.core.render.FilamentPointCloudRenderer
import com.bioacoustic.visualizer.core.stream.VisualDataStreamer
import com.bioacoustic.visualizer.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val audioAnalyzer = AudioAnalyzer()
    private lateinit var renderer: FilamentPointCloudRenderer
    private val streamer = VisualDataStreamer(audioAnalyzer)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderer = FilamentPointCloudRenderer(binding.surfaceView)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            startVisualization()
        }
    }

    private fun startVisualization() {
        scope.launch {
            streamer.getVisualData().collectLatest { points ->
                renderer.updatePoints(points)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        renderer.release()
        scope.cancel()
    }
}
