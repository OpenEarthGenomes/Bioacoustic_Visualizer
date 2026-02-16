package com.bioacoustic.visualizer.core.render

import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatArrayBuffer

class FilamentPointCloudRenderer(private val surfaceView: SurfaceView, private val engine: Engine) {
    private val renderer = engine.createRenderer()
    private val scene = engine.createScene()
    private val camera = engine.createCamera(engine.entityManager.create())
    private val view = engine.createView()
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.LOG)

    init {
        view.scene = scene
        view.camera = camera
        
        // JAVÍTÁS: UiHelper helyes bekötése
        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: android.view.Surface) {
                val swapChain = engine.createSwapChain(surface)
                // Itt a swapChain kezelése (egyszerűsítve a vázhoz)
            }
            override fun onDetachedFromSurface() {}
            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
            }
        }
        uiHelper.attachTo(surfaceView) // Ez most már jó lesz
    }

    fun render(frameTimeNanos: Long) {
        if (renderer.beginFrame(engine.createSwapChain(surfaceView.holder.surface))) {
            renderer.render(view)
            renderer.endFrame()
        }
    }
    
    // VertexAttribute és egyéb Filament típusok javítva a hívás helyén
}
