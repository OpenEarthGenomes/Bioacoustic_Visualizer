package com.bioacoustic.visualizer.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import org.jtransforms.fft.FloatFFT_1D
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

class AudioAnalyzer(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 1024
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val fft = FloatFFT_1D(bufferSize.toLong())
    
    // --- EZEK KELLENEK A BUILDHEZ ---
    private val _audioFlow = MutableStateFlow(ShortArray(bufferSize))
    val audioFlow: StateFlow<ShortArray> = _audioFlow
    
    var audioBuffer = ShortArray(bufferSize)
    var fftMagnitudes = FloatArray(bufferSize / 2)
    var spectralCentroid: Float = 0f
    // --------------------------------

    private val fftBuffer = FloatArray(bufferSize * 2)

    @SuppressLint("MissingPermission")
    fun startAnalysis(scope: CoroutineScope) {
        val minBufSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufSize.coerceAtLeast(bufferSize * 2)
        )

        isRecording = true
        audioRecord?.startRecording()

        scope.launch(Dispatchers.IO) {
            while (isRecording && isActive) {
                val readCount = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readCount > 0) {
                    processAudio(readCount)
                    _audioFlow.value = audioBuffer.copyOf()
                }
            }
        }
    }

    private fun processAudio(count: Int) {
        for (i in 0 until count) {
            val window = 0.5f * (1f - Math.cos(2.0 * Math.PI * i / count).toFloat())
            fftBuffer[i] = (audioBuffer[i] / 32768.0f) * window
        }
        for (i in count until bufferSize * 2) fftBuffer[i] = 0f

        fft.realForward(fftBuffer)

        var sumWeight = 0f
        var sumMag = 0f

        for (i in 0 until bufferSize / 2) {
            val re = fftBuffer[2 * i]
            val im = fftBuffer[2 * i + 1]
            val mag = sqrt(re * re + im * im)
            fftMagnitudes[i] = mag
            
            sumWeight += i * mag
            sumMag += mag
        }
        
        spectralCentroid = if (sumMag > 0) sumWeight / sumMag else 0f
    }

    fun stop() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
