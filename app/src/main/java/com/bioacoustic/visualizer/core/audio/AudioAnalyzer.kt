package com.bioacoustic.visualizer.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jtransforms.fft.DoubleFFT_1D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class AudioAnalyzer(
    private val sampleRate: Int = 44100,
    private val bufferSizeInSeconds: Double = 0.1,
    private val coroutineScope: CoroutineScope
) : DefaultLifecycleObserver {
    
    companion object {
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
        private const val BYTES_PER_SAMPLE = 4 
    }
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _spectralCentroid = MutableStateFlow(0f)
    val spectralCentroid: StateFlow<Float> = _spectralCentroid.asStateFlow()
    
    private val _audioBuffer = MutableStateFlow<FloatArray?>(null)
    val audioBuffer: StateFlow<FloatArray?> = _audioBuffer.asStateFlow()
    
    private val _fftMagnitudes = MutableStateFlow<FloatArray?>(null)
    val fftMagnitudes: StateFlow<FloatArray?> = _fftMagnitudes.asStateFlow()
    
    private var audioRecord: AudioRecord? = null
    private val bufferSizeInFrames: Int = (sampleRate * bufferSizeInSeconds).toInt()
    private val bufferSizeInBytes: Int = bufferSizeInFrames * BYTES_PER_SAMPLE
    
    private val fftSize: Int = nextPowerOfTwo(bufferSizeInFrames)
    private val fftAnalyzer = DoubleFFT_1D(fftSize.toLong())
    private val fftWindow = DoubleArray(fftSize) { i ->
        if (i < bufferSizeInFrames) 0.5 * (1 - cos(2 * PI * i / (bufferSizeInFrames - 1))) else 0.0
    }
    
    private val mutex = Mutex()
    private var recordingJob: Job? = null
    
    override fun onStart(owner: LifecycleOwner) { startRecording() }
    override fun onStop(owner: LifecycleOwner) { stopRecording() }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        coroutineScope.launch {
            mutex.withLock {
                if (_isRecording.value) return@launch
                try {
                    initializeAudioRecord()
                    audioRecord?.startRecording()
                    _isRecording.value = true
                    recordingJob = coroutineScope.launch(Dispatchers.Default) {
                        analyzeAudioContinuously()
                    }
                } catch (e: Exception) {
                    _isRecording.value = false
                }
            }
        }
    }

    fun stopRecording() {
        coroutineScope.launch {
            mutex.withLock {
                recordingJob?.cancel()
                audioRecord?.apply { 
                    try { stop() } catch(e: Exception) {}
                    release() 
                }
                audioRecord = null
                _isRecording.value = false
            }
        }
    }

    private fun initializeAudioRecord() {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT)
        audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(sampleRate)
                .setChannelMask(CHANNEL_CONFIG)
                .build())
            .setBufferSizeInBytes(maxOf(minBufferSize, bufferSizeInBytes))
            .build()
    }

    private suspend fun analyzeAudioContinuously() {
        val byteBuffer = ByteBuffer.allocateDirect(bufferSizeInBytes).order(ByteOrder.nativeOrder())
        val floatBuffer = FloatArray(bufferSizeInFrames)
        val doubleBuffer = DoubleArray(fftSize * 2) 

        while (isActive && _isRecording.value) {
            val bytesRead = audioRecord?.read(byteBuffer, bufferSizeInBytes) ?: 0
            if (bytesRead > 0) {
                byteBuffer.rewind()
                byteBuffer.asFloatBuffer().get(floatBuffer)
                _audioBuffer.value = floatBuffer.copyOf()
                
                analyzeFrame(floatBuffer, doubleBuffer)
                byteBuffer.clear()
            }
            delay(16) 
        }
    }

    private fun analyzeFrame(floatBuffer: FloatArray, doubleBuffer: DoubleArray) {
        doubleBuffer.fill(0.0)
        for (i in floatBuffer.indices) {
            doubleBuffer[i] = floatBuffer[i].toDouble() * fftWindow[i]
        }

        fftAnalyzer.realForward(doubleBuffer)

        val magnitudes = FloatArray(fftSize / 2)
        for (i in magnitudes.indices) {
            val re = doubleBuffer[2 * i]
            val im = doubleBuffer[2 * i + 1]
            magnitudes[i] = sqrt(re * re + im * im).toFloat()
        }
        _fftMagnitudes.value = magnitudes
        _spectralCentroid.value = calculateSpectralCentroid(magnitudes, sampleRate)
    }

    private fun calculateSpectralCentroid(magnitudes: FloatArray, sampleRate: Int): Float {
        var weightedSum = 0f
        var magnitudeSum = 0f
        val freqStep = sampleRate.toFloat() / fftSize
        for (i in magnitudes.indices) {
            weightedSum += (i * freqStep) * magnitudes[i]
            magnitudeSum += magnitudes[i]
        }
        return if (magnitudeSum > 0) weightedSum / magnitudeSum else 0f
    }

    private fun nextPowerOfTwo(x: Int): Int {
        var n = x - 1
        n = n or (n shr 1)
        n = n or (n shr 2)
        n = n or (n shr 4)
        n = n or (n shr 8)
        n = n or (n shr 16)
        return n + 1
    }

    fun release() { stopRecording() }
}

