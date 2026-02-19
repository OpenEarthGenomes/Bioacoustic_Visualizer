package com.bioacoustic.visualizer.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.bioacoustic.visualizer.core.render.KotlinPointRenderer
import org.jtransforms.fft.FloatFFT_1D
import kotlin.concurrent.thread

class AudioAnalyzer(private val renderer: KotlinPointRenderer) {
    private val sampleRate = 16000 // Stabilabb mintavételezés a teszthez
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    private var audioRecord: AudioRecord? = null
    private var isRunning = false

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, 
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) return

        isRunning = true
        audioRecord?.startRecording()

        thread(start = true, isDaemon = true) {
            val readBuffer = ShortArray(512)
            val fft = FloatFFT_1D(512.toLong())
            val fftBuffer = FloatArray(1024)

            while (isRunning) {
                val readSize = audioRecord?.read(readBuffer, 0, 512) ?: 0
                if (readSize > 0) {
                    for (i in 0 until 512) {
                        fftBuffer[i] = readBuffer[i].toFloat() / 32768f
                    }
                    fft.realForward(fftBuffer)
                    
                    val magnitudes = FloatArray(256)
                    for (i in 0 until 256) {
                        val re = fftBuffer[2 * i]
                        val im = fftBuffer[2 * i + 1]
                        magnitudes[i] = Math.sqrt((re * re + im * im).toDouble()).toFloat()
                    }
                    // KÖZVETLEN ÁTADÁS A RENDERERNEK (Nincs Flow késleltetés)
                    renderer.updatePoints(magnitudes)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
