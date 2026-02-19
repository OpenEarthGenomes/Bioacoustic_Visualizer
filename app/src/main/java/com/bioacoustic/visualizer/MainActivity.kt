package com.bioacoustic.visualizer

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
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
    private lateinit var glSurfaceView: GLSurfaceView
    private val renderer = KotlinPointRenderer()
    private val audioAnalyzer = AudioAnalyzer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        glSurfaceView = findViewById(R.id.glSurfaceView)
        glSurfaceView.apply {
            setEGLContextClientVersion(2)
            setRenderer(this@MainActivity.renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        val label = findViewById<TextView>(R.id.sensitivityLabel)
        findViewById<SeekBar>(R.id.sensitivitySeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val factor = progress / 100f
                renderer.sensitivity = factor
                label.text = "Érzékenység: ${String.format("%.1f", factor)}x"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
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

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, rs: IntArray) {
        super.onRequestPermissionsResult(rc, p, rs)
        if (rc == 101 && rs.isNotEmpty() && rs[0] == PackageManager.PERMISSION_GRANTED) {
            startAudioCapture()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer.stop()
    }
}

