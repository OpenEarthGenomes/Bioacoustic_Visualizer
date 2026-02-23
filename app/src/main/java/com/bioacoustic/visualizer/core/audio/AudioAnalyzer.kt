package com.bioacoustic.visualizer.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.bioacoustic.visualizer.core.render.KotlinPointRenderer
import kotlin.concurrent.thread
import kotlin.math.sqrt

class AudioAnalyzer(private val renderer: KotlinPointRenderer) {
    private var isRunning = false
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        isRunning = true
        
        thread {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                bufferSize
            )

            val audioData = FloatArray(bufferSize)
            audioRecord?.startRecording()

            while (isRunning) {
                val read = audioRecord?.read(audioData, 0, bufferSize, AudioRecord.READ_BLOCKING) ?: 0
                if (read > 0) {
                    // Egyszerűsített FFT-szerű vizualizáció a gyorsasághoz
                    renderer.updatePoints(audioData) 
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
