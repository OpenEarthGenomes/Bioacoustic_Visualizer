package com.bioacoustic.visualizer.core.audio

import android.media.audiofx.Visualizer
import com.bioacoustic.visualizer.core.render.KotlinPointRenderer
import kotlin.math.sqrt

class AudioAnalyzer(private val renderer: KotlinPointRenderer) {
    private var visualizer: Visualizer? = null

    fun start() {
        try {
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, sr: Int) {
                        fft?.let {
                            val magnitudes = FloatArray(it.size / 2)
                            for (i in 0 until magnitudes.size) {
                                val r = it[i * 2].toInt()
                                val im = it[i * 2 + 1].toInt()
                                magnitudes[i] = sqrt((r * r + im * im).toFloat())
                            }
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
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
    }
}
