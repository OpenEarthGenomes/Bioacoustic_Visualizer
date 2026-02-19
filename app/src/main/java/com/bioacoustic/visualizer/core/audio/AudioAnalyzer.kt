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
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_FLOAT
    )

    @SuppressLint("MissingPermission")
    private var audioRecord: AudioRecord? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        
        thread {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, // Közvetlen mikrofon forrás
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
                    // Az adatokat átadjuk a vízesésnek
                    // Mivel ez már eleve magnitúdó-szerű adat, közvetlenül skálázhatjuk
                    val fftSim = FloatArray(256)
                    for (i in 0 until 256) {
                        fftSim[i] = if (i < audioData.size) Math.abs(audioData[i]) * 100f else 0f
                    }
                    renderer.updatePoints(fftSim)
                }
            }
            audioRecord?.stop()
            audioRecord?.release()
        }
    }

    fun stop() {
        isRunning = false
    }
}
