package com.bioacoustic.visualizer.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jtransforms.fft.FloatFFT_1D

class AudioAnalyzer {
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    
    private val _fftData = MutableStateFlow(FloatArray(0))
    val fftData: StateFlow<FloatArray> = _fftData

    private var isRunning = false
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        isRunning = true
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()

        Thread {
            val audioBuffer = ShortArray(bufferSize)
            val fft = FloatFFT_1D(bufferSize.toLong())
            val fftBuffer = FloatArray(bufferSize * 2)

            while (isRunning) {
                val readSize = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    for (i in 0 until readSize) {
                        fftBuffer[i] = audioBuffer[i].toFloat()
                    }
                    fft.realForward(fftBuffer)
                    
                    // Csak a magnitúdókat számoljuk ki (látványhoz ez kell)
                    val magnitudes = FloatArray(readSize / 2)
                    for (i in 0 until readSize / 2) {
                        val re = fftBuffer[2 * i]
                        val im = fftBuffer[2 * i + 1]
                        magnitudes[i] = Math.sqrt((re * re + im * im).toDouble()).toFloat()
                    }
                    _fftData.value = magnitudes
                }
            }
        }.start()
    }

    fun stop() {
        isRunning = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
