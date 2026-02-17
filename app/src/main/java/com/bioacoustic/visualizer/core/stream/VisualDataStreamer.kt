package com.bioacoustic.visualizer.core.stream

import com.bioacoustic.visualizer.core.audio.AudioAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * VisualDataStreamer: A hangadatokat alakítja át 3D-s pontfelhő koordinátákká.
 */
class VisualDataStreamer(private val audioAnalyzer: AudioAnalyzer) {

    // Simítási faktor: segít, hogy a pontok mozgása elegánsabb legyen
    private var previousMagnitudes: FloatArray? = null
    private val smoothing = 0.2f 

    fun getVisualData(): Flow<FloatArray> {
        return audioAnalyzer.fftData.map { magnitudes ->
            // Ha ez az első futás, inicializáljuk a tárolót
            if (previousMagnitudes == null || previousMagnitudes!!.size != magnitudes.size) {
                previousMagnitudes = magnitudes.copyOf()
            }

            val points = FloatArray(magnitudes.size * 3)
            
            for (i in magnitudes.indices) {
                // Egyszerű simítás (lerp): az új érték és a régi átlaga
                val smoothedValue = (magnitudes[i] * (1f - smoothing)) + (previousMagnitudes!![i] * smoothing)
                previousMagnitudes!![i] = smoothedValue

                // X tengely: A frekvenciák elosztása a képernyőn (-1.0-tól +1.0-ig)
                // Így a mély hangok balra, a magasak jobbra kerülnek
                points[i * 3] = (i.toFloat() / magnitudes.size) * 2.0f - 1.0f
                
                // Y tengely: A hangerő (amplitúdó) mértéke. 
                // A 8.0f-es szorzó teszi látványossá a mozgást a Samsung kijelzőjén.
                points[i * 3 + 1] = smoothedValue * 8.0f
                
                // Z tengely: Mélység. Egyelőre 0, de később itt kaphat távolságot a pontfelhő.
                points[i * 3 + 2] = 0.0f
            }
            points
        }
    }
}
