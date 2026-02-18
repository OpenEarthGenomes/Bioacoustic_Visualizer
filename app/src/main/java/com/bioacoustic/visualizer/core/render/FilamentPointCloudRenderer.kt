package com.bioacoustic.visualizer.core.render

import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
            override fun onNativeWindowChanged(surface: Any) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
            }
            override fun onDetachedFromSurface() { swapChain = null }
            override fun onResized(w: Int, h: Int) {
                view.viewport = Viewport(0, 0, w, h)
                camera.setProjection(45.0, w.toDouble()/h.toDouble(), 0.1, 100.0, Camera.Projection.PERSPECTIVE)
            }
        }
    }

    init { uiHelper.attachTo(surfaceView) }

    fun updatePoints(points: FloatArray) {
        // Itt jönne a VertexBuffer logikája a pontfelhőhöz
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
