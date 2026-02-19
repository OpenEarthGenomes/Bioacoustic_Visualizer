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

class MainActivity : AppCompatActivity() {
    private lateinit var visualizerView: GLSurfaceView
    private val renderer = KotlinPointRenderer()
    // Átadjuk a renderert az analyzernek a közvetlen kapcsolathoz
    private val audioAnalyzer = AudioAnalyzer(renderer)

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
                val boost = (progress / 10f) + 1.0f // Még durvább erősítés skála
                renderer.sensitivity = boost
                label.text = "BIO-BOOST: ${String.format("%.1f", boost)}x"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            audioAnalyzer.start()
        }
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(rc, p, res)
        if (rc == 101 && res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) {
            audioAnalyzer.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer.stop()
    }
}
