package com.bioacoustic.visualizer.core.stream

import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import com.bioacoustic.visualizer.core.model.SlidingPointBuffer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class VisualDataStreamer(
    private val audioAnalyzer: AudioAnalyzer,
    private val pointBuffer: SlidingPointBuffer,
    private val coroutineScope: CoroutineScope
) {
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private var streamingJob: Job? = null

    fun startStreaming() {
        if (_isStreaming.value) return
        _isStreaming.value = true

        streamingJob = coroutineScope.launch {
            // Kombináljuk a spektrális adatokat és az amplitúdót
            combine(
                audioAnalyzer.spectralCentroid,
                audioAnalyzer.fftMagnitudes,
                audioAnalyzer.audioBuffer
            ) { centroid, magnitudes, waveform ->
                if (magnitudes != null && waveform != null) {
                    // Kiszámoljuk az átlagos amplitúdót (hangerőt)
                    val amplitude = waveform.map { kotlin.math.abs(it) }.average().toFloat()
                    
                    // Keressük a domináns frekvenciát
                    val maxIndex = magnitudes.indices.maxByOrNull { magnitudes[it] } ?: 0
                    val dominantFreq = maxIndex * (44100f / (magnitudes.size * 2))

                    // Átadjuk a puffernek, ami kiszámolja a 3D koordinátákat
                    pointBuffer.addPoint(
                        frequency = dominantFreq,
                        amplitude = amplitude,
                        spectralCentroid = centroid
                    )
                }
            }.collect()
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _isStreaming.value = false
    }
}

