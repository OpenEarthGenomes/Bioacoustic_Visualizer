package com.bioacoustic.visualizer.core.render

import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper

class FilamentPointCloudRenderer(private val surfaceView: SurfaceView) {
    private val engine = Engine.create()
    private val renderer = engine.createRenderer()
    private val scene = engine.createScene()
    private val camera = engine.createCamera(engine.entityManager.create())
    private val view = engine.createView().apply { 
        this.scene = this@FilamentPointCloudRenderer.scene
        this.camera = this@FilamentPointCloudRenderer.camera
    }
    private var swapChain: SwapChain? = null
    private val uiHelper = UiHelper().apply {
        renderCallback = object : UiHelper.RendererCallback {
            // JAVÍTVA: Az új Filament verzióban ez a pontos név és paraméter
            override fun onNativeWindowChanged(surface: Any) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
            }
            override fun onDetachedFromSurface() {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = null
            }
            override fun onResized(w: Int, h: Int) {
                view.viewport = Viewport(0, 0, w, h)
                // JAVÍTVA: A Projection helyett Fov paraméter kell
                camera.setProjection(45.0, w.toDouble()/h.toDouble(), 0.1, 100.0, Camera.Fov.VERTICAL)
            }
        }
    }

    init { uiHelper.attachTo(surfaceView) }

    fun updatePoints(points: FloatArray) {
        // Itt jön majd a pontfelhő frissítése
    }

    fun render(frameTimeNanos: Long) {
        if (uiHelper.isReadyToRender && swapChain != null) {
            if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                renderer.render(view)
                renderer.endFrame()
            }
        }
    }

    fun release() {
        uiHelper.detach()
        engine.destroy()
    }
}
