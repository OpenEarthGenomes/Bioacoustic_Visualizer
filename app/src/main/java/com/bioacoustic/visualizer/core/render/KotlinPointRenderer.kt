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

    private val vertexData = floatArrayOf(-1f, -1f, 0f, 1f, -1f, 0f, -1f, 1f, 0f, 1f, 1f, 0f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.0f, 0.0f, 0.05f, 1.0f)
        
        // 3D Shader - Döntött perspektíva
        val vs = """#version 300 es
            layout(location = 0) in vec3 aPos;
            out vec2 vTexCoord;
            void main() {
                vec3 pos = aPos;
                pos.z = pos.y * 0.4; 
                pos.y *= 0.7;
                gl_Position = vec4(pos, 1.1 - pos.z);
                vTexCoord = aPos.xy * 0.5 + 0.5;
            }""".trimIndent()

        val fs = """#version 300 es
            precision highp float;
            uniform sampler2D uTexture;
            uniform int uWriteRow;
            in vec2 vTexCoord;
            out vec4 fragColor;
            void main() {
                float scroll = mod(vTexCoord.y + float(uWriteRow) / 512.0, 1.0);
                float val = texture(uTexture, vec2(vTexCoord.x, scroll)).r;
                vec3 color = mix(vec3(0.0, 0.0, 0.2), vec3(0.0, 1.0, 0.8), clamp(val, 0.0, 1.0));
                if(val > 0.5) color = mix(color, vec3(1.0, 0.0, 0.0), (val - 0.5) * 2.0);
                fragColor = vec4(color * vTexCoord.y, 1.0);
            }""".trimIndent()

        program = GLES30.glCreateProgram().apply {
            val vsh = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER).also { GLES30.glShaderSource(it, vs); GLES30.glCompileShader(it) }
            val fsh = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER).also { GLES30.glShaderSource(it, fs); GLES30.glCompileShader(it) }
            GLES30.glAttachShader(this, vsh); GLES30.glAttachShader(this, fsh); GLES30.glLinkProgram(this)
        }

        val t = IntArray(1); GLES30.glGenTextures(1, t, 0); textureId = t[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R32F, texWidth, texHeight, 0, GLES30.GL_RED, GLES30.GL_FLOAT, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
    }

    fun updatePoints(magnitudes: FloatArray) {
        for (i in 0 until texWidth) {
            textureData[writeRow * texWidth + i] = (magnitudes.getOrElse(i) { 0f } / 30f) * sensitivity
        }
        writeRow = (writeRow + 1) % texHeight
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(program)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, writeRow, texWidth, 1, GLES30.GL_RED, GLES30.GL_FLOAT, FloatBuffer.wrap(textureData, writeRow * texWidth, texWidth))
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "uWriteRow"), writeRow)
        val pos = GLES30.glGetAttribLocation(program, "aPos")
        GLES30.glEnableVertexAttribArray(pos)
        val bb = ByteBuffer.allocateDirect(vertexData.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertexData).apply { position(0) }
        GLES30.glVertexAttribPointer(pos, 3, GLES30.GL_FLOAT, false, 0, bb)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) = GLES30.glViewport(0, 0, w, h)
}

