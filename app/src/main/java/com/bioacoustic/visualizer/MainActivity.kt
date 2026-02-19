package com.bioacoustic.visualizer

import android.os.Bundle
import android.opengl.GLSurfaceView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.bioacoustic.visualizer.core.render.KotlinPointRenderer
import com.bioacoustic.visualizer.core.audio.AudioAnalyzer

class MainActivity : AppCompatActivity() {
    private var visualizerView: GLSurfaceView? = null
    private val renderer = KotlinPointRenderer()
    private lateinit var audioAnalyzer: AudioAnalyzer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        visualizerView = findViewById(R.id.visualizerView)
        
        // OpenGL ES 3.0 beállítása a vízeséshez
        visualizerView?.setEGLContextClientVersion(3)
        visualizerView?.setRenderer(renderer)
        
        audioAnalyzer = AudioAnalyzer(renderer)

        val seekBar = findViewById<SeekBar>(R.id.sensitivitySeekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                // Érzékenység állítása a rendererben
                renderer.sensitivity = progress / 100f
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        audioAnalyzer.start()
    }

    override fun onPause() {
        super.onPause()
        audioAnalyzer.stop()
    }
}
