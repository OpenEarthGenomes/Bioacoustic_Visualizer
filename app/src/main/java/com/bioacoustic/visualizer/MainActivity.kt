package com.bioacoustic.visualizer

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import com.bioacoustic.visualizer.core.render.KotlinPointRenderer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var visualizerView: GLSurfaceView
    private val renderer = KotlinPointRenderer()
    private val audioAnalyzer = AudioAnalyzer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        visualizerView = findViewById(R.id.visualizerView)
        visualizerView.setEGLContextClientVersion(2)
        visualizerView.setRenderer(renderer)

        val label = findViewById<TextView>(R.id.sensitivityLabel)
        findViewById<SeekBar>(R.id.sensitivitySeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                // Skála: 0.5x - 20.5x közötti erősítés
                val boost = (progress / 50f) + 0.5f
                renderer.sensitivity = boost
                label.text = "BIO-BOOST: ${String.format("%.1f", boost)}x"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            startAudioCapture()
        }
    }

    private fun startAudioCapture() {
        lifecycleScope.launch(Dispatchers.IO) {
            audioAnalyzer.fftData.collect { data ->
                renderer.updatePoints(data)
            }
        }
        audioAnalyzer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer.stop()
    }
}
