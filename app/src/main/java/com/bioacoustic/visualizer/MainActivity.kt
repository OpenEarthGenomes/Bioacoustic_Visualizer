package com.bioacoustic.visualizer

import android.Manifest
import android.os.Bundle
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import com.bioacoustic.visualizer.core.render.FilamentPointCloudRenderer
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var surfaceView: SurfaceView
    private lateinit var renderer: FilamentPointCloudRenderer
    private lateinit var audioAnalyzer: AudioAnalyzer
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Képernyő beállítása
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        // 2. 3D Megjelenítő indítása
        renderer = FilamentPointCloudRenderer(surfaceView)

        // 3. Hang elemző indítása
        audioAnalyzer = AudioAnalyzer { audioData ->
            // Amikor jön hang, küldjük a 3D motornak
            runOnUiThread {
                renderer.updatePoints(audioData)
            }
        }

        // 4. Mikrofon engedély kérése és indítás
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        
        // Renderelési ciklus indítása
        startRenderLoop()
    }

    private fun startRenderLoop() {
        executor.execute {
            while (!isFinishing) {
                val frameTime = System.nanoTime()
                runOnUiThread {
                    renderer.render(frameTime)
                }
                Thread.sleep(16) // ~60 FPS
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
