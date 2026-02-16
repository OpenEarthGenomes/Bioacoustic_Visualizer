package com.bioacoustic.visualizer.core.render

import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FilamentPointCloudRenderer(private val surfaceView: SurfaceView, private val engine: Engine) {
    private var renderer = engine.createRenderer()
    private var scene = engine.createScene()
    private var camera = engine.createCamera(engine.entityManager.create())
    private var view = engine.createView()
    private var swapChain: SwapChain? = null
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.LOG)
    private val pointCount = 1024
    private var vertexBuffer: VertexBuffer? = null
    private val renderableEntity = EntityManager.get().create()
    private val currentMagnitudes = FloatArray(pointCount)
    private val lerpFactor = 0.15f

    init {
        view.scene = scene
        view.camera = camera
        camera.setProjection(45.0, 1.0, 0.1, 100.0, Camera.Projection.PERSPECTIVE)
        camera.lookAt(0.0, 2.0, 8.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)

        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
            }
            override fun onDetachedFromSurface() { swapChain?.let { engine.destroySwapChain(it); swapChain = null } }
            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                camera.setProjection(45.0, width.toDouble() / height.toDouble(), 0.1, 100.0, Camera.Projection.PERSPECTIVE)
            }
        }
        uiHelper.attachTo(surfaceView)
        setupPointCloud()
    }

    private fun setupPointCloud() {
        vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1).vertexCount(pointCount)
            .attribute(VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 24)
            .attribute(VertexAttribute.COLOR, 0, VertexBuffer.AttributeType.FLOAT3, 12, 24)
            .build(engine)

        RenderableManager.Builder(1)
            .boundingBox(Box(0.0f, 0.0f, 0.0f, 5.0f, 5.0f, 5.0f))
            .geometry(0, RenderableManager.PrimitiveType.POINTS, vertexBuffer!!, 0, pointCount)
            .culling(false).build(engine, renderableEntity)
        scene.addEntity(renderableEntity)
    }

    fun updateVisuals(newMagnitudes: FloatArray) {
        val buffer = ByteBuffer.allocateDirect(pointCount * 6 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (i in 0 until pointCount) {
            val targetMag = if (i < newMagnitudes.size) newMagnitudes[i] else 0f
            currentMagnitudes[i] += (targetMag - currentMagnitudes[i]) * lerpFactor
            val mag = currentMagnitudes[i]
            val angle = (i.toFloat() / pointCount) * 2.0 * Math.PI
            val radius = 1.5f + mag * 4.0f
            
            buffer.put((radius * Math.cos(angle)).toFloat()).put((i.toFloat() / pointCount) * 6.0f - 3.0f).put((radius * Math.sin(angle)).toFloat())
            buffer.put((1.0f - (i.toFloat() / pointCount)) * (0.5f + mag)).put(mag * 0.8f).put((i.toFloat() / pointCount) * (0.5f + mag))
        }
        buffer.flip()
        vertexBuffer?.setBufferAt(engine, 0, buffer)
    }

    fun render(frameTimeNanos: Long) {
        if (!uiHelper.isReadyToRender) return
        val currentSwapChain = swapChain
        if (currentSwapChain != null && renderer.beginFrame(currentSwapChain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }
}
