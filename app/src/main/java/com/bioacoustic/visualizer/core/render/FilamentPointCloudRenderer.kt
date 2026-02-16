package com.bioacoustic.visualizer.core.render

import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class FilamentPointCloudRenderer(
    private val surfaceView: SurfaceView,
    private val engine: Engine
) {
    private var renderer: Renderer = engine.createRenderer()
    private var scene: Scene = engine.createScene()
    private var camera: Camera = engine.createCamera(engine.entityManager.create())
    private var view: View = engine.createView()
    private var swapChain: SwapChain? = null
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.LOG)

    private var vertexBuffer: VertexBuffer? = null
    private var material: Material? = null
    private var materialInstance: MaterialInstance? = null
    private val renderableEntity = EntityManager.get().create()

    init {
        view.scene = scene
        view.camera = camera
        
        // Kamera alaphelyzetbe állítása
        camera.setProjection(45.0, surfaceView.width.toDouble() / surfaceView.height.toDouble(), 0.1, 100.0, Camera.Projection.PERSPECTIVE)
        camera.lookAt(0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)

        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
            }

            override fun onDetachedFromSurface() {
                swapChain?.let {
                    engine.destroySwapChain(it)
                    swapChain = null
                }
            }

            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                camera.setProjection(45.0, width.toDouble() / height.toDouble(), 0.1, 100.0, Camera.Projection.PERSPECTIVE)
            }
        }
        uiHelper.attachTo(surfaceView)
        
        setupScene()
    }

    private fun setupScene() {
        // Alap vertex buffer létrehozása (pozíció + szín)
        vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(1000)
            .attribute(VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 24)
            .attribute(VertexAttribute.COLOR, 0, VertexBuffer.AttributeType.FLOAT3, 12, 24)
            .build(engine)

        // Itt egy egyszerű pontfelhő renderelhető objektum létrehozása
        RenderableManager.Builder(1)
            .boundingBox(Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
            .geometry(0, RenderableManager.PrimitiveType.POINTS, vertexBuffer!!, 0, 1000)
            .build(engine, renderableEntity)
            
        scene.addEntity(renderableEntity)
    }

    fun render(frameTimeNanos: Long) {
        if (!uiHelper.isReadyToRender) return

        val currentSwapChain = swapChain
        if (currentSwapChain != null && renderer.beginFrame(currentSwapChain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun destroy() {
        uiHelper.detach()
        engine.destroyEntity(renderableEntity)
        vertexBuffer?.let { engine.destroyVertexBuffer(it) }
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
    }
}
