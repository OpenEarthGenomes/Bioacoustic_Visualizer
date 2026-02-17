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
    
    // A te JTransforms-os analyzer-edet használjuk
    private val audioAnalyzer = AudioAnalyzer(sampleRate = 44100, bufferSize = 1024)
    private lateinit var renderer: FilamentPointCloudRenderer
    private val streamer = VisualDataStreamer(audioAnalyzer)
    
    // A fő szálon futó Coroutine a UI és a renderelés frissítéséhez
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ViewBinding beállítása (ez kell az XML-hez)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // A 3D renderer elindítása a SurfaceView-n
        renderer = FilamentPointCloudRenderer(binding.surfaceView)

        // Mikrofon engedély ellenőrzése (Android 14 / Samsung A35 miatt kritikus)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            startEverything()
        }
    }

    private fun startEverything() {
        // 1. Elindítjuk a mikrofon olvasását és az FFT-t a háttérben
        audioAnalyzer.startAnalysis(scope)

        // 2. Bekötjük a vizuális adatfolyamot a rendererbe
        // Amint jön új adat a Streamer-től, a Renderer azonnal frissíti a 3D pontokat
        scope.launch {
            streamer.getVisualData().collectLatest { points ->
                renderer.updatePoints(points)
                // A Filament renderernek szólni kell, hogy rajzoljon új képkockát
                renderer.render(System.nanoTime())
            }
        }
    }

    // Ha a felhasználó rányom az "Engedélyezem" gombra, akkor indul el a buli
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startEverything()
        }
    }

    override fun onResume() {
        super.onResume()
        // Itt lehetne újraindítani a renderert, ha háttérbe került az app
    }

    override fun onDestroy() {
        super.onDestroy()
        // Mindent leállítunk, hogy ne egye az akksit és ne maradjon nyitva a mikrofon
        audioAnalyzer.stop()
        renderer.release()
        scope.cancel()
    }
}
