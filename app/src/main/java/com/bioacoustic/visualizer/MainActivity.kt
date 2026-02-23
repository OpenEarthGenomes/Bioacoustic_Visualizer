package com.bioacoustic.visualizer

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import com.bioacoustic.visualizer.core.render.KotlinPointRenderer

class MainActivity : AppCompatActivity() {
    private var visualizerView: GLSurfaceView? = null
    private val renderer = KotlinPointRenderer()
    private var audioAnalyzer: AudioAnalyzer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
        } else {
            initApp()
        }
    }

    private fun initApp() {
        visualizerView = findViewById(R.id.visualizerView)
        visualizerView?.setEGLContextClientVersion(3)
        visualizerView?.setRenderer(renderer)
        audioAnalyzer = AudioAnalyzer(renderer)

        // Itt volt a hiba esélyes helye - a biztonság kedvéért null-checkel
        val boostText = findViewById<TextView>(R.id.boostText)
        findViewById<SeekBar>(R.id.sensitivitySeekBar)?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                val boost = progress / 10.0f
                renderer.sensitivity = boost
                boostText?.text = "BIO-BOOST: ${String.format("%.1fx", boost)}"
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initApp()
        }
    }

    override fun onResume() { super.onResume(); audioAnalyzer?.start() }
    override fun onPause() { super.onPause(); audioAnalyzer?.stop() }
}
