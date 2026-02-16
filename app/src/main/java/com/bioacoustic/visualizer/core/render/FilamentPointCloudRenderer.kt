package com.bioacoustic.visualizer.core.render

import android.content.Context
import android.view.SurfaceView
import com.bioacoustic.visualizer.core.model.VisualPoint
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class FilamentPointCloudRenderer(private val context: Context) {
    companion object {
        private const val FLOATS_PER_VERTEX = 8 // position(3) + color(4) + size/intensity(1)
        private const val BYTES_PER_FLOAT = 4
        private const val STRIDE = FLOATS_PER_VERTEX * BYTES_PER_FLOAT
    }

    private var engine: Engine = Engine.create()
    private var scene: Scene = engine.createScene()
    private var view: View = engine.createView()
    private var camera: Camera = engine.createCamera(EntityManager.get().create())
    private var renderer: Renderer = engine.createRenderer()
    
    private var vertexBuffer: VertexBuffer? = null
    private var material: Material? = null
    private var materialInstance: MaterialInstance? = null
    private var pointCloudEntity = EntityManager.get().create()

    fun initialize(surfaceView: SurfaceView, uiHelper: UiHelper) {
        view.scene = scene
        view.camera = camera
        
        // Alapértelmezett sötét háttér
        scene.skybox = Skybox.Builder().color(0.05f, 0.05f, 0.05f, 1.0f).build(engine)
        
        // Kamera pozíció (szemben a "fával")
        camera.setProjection(45.0, surfaceView.width.toDouble() / surfaceView.height.toDouble(), 0.1, 100.0, Camera.Fov.VERTICAL)
        camera.lookAt(0.0, 1.0, 5.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0)

        setupMaterial()
        uiHelper.attachTo(engine, surfaceView, renderer)
    }

    private fun setupMaterial() {
        // Mivel a .filamat fájl betöltése bonyolult, egy unlit (nem világított) alapanyagot használunk
        material = Material.Builder()
            .shading(Material.Shading.UNLIT)
            .build(engine)
        materialInstance = material?.defaultInstance
    }

    fun updatePoints(points: List<VisualPoint>) {
        if (points.isEmpty()) return

        // Vertex Buffer létrehozása vagy frissítése
        if (vertexBuffer == null || vertexBuffer!!.vertexCount < points.size) {
            vertexBuffer?.let { engine.destroyVertexBuffer(it) }
            vertexBuffer = VertexBuffer.Builder()
                .vertexCount(15000) // Fix méret a Samsung A35 stabilitása miatt
                .bufferCount(1)
                .attribute(VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, STRIDE)
                .attribute(VertexAttribute.COLOR, 0, VertexBuffer.AttributeType.FLOAT4, 12, STRIDE)
                .build(engine)
        }

        val buffer = ByteBuffer.allocateDirect(points.size * STRIDE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        points.forEach { point ->
            buffer.put(point.position) // x, y, z
            buffer.put(point.color)    // r, g, b, a
            buffer.put(point.size)     // méret (ha a shader támogatja)
        }
        buffer.flip()
        vertexBuffer?.setBufferAt(engine, 0, buffer)

        // Renderelhető objektum (pontfelhő) frissítése
        scene.removeEntity(pointCloudEntity)
        RenderableManager.Builder(1)
            .boundingBox(Box(0.0f, 0.0f, 0.0f, 10.0f, 10.0f, 10.0f))
            .geometry(0, RenderableManager.PrimitiveType.POINTS, vertexBuffer!!, 0, points.size)
            .material(0, materialInstance!!)
            .build(engine, pointCloudEntity)
        scene.addEntity(pointCloudEntity)
    }

    fun render(frameTimeNanos: Long) {
        if (renderer.beginFrame(view.viewHandle, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun destroy() {
        engine.destroyEntity(pointCloudEntity)
        engine.destroyScene(scene)
        engine.destroyView(view)
        engine.destroyRenderer(renderer)
        vertexBuffer?.let { engine.destroyVertexBuffer(it) }
        engine.destroy()
    }
}

