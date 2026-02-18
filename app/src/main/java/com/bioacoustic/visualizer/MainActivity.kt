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
    
    companion object {
        init {
            try {
                com.google.android.filament.Filament.init()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private lateinit var surfaceView: SurfaceView
    private var renderer: FilamentPointCloudRenderer? = null
    private val audioAnalyzer = AudioAnalyzer()
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            startEverything()
        }
    }

    private fun startEverything() {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(500) 
            try {
                renderer = FilamentPointCloudRenderer(surfaceView)
                
                // EZ AZ A RÉSZ, AMIT BELE KELL TENNÜNK:
                // Elindítjuk a mikrofon adatainak gyűjtését és átadjuk a pontoknak
                lifecycleScope.launch(Dispatchers.IO) {
                    audioAnalyzer.fftData.collect { data ->
                        runOnUiThread {
                            renderer?.updatePoints(data)
                        }
                    }
                }

                audioAnalyzer.start()
                
                executor.execute {
                    while (!isFinishing) {
                        val frameTime = System.nanoTime()
                        runOnUiThread {
                            renderer?.render(frameTime)
                        }
                        Thread.sleep(16)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, rs: IntArray) {
        super.onRequestPermissionsResult(rc, p, rs)
        if (rc == 101 && rs.isNotEmpty() && rs[0] == PackageManager.PERMISSION_GRANTED) {
            startEverything()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer.stop()
        renderer?.release()
        executor.shutdownNow()
    }
}
