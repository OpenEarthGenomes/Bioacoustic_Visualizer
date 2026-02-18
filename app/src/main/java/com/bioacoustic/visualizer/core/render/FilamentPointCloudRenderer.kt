package com.bioacoustic.visualizer.core.render

import android.view.SurfaceView
import com.google.android.filament.*

class FilamentPointCloudRenderer(private val surfaceView: SurfaceView) {
    private var engine: Engine? = null
    private var renderer: Renderer? = null
    private var scene: Scene? = null
    private var camera: Camera? = null
    private var view: View? = null
    private var swapChain: SwapChain? = null

    init {
        try {
            // Minimális inicializálás az összeomlás elkerülésére
            engine = Engine.create()
            renderer = engine?.createRenderer()
            scene = engine?.createScene()
            camera = engine?.createCamera(engine!!.entityManager.create())
            view = engine?.createView()?.apply {
                this.scene = this@FilamentPointCloudRenderer.scene
                this.camera = this@FilamentPointCloudRenderer.camera
            }
            // A SwapChain-t az első renderelésnél fogjuk létrehozni, ha a Surface készen áll
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun render(frameTimeNanos: Long) {
        // Most még üresen hagyjuk a rajzolást, csak a motort teszteljük
    }

    fun updatePoints(points: FloatArray) {
        // Most még nem csinál semmit
    }

    fun release() {
        try {
            engine?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
