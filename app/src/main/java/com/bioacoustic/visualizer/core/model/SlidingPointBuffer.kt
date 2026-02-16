package com.bioacoustic.visualizer.core.model

import kotlin.math.*

class SlidingPointBuffer(
    private val maxDurationSeconds: Int = 30,
    private val maxPoints: Int = 15000
) {
    private val points = mutableListOf<VisualPoint>()
    private var nextId = 0L
    private val startTime = System.currentTimeMillis()

    fun addPoint(frequency: Float, amplitude: Float, spectralCentroid: Float): VisualPoint {
        val now = System.currentTimeMillis()
        
        // --- JAVÍTOTT SPIRÁL LOGIKA A "FA" ALAKZATHOZ ---
        // Az idő emeli a pontot felfelé (Y tengely)
        // A frekvencia forgatja a pontot a középpont körül
        // Az amplitúdó határozza meg a távolságot a törzstől (radius)
        
        val timeFactor = (now - startTime) * 0.001f 
        val angle = spectralCentroid * 0.002f + timeFactor * 0.5f
        val radius = 0.2f + (amplitude * 2.5f)
        val height = timeFactor * 0.2f // Felfelé növekedés

        val x = (radius * cos(angle)).toFloat()
        val y = height
        val z = (radius * sin(angle)).toFloat()

        // Szín: Frekvencia alapján (HUE)
        val hue = (spectralCentroid % 2000) / 2000f
        val rgb = hsvToRgb(hue, 0.7f, 1.0f)

        val point = VisualPoint(
            id = nextId++,
            position = floatArrayOf(x, y, z),
            color = floatArrayOf(rgb[0], rgb[1], rgb[2], 0.8f),
            size = 0.05f + (amplitude * 0.2f),
            intensity = min(1.0f, amplitude * 2.0f),
            timestamp = now
        )

        points.add(point)
        
        // Takarítás: régi pontok kidobása
        val cutoff = now - (maxDurationSeconds * 1000L)
        points.removeAll { it.timestamp < cutoff }
        if (points.size > maxPoints) points.removeAt(0)

        return point
    }

    fun getPointsForRendering(): List<VisualPoint> = points.toList()

    fun clear() {
        points.clear()
        nextId = 0L
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): FloatArray {
        val i = (h * 6).toInt()
        val f = h * 6 - i
        val p = v * (1 - s)
        val q = v * (1 - f * s)
        val t = v * (1 - (1 - f) * s)
        return when (i % 6) {
            0 -> floatArrayOf(v, t, p)
            1 -> floatArrayOf(q, v, p)
            2 -> floatArrayOf(p, v, t)
            3 -> floatArrayOf(p, q, v)
            4 -> floatArrayOf(t, p, v)
            else -> floatArrayOf(v, p, q)
        }
    }
}

