package com.bioacoustic.visualizer.core.render

import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.VertexBuffer.VertexAttribute // EZ HIÁNYZOTT!
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FilamentPointCloudRenderer(private val surfaceView: SurfaceView) {
    private var engine: Engine = Engine.create()
    private var renderer: Renderer = engine.createRenderer()
    private var scene: Scene = engine.createScene()
    private var camera: Camera = engine.createCamera(engine.entityManager.create())
    private var view: View = engine.createView()
    private var swapChain: SwapChain? = null
    private val uiHelper = UiHelper()

    private var vertexBuffer: VertexBuffer? = null
    private var indexBuffer: IndexBuffer? = null
    private var renderable: Int = engine.entityManager.create()

    init {
        view.scene = scene
        view.camera = camera
        
        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
                view.viewport = Viewport(0, 0, surfaceView.width, surfaceView.height)
            }
            override fun onDetachedFromSurface() {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = null
            }
            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                val aspect = width.toDouble() / height.toDouble()
                camera.setProjection(45.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL)
            }
        }
        uiHelper.attachTo(surfaceView)
    }

    fun updatePoints(points: FloatArray) {
        val vertexCount = points.size / 3
        if (vertexCount == 0) return

        val buffer = ByteBuffer.allocateDirect(points.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(points).flip()

        vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(vertexCount)
            .attribute(VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
            .build(engine)
        
        vertexBuffer?.setBufferAt(engine, 0, buffer)

        if (!scene.hasEntity(renderable)) {
            RenderableManager.Builder(1)
                .boundingBox(Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
                .geometry(0, RenderableManager.PrimitiveType.POINTS, vertexBuffer!!, createIndexBuffer(vertexCount))
                .build(engine, renderable)
            scene.addEntity(renderable)
        }
    }

    private fun createIndexBuffer(count: Int): IndexBuffer {
        val indices = ShortArray(count) { it.toShort() }
        val buffer = ByteBuffer.allocateDirect(count * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        buffer.put(indices).flip()
        
        val ib = IndexBuffer.Builder().indexCount(count).build(engine)
        ib.setBuffer(engine, buffer)
        return ib
    }

    fun render(frameTimeNanos: Long) {
        val sc = swapChain ?: return
        if (renderer.beginFrame(sc, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun release() {
        uiHelper.detach()
        swapChain?.let { engine.destroySwapChain(it) }
        engine.destroyEntity(renderable)
        engine.destroy()
    }
}
