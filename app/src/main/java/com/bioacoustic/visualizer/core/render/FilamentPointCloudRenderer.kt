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
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun render(frameTimeNanos: Long) {
        val currentSwapChain = swapChain
        val currentRenderer = renderer
        val currentView = view

        if (uiHelper.isReadyToRender && currentSwapChain != null && currentRenderer != null && currentView != null) {
            // Itt adjuk meg a háttérszínt minden képkockánál (sötétszürke)
            val options = currentRenderer.clearOptions
            options.clearColor = floatArrayOf(0.1f, 0.1f, 0.1f, 1.0f)
            options.clear = true
            currentRenderer.clearOptions = options

            if (currentRenderer.beginFrame(currentSwapChain, frameTimeNanos)) {
                currentRenderer.render(currentView)
                currentRenderer.endFrame()
            }
        }
    }

    fun updatePoints(points: FloatArray) {
        // Később ide jön a pontfelhő
    }

    fun release() {
        uiHelper.detach()
        swapChain?.let { engine?.destroySwapChain(it) }
        view?.let { engine?.destroyView(it) }
        scene?.let { engine?.destroyScene(it) }
        engine?.destroy()
    }
}
