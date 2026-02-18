package com.bioacoustic.visualizer

import android.Manifest
import android.os.Bundle
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bioacoustic.visualizer.core.render.FilamentPointCloudRenderer
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var surfaceView: SurfaceView
    private lateinit var renderer: FilamentPointCloudRenderer
    private val audioAnalyzer = AudioAnalyzer()
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        renderer = FilamentPointCloudRenderer(surfaceView)

        // Engedélykérés
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)

        // Adatfolyam figyelése: ha jön új hangadat, küldjük a renderelőnek
        lifecycleScope.launch {
            audioAnalyzer.fftData.collect { data ->
                renderer.updatePoints(data)
            }
        }

        audioAnalyzer.start()
        startRenderLoop()
    }

    private fun startRenderLoop() {
        executor.execute {
            while (!isFinishing) {
                val frameTime = System.nanoTime()
                runOnUiThread {
                    renderer.render(frameTime)
                }
                Thread.sleep(16)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer.stop()
        renderer.release()
        executor.shutdown()
    }
}
