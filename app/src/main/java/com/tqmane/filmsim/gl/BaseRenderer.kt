package com.tqmane.filmsim.gl

import android.content.Context
import android.opengl.GLES30
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Base class for OpenGL ES renderers.
 * Provides common functionality for vertex buffer initialization, shader loading, and resource reading.
 */
abstract class BaseRenderer(protected val context: Context) {

    /**
     * Initializes vertex and texture coordinate buffers for a full-screen quad.
     * @param flipY If true, flips the Y coordinate (used for export rendering)
     * @return Pair of (vertexBuffer, texCoordBuffer)
     */
    protected fun initQuadBuffers(flipY: Boolean = false): Pair<FloatBuffer, FloatBuffer> {
        val vertices = floatArrayOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )

        val texCoords = if (flipY) {
            // Flip Y for correct output (used in export rendering)
            floatArrayOf(
                0f, 0f,
                1f, 0f,
                0f, 1f,
                1f, 1f
            )
        } else {
            // Normal orientation (used in preview rendering)
            floatArrayOf(
                0f, 1f,
                1f, 1f,
                0f, 0f,
                1f, 0f
            )
        }

        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices)
        vertexBuffer.position(0)

        val texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(texCoords)
        texCoordBuffer.position(0)

        return Pair(vertexBuffer, texCoordBuffer)
    }

    /**
     * Loads and compiles a shader from source code.
     * @param type Either GL_VERTEX_SHADER or GL_FRAGMENT_SHADER
     * @param shaderCode The shader source code
     * @return The shader ID, or 0 if compilation failed
     */
    protected fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            val info = GLES30.glGetShaderInfoLog(shader)
            android.util.Log.e(getLogTag(), "Shader compile failed (type=$type): $info")
        }

        return shader
    }

    /**
     * Reads a text file from raw resources.
     * @param resId The resource ID of the raw file
     * @return The content of the file as a String
     */
    protected fun readRawTextFile(resId: Int): String {
        val inputStream = context.resources.openRawResource(resId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.use { it.readText() }
    }

    /**
     * Returns the tag to use for logging.
     * Subclasses should override this to provide their specific class name.
     */
    protected abstract fun getLogTag(): String
}
