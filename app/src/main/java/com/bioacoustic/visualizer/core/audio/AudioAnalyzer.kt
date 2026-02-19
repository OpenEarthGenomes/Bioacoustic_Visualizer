package com.bioacoustic.visualizer.core.audio

import android.media.audiofx.Visualizer
import com.bioacoustic.visualizer.core.render.KotlinPointRenderer
import kotlin.math.sqrt

class AudioAnalyzer(private val renderer: KotlinPointRenderer) {
    private var visualizer: Visualizer? = null

    fun start() {
        try {
            // A 0-s session az összes kimenő hangot elkapja
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, sr: Int) {
                        fft?.let {
                            val magnitudes = FloatArray(it.size / 2)
                            for (i in 0 until magnitudes.size) {
                                val r = it[i * 2].toInt()
                                val im = it[i * 2 + 1].toInt()
                                // Magnitúdó számítás (a te 3D-s terved alapján)
                                magnitudes[i] = sqrt((r * r + im * im).toFloat())
                            }
                            // A renderer meglévő függvényét hívjuk
                            renderer.updatePoints(magnitudes)
                        }
                    }
                    override fun onWaveFormDataCapture(v: Visualizer?, w: ByteArray?, sr: Int) {}
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        visualizer?.apply {
            enabled = false
            release()
        }
        visualizer = null
    }
}

