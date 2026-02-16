package com.bioacoustic.visualizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import com.bioacoustic.visualizer.core.render.FilamentPointCloudRenderer
import com.google.android.filament.Engine

class MainActivity : ComponentActivity() {

    private lateinit var engine: Engine
    private lateinit var renderer3D: FilamentPointCloudRenderer
    private lateinit var audioAnalyzer: AudioAnalyzer
    private lateinit var surfaceView: SurfaceView

    // Mikrofon engedély kérése modern módon
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startLogic()
        } else {
            Toast.makeText(this, "Mikrofon engedély szükséges a vizualizációhoz!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SurfaceView létrehozása a Filament számára
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        // Engedély ellenőrzése
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == 
                PackageManager.PERMISSION_GRANTED -> {
                startLogic()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startLogic() {
        // 1. Filament motor indítása
        engine = Engine.create()
        renderer3D = FilamentPointCloudRenderer(surfaceView, engine)

        // 2. Audio elemző indítása
        audioAnalyzer = AudioAnalyzer()
        audioAnalyzer.onDataReady = { magnitudes ->
            // Itt küldjük az adatokat a 3D-nek
            // renderer3D.updatePoints(magnitudes) 
        }
        audioAnalyzer.startAnalysis(lifecycleScope)

        // 3. Renderelési ciklus indítása
        startRenderLoop()
    }

    private fun startRenderLoop() {
        lifecycleScope.launch {
            while (true) {
                renderer3D.render(System.nanoTime())
                kotlinx.coroutines.delay(16) // ~60 FPS
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioAnalyzer.isInitialized) audioAnalyzer.stop()
        if (::engine.isInitialized) engine.destroy()
    }
}
