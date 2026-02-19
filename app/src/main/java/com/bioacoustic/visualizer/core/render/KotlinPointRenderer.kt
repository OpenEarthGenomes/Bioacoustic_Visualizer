package com.bioacoustic.visualizer.core.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class KotlinPointRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var textureId = 0
    private val texWidth = 256
    private val texHeight = 512
    private val textureData = FloatArray(texWidth * texHeight)
    private var writeRow = 0
    
    var sensitivity = 1.0f
    var isSnapshotMode = false

    private val vertexData = floatArrayOf(
        -1f, -1f, 0f, 
         1f, -1f, 0f, 
        -1f,  1f, 0f, 
         1f,  1f, 0f
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.0f, 0.0f, 0.05f, 1.0f)
        
        val vsCode = """#version 300 es
            layout(location = 0) in vec3 aPos;
            out vec2 vTexCoord;
            void main() {
                gl_Position = vec4(aPos, 1.0);
                vTexCoord = aPos.xy * 0.5 + 0.5;
            }""".trimIndent()

        val fsCode = """#version 300 es
            precision highp float;
            uniform sampler2D uTexture;
            uniform int uWriteRow;
            in vec2 vTexCoord;
            out vec4 fragColor;
            void main() {
                float shiftedY = mod(vTexCoord.y + float(uWriteRow) / 512.0, 1.0);
                float val = texture(uTexture, vec2(vTexCoord.x, shiftedY)).r;
                vec3 color = mix(vec3(0.0, 0.0, 0.2), vec3(0.0, 1.0, 0.8), clamp(val * 2.0, 0.0, 1.0));
                if(val > 0.5) color = mix(color, vec3(1.0, 0.0, 0.0), (val - 0.5) * 2.0);
                fragColor = vec4(color, 1.0);
            }""".trimIndent()

        val vs = loadShader(GLES30.GL_VERTEX_SHADER, vsCode)
        val fs = loadShader(GLES30.GL_FRAGMENT_SHADER, fsCode)
        
        program = GLES30.glCreateProgram().apply {
            GLES30.glAttachShader(this, vs)
            GLES30.glAttachShader(this, fs)
            GLES30.glLinkProgram(this)
        }

        val t = IntArray(1)
        GLES30.glGenTextures(1, t, 0)
        textureId = t[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R32F, texWidth, texHeight, 0, GLES30.GL_RED, GLES30.GL_FLOAT, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
    }

    fun updatePoints(magnitudes: FloatArray) {
        if (isSnapshotMode) return
        for (i in 0 until texWidth) {
            val idx = writeRow * texWidth + i
            if (idx < textureData.size) {
                textureData[idx] = (magnitudes.getOrElse(i) { 0f } / 50f) * sensitivity
            }
        }
        writeRow = (writeRow + 1) % texHeight
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(program)
        
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        val rowBuffer = FloatBuffer.wrap(textureData, writeRow * texWidth, texWidth)
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, writeRow, texWidth, 1, GLES30.GL_RED, GLES30.GL_FLOAT, rowBuffer)

        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "uWriteRow"), writeRow)
        
        val pos = GLES30.glGetAttribLocation(program, "aPos")
        GLES30.glEnableVertexAttribArray(pos)
        val vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertexData).apply { position(0) }
        GLES30.glVertexAttribPointer(pos, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) = GLES30.glViewport(0, 0, w, h)

    private fun loadShader(type: Int, source: String): Int {
        return GLES30.glCreateShader(type).also {
            GLES30.glShaderSource(it, source)
            GLES30.glCompileShader(it)
        }
    }
}
