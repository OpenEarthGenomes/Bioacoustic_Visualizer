package com.bioacoustic.visualizer.core.render

import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class FilamentPointCloudRenderer(private val surfaceView: SurfaceView) {
    private var engine: Engine
    private var renderer: Renderer
    private var scene: Scene
    private var view: View
    private var camera: Camera
    private var swapChain: SwapChain? = null
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.REPORT)

    // Pontfelhő adatok
    private var vertexBuffer: VertexBuffer? = null
    private var material: Material? = null
    private var renderable: Int = -1

    init {
        // 1. A motor elindítása (Itt hívódik meg a "rejtett" C++ rész)
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())

        view.scene = scene
        view.camera = camera

        setupUiHelper()
    }

    private fun setupUiHelper() {
        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Any) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
            }

            override fun onDetachedFromSurface() {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = null
            }

            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                val aspect = width.toDouble() / height.toDouble()
                camera.setProjection(45.0, aspect, 0.1, 100.0, Camera.Projection.PERSPECTIVE)
            }
        }
        uiHelper.attachTo(surfaceView)
    }

    fun updatePoints(points: FloatArray) {
        // Itt alakítjuk át a Kotlin FloatArray-t a GPU számára emészthető Buffer-ré
        val buffer = ByteBuffer.allocateDirect(points.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatArrayBuffer()
        buffer.put(points).flip()

        // Ha még nincs VertexBuffer, létrehozzuk (Android 16 memóriakezelés barát módon)
        if (vertexBuffer == null) {
            vertexBuffer = VertexBuffer.Builder()
                .bufferCount(1)
                .vertexCount(points.size / 3)
                .attribute(VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
                .build(engine)
        }
        vertexBuffer?.setBufferAt(engine, 0, buffer)
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
        // Nagyon fontos a tiszta lezárás, különben a One UI 8 memóriaszivárgást jelez
        uiHelper.detach()
        engine.destroy() 
    }
}
