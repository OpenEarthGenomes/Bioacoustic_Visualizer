package com.bioacoustic.visualizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import com.bioacoustic.visualizer.core.render.FilamentPointCloudRenderer
import com.google.android.filament.Engine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var engine: Engine
    private lateinit var renderer3D: FilamentPointCloudRenderer
    private lateinit var audioAnalyzer: AudioAnalyzer
    private lateinit var surfaceView: SurfaceView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startLogic() else Toast.makeText(this, "Engedély kell!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge mód a Samsung A35-höz
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startLogic()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startLogic() {
        engine = Engine.create()
        renderer3D = FilamentPointCloudRenderer(surfaceView, engine)
        audioAnalyzer = AudioAnalyzer()

        audioAnalyzer.onDataReady = { magnitudes ->
            runOnUiThread { renderer3D.updateVisuals(magnitudes) }
        }
        
        audioAnalyzer.startAnalysis(lifecycleScope)

        lifecycleScope.launch {
            while (true) {
                renderer3D.render(System.nanoTime())
                delay(16)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioAnalyzer.isInitialized) audioAnalyzer.stop()
        if (::engine.isInitialized) engine.destroy()
    }
}
