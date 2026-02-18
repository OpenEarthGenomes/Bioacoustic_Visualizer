package com.bioacoustic.visualizer.core.render

import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper

class FilamentPointCloudRenderer(private val surfaceView: SurfaceView) {
    private var engine: Engine? = null
    private var renderer: Renderer? = null
    private var scene: Scene? = null
    private var camera: Camera? = null
    private var view: View? = null
    private var swapChain: SwapChain? = null
    private val uiHelper = UiHelper()

    init {
        // Motor indítása csak óvatosan
        engine = Engine.create()
        renderer = engine?.createRenderer()
        scene = engine?.createScene()
        camera = engine?.createCamera(engine!!.entityManager.create())
        view = engine?.createView()?.apply {
            this.scene = this@FilamentPointCloudRenderer.scene
            this.camera = this@FilamentPointCloudRenderer.camera
        }

        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine?.destroySwapChain(it) }
                swapChain = engine?.createSwapChain(surface)
            }

            override fun onDetachedFromSurface() {
                swapChain?.let { engine?.destroySwapChain(it) }
                swapChain = null
            }

            override fun onResized(w: Int, h: Int) {
                view?.viewport = Viewport(0, 0, w, h)
                val aspect = w.toDouble() / h.toDouble()
                camera?.setProjection(45.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL)
            }
        }
        uiHelper.attachTo(surfaceView)
    }

    fun render(frameTimeNanos: Long) {
        // Csak akkor rajzolunk, ha MINDEN készen áll
        val currentSwapChain = swapChain
        if (uiHelper.isReadyToRender && currentSwapChain != null) {
            renderer?.let { r ->
                if (r.beginFrame(currentSwapChain, frameTimeNanos)) {
                    view?.let { v -> r.render(v) }
                    r.endFrame()
                }
            }
        }
    }

    fun updatePoints(points: FloatArray) {
        // Biztonsági ellenőrzés, hogy ne szálljon el, ha üres az adat
        if (points.isEmpty()) return
    }

    fun release() {
        uiHelper.detach()
        swapChain?.let { engine?.destroySwapChain(it) }
        view?.let { engine?.destroyView(it) }
        scene?.let { engine?.destroyScene(it) }
        engine?.destroy()
    }
}
