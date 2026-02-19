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
    private var pointCount: Int = 0
    var sensitivity: Float = 1.0f

    private val vertexShaderCode = """
        attribute vec4 vPosition;
        varying float vAmp;
        void main() {
            gl_Position = vPosition;
            gl_PointSize = 14.0;
            vAmp = vPosition.y; 
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying float vAmp;
        void main() {
            float val = (vAmp + 0.5) * 1.2; 
            vec3 cold = vec3(0.0, 0.4, 1.0); // Sötétkék
            vec3 warm = vec3(0.0, 1.0, 0.8); // Türkiz
            vec3 hot = vec3(1.0, 0.1, 0.0);  // Vörös
            
            vec3 color;
            if(val < 0.5) {
                color = mix(cold, warm, val * 2.0);
            } else {
                color = mix(warm, hot, (val - 0.5) * 2.0);
            }
            gl_FragColor = vec4(color, 1.0);
        }
    """.trimIndent()

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.01f, 0.03f, 0.05f, 1.0f)
        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
    }

    fun updatePoints(fftData: FloatArray) {
        pointCount = fftData.size
        val vertices = FloatArray(pointCount * 3)
        for (i in fftData.indices) {
            vertices[i * 3] = (i.toFloat() / pointCount.toFloat()) * 2f - 1f
            // Logaritmikus felerősítés a valós idejű érzékeléshez
            val logAmp = Math.log10(1.0 + fftData[i].toDouble()).toFloat() * sensitivity * 6f
            vertices[i * 3 + 1] = logAmp - 0.7f
            vertices[i * 3 + 2] = 0f
        }

        val bb = ByteBuffer.allocateDirect(vertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer().apply {
            put(vertices)
            position(0)
        }
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (vertexBuffer == null) return
        GLES20.glUseProgram(program)
        val pos = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointCount)
    }

    override fun onSurfaceChanged(u: GL10?, w: Int, h: Int) = GLES20.glViewport(0, 0, w, h)
    private fun loadShader(t: Int, s: String) = GLES20.glCreateShader(t).also { 
        GLES20.glShaderSource(it, s); GLES20.glCompileShader(it) 
    }
}
