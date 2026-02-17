package com.bioacoustic.visualizer.core.stream

import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VisualDataStreamer(private val audioAnalyzer: AudioAnalyzer) {
    
    // Ez a függvény készíti el a 3D pontokat a MainActivity számára
    fun getVisualData(): Flow<FloatArray> {
        return audioAnalyzer.audioFlow.map { audioData ->
            // A hangminta hossza (pl. 1024)
            val size = audioData.size
            // Minden hangmintához 3 koordináta kell (X, Y, Z)
            val points = FloatArray(size * 3) 
            
            for (i in 0 until size) {
                // X: -1.0 és 1.0 között a képernyőn (vízszintes elhelyezkedés)
                val x = (i.toFloat() / size.toFloat()) * 2.0f - 1.0f
                
                // Y: A hangerő alapján (függőleges kitérés)
                // A Short értéket (max 32768) normalizáljuk
                val y = audioData[i].toFloat() / 32768.0f
                
                // Z: Mélység (egyelőre fix 0, hogy tiszta legyen a hullámformád)
                val z = 0.0f
                
                points[i * 3] = x
                points[i * 3 + 1] = y
                points[i * 3 + 2] = z
            }
            points
        }
    }
}
