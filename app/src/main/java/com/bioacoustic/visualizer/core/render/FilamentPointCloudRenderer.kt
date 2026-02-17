package com.bioacoustic.visualizer.core.render

import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FilamentPointCloudRenderer(private val surfaceView: SurfaceView) {
    private var engine: Engine = Engine.create()
    private var renderer: Renderer = engine.createRenderer()
    private var scene: Scene = engine.createScene()
    private var camera: Camera = engine.createCamera(engine.entityManager.create())
    private var view: View = engine.createView()
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.REPORT)

    private var vertexBuffer: VertexBuffer? = null
    private var indexBuffer: IndexBuffer? = null
    private var renderable: Int = engine.entityManager.create()

    init {
        view.scene = scene
        view.camera = camera
        camera.setProjection(45.0, surfaceView.width.toDouble() / surfaceView.height.toDouble(), 0.1, 100.0, Camera.Fov.VERTICAL)
        
        uiHelper.renderCallback = object : UiHelper.RenderCallback {
            override fun onNativeWindowChanged(surface: android.view.Surface) {
                view.viewport = Viewport(0, 0, surfaceView.width, surfaceView.height)
            }
            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                camera.setProjection(45.0, width.toDouble() / height.toDouble(), 0.1, 100.0, Camera.Fov.VERTICAL)
            }
        }
        uiHelper.attachTo(surfaceView)
        
        val light = engine.entityManager.create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 1.0f, 1.0f)
            .intensity(100000.0f)
            .direction(0.0f, -1.0f, -1.0f)
            .build(engine, light)
        scene.addEntity(light)
    }

    fun updatePoints(points: FloatArray) {
        val vertexCount = points.size / 3
        if (vertexCount == 0) return

        val buffer = ByteBuffer.allocateDirect(points.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
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
                .geometry(0, RenderableManager.PrimitiveType.POINTS, vertexBuffer!!, indexBuffer ?: createDummyIndexBuffer())
                .build(engine, renderable)
            scene.addEntity(renderable)
        }
    }

    private fun createDummyIndexBuffer(): IndexBuffer {
        val ib = IndexBuffer.Builder().indexCount(0).build(engine)
        this.indexBuffer = ib
        return ib
    }

    fun render(frameTimeNanos: Long) {
        if (renderer.beginFrame(surfaceView.holder.surface, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun release() {
        uiHelper.detach()
        engine.destroyEntity(renderable)
        engine.destroy()
    }
}
