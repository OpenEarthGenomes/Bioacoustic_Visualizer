package com.bioacoustic.visualizer.core.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class KotlinPointRenderer : GLSurfaceView.Renderer {
    private var vertexBuffer: FloatBuffer? = null
    private var program: Int = 0
    private val maxHistory = 50 // Ennyi időpillanatot látunk egyszerre
    private val pointsPerFrame = 256
    private var historyData = FloatArray(maxHistory * pointsPerFrame * 3)
    var sensitivity: Float = 5.0f

    private val vertexShaderCode = """
        attribute vec4 vPosition;
        varying float vIntensity;
        void main() {
            gl_Position = vPosition;
            gl_PointSize = 8.0; 
            vIntensity = vPosition.y;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying float vIntensity;
        void main() {
            float y = (vIntensity + 0.6) * 1.5;
            vec3 color = mix(vec3(0.0, 0.1, 0.4), vec3(0.0, 1.0, 0.8), clamp(y, 0.0, 0.5) * 2.0);
            if(y > 0.5) color = mix(color, vec3(1.0, 0.0, 0.0), (y - 0.5) * 2.0);
            gl_FragColor = vec4(color, 1.0);
        }
    """.trimIndent()

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.05f, 1.0f)
        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vs)
            GLES20.glAttachShader(this, fs)
            GLES20.glLinkProgram(this)
        }
    }

    fun updatePoints(fftData: FloatArray) {
        // Régi adatok eltolása (Spektrogram effekt)
        System.arraycopy(historyData, 0, historyData, pointsPerFrame * 3, (maxHistory - 1) * pointsPerFrame * 3)

        for (i in 0 until pointsPerFrame) {
            val x = (i.toFloat() / pointsPerFrame) * 2f - 1f
            val rawAmp = Math.pow(fftData[i].toDouble(), 0.4).toFloat() * sensitivity * 0.8f
            
            historyData[i * 3] = x
            historyData[i * 3 + 1] = rawAmp - 0.6f
            historyData[i * 3 + 2] = 0f
        }

        // Időbeli eltolás (Z tengely vagy X eltolás helyett most maradjon statikus ugrálás, 
        // de az intenzitás maradandóbb lesz)
        val bb = ByteBuffer.allocateDirect(historyData.size * 4).order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer().put(historyData).apply { position(0) }
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val buffer = vertexBuffer ?: return
        GLES20.glUseProgram(program)
        val pos = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 12, buffer)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, maxHistory * pointsPerFrame)
    }

    override fun onSurfaceChanged(u: GL10?, w: Int, h: Int) = GLES20.glViewport(0, 0, w, h)
    private fun loadShader(t: Int, s: String) = GLES20.glCreateShader(t).also { GLES20.glShaderSource(it, s); GLES20.glCompileShader(it) }
}

