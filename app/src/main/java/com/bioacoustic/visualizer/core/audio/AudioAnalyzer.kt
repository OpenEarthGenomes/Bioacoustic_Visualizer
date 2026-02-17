package com.bioacoustic.visualizer.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.jtransforms.fft.FloatFFT_1D

class AudioAnalyzer(private val sampleRate: Int, private val bufferSize: Int) {
    private var audioRecord: AudioRecord? = null
    private var isAnalyzing = false
    val fftData = MutableStateFlow(FloatArray(bufferSize / 2))

    @SuppressLint("MissingPermission")
    fun startAnalysis(scope: CoroutineScope) {
        val minBufSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, minBufSize)
        isAnalyzing = true
        audioRecord?.startRecording()

        scope.launch(Dispatchers.IO) {
            val buffer = FloatArray(bufferSize)
            val fft = FloatFFT_1D(bufferSize.toLong())
            while (isAnalyzing) {
                audioRecord?.read(buffer, 0, bufferSize, AudioRecord.READ_BLOCKING)
                fft.realForward(buffer)
                val magnitudes = FloatArray(bufferSize / 2)
                for (i in 0 until bufferSize / 2) {
                    magnitudes[i] = kotlin.math.sqrt(buffer[2*i]*buffer[2*i] + buffer[2*i+1]*buffer[2*i+1])
                }
                fftData.value = magnitudes
            }
        }
    }
}
