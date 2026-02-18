package com.bioacoustic.visualizer.core.render

import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FilamentPointCloudRenderer(private val surfaceView: SurfaceView) {
    private var engine: Engine? = null
    private var renderer: Renderer? = null
    private var scene: Scene? = null
    private var camera: Camera? = null
    private var view: View? = null
    private var swapChain: SwapChain? = null
    private val uiHelper = UiHelper()

    private var vertexBuffer: VertexBuffer? = null
    private var indexBuffer: IndexBuffer? = null
    private var renderable: Int? = null
    private val maxPoints = 1024

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
            setupPointCloud()
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
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setupPointCloud() {
        val e = engine ?: return
        
        vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(maxPoints)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
            .build(e)

        // RADIKÁLIS JAVÍTÁS: 
        // Ha a fordító nem ismeri fel az IndexType-ot, kikerüljük a Builder-t ennél a résznél, 
        // vagy kényszerítjük a pontos elérési utat.
        indexBuffer = IndexBuffer.Builder()
            .indexCount(maxPoints)
            .bufferType(com.google.android.filament.IndexBuffer.IndexType.USHORT)
            .build(e)

        renderable = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(0.0f, 0.0f, 0.0f, 10.0f, 10.0f, 10.0f))
            .geometry(0, RenderableManager.PrimitiveType.POINTS, vertexBuffer!!, indexBuffer!!, 0, maxPoints)
            .build(e, renderable!!)
        
        scene?.addEntity(renderable!!)
    }

    fun updatePoints(points: FloatArray) {
        val e = engine ?: return
        if (points.isEmpty()) return
        
        val floatBuffer = ByteBuffer.allocateDirect(maxPoints * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            
        // Adatok feltöltése: X, Y, Z koordináták
        for (i in 0 until Math.min(points.size / 3, maxPoints)) {
            floatBuffer.put(points[i * 3])
            floatBuffer.put(points[i * 3 + 1])
            floatBuffer.put(points[i * 3 + 2])
        }
        floatBuffer.flip()
        vertexBuffer?.setBufferAt(e, 0, floatBuffer)
    }

    fun render(frameTimeNanos: Long) {
        val sc = swapChain ?: return
        val r = renderer ?: return
        val v = view ?: return
        if (uiHelper.isReadyToRender) {
            r.clearOptions = r.clearOptions.apply {
                clearColor = floatArrayOf(0.05f, 0.05f, 0.1f, 1.0f) // Sötétkék háttér a szürke helyett
                clear = true
            }
            if (r.beginFrame(sc, frameTimeNanos)) {
                r.render(v)
                r.endFrame()
            }
        }
    }

    fun release() {
        uiHelper.detach()
        renderable?.let { engine?.destroyEntity(it) }
        vertexBuffer?.let { engine?.destroyVertexBuffer(it) }
        indexBuffer?.let { engine?.destroyIndexBuffer(it) }
        swapChain?.let { engine?.destroySwapChain(it) }
        view?.let { engine?.destroyView(it) }
        scene?.let { engine?.destroyScene(it) }
        engine?.destroy()
    }
}

