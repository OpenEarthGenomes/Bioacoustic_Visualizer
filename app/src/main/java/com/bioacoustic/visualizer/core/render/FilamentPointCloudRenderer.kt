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
            // A motort csak akkor hozzuk létre, ha a rendszer engedi
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
                    // Itt történik a varázslat: csak akkor csinálunk swapChaint, ha van felület
                    swapChain?.let { engine?.destroySwapChain(it) }
                    try {
                        swapChain = engine?.createSwapChain(surface)
                    } catch (e: Exception) {
                        swapChain = null
                    }
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
            // Ha a Filament összeomlik az elején, az app akkor is fusson tovább (üresen)
            e.printStackTrace()
        }
    }

    fun render(frameTimeNanos: Long) {
        // Kőkemény biztonsági ellenőrzés
        val currentSwapChain = swapChain
        val currentRenderer = renderer
        val currentView = view

        if (uiHelper.isReadyToRender && currentSwapChain != null && currentRenderer != null && currentView != null) {
            if (currentRenderer.beginFrame(currentSwapChain, frameTimeNanos)) {
                currentRenderer.render(currentView)
                currentRenderer.endFrame()
            }
        }
    }

    fun updatePoints(points: FloatArray) {
        if (points.isEmpty()) return
        // Ide jön majd a pontok rajzolása, de most még csak ne omoljon össze
    }

    fun release() {
        uiHelper.detach()
        swapChain?.let { engine?.destroySwapChain(it) }
        view?.let { engine?.destroyView(it) }
        scene?.let { engine?.destroyScene(it) }
        engine?.destroy()
    }
}
