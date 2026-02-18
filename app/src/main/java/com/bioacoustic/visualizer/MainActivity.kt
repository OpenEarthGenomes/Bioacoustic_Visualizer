package com.bioacoustic.visualizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bioacoustic.visualizer.core.render.FilamentPointCloudRenderer
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var surfaceView: SurfaceView
    private var renderer: FilamentPointCloudRenderer? = null
    private val audioAnalyzer = AudioAnalyzer()
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Képernyő ébren tartása (fontos a vizualizációnál)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        // Csak akkor indítjuk a motort, ha megvannak az engedélyek
        checkPermissions()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            initAppSafe()
        }
    }

    private fun initAppSafe() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // Adunk egy kis időt a rendszernek az inicializálásra
                delay(500)
                renderer = FilamentPointCloudRenderer(surfaceView)
                
                // Indítjuk a hang elemzést külön szálon
                launch(Dispatchers.IO) {
                    audioAnalyzer.fftData.collect { data ->
                        runOnUiThread {
                            renderer?.updatePoints(data)
                        }
                    }
                }

                audioAnalyzer.start()
                startRenderLoop()
            } catch (e: Exception) {
                e.printStackTrace()
                // Ha hiba van, nem hagyjuk, hogy csendben omoljon össze
            }
        }
    }

    private fun startRenderLoop() {
        executor.execute {
            while (!isFinishing) {
                val frameTime = System.nanoTime()
                runOnUiThread {
                    renderer?.render(frameTime)
                }
                try {
                    Thread.sleep(16)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, rs: IntArray) {
        super.onRequestPermissionsResult(rc, p, rs)
        if (rc == 101 && rs.isNotEmpty() && rs[0] == PackageManager.PERMISSION_GRANTED) {
            initAppSafe()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer.stop()
        renderer?.release()
        executor.shutdownNow()
    }
}
