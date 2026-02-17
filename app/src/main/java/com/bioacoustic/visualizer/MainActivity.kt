package com.bioacoustic.visualizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import com.bioacoustic.visualizer.core.render.FilamentPointCloudRenderer
import com.bioacoustic.visualizer.core.stream.VisualDataStreamer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {
    // Eltávolítottuk a fix binding importot, mert a fordító nem találta
    private var _binding: com.bioacoustic.visualizer.databinding.ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val audioAnalyzer = AudioAnalyzer(sampleRate = 44100, bufferSize = 1024)
    private lateinit var renderer: FilamentPointCloudRenderer
    private val streamer = VisualDataStreamer(audioAnalyzer)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Közvetlen elérési úttal hívjuk meg a bindingot
        _binding = com.bioacoustic.visualizer.databinding.ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderer = FilamentPointCloudRenderer(binding.surfaceView)

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
        _binding = null
    }
}
