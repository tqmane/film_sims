package com.tqmane.filmsim.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import com.tqmane.filmsim.R
import com.tqmane.filmsim.util.CubeLUT
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * GPU-accelerated high-resolution export using OpenGL FBO.
 * This class creates an offscreen rendering context for full-resolution LUT processing.
 */
class GpuExportRenderer(private val context: Context) {
    
    private var programId: Int = 0
    private var inputTextureId: Int = 0
    private var lutTextureId: Int = 0
    private var grainTextureId: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0
    
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer
    
    private var isInitialized = false
    
    init {
        // Full screen quad (no aspect ratio correction for export)
        val vertices = floatArrayOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )
        // Flip Y for correct output
        val texCoords = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )
        
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices)
        vertexBuffer.position(0)
        
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(texCoords)
        texCoordBuffer.position(0)
    }
    
    /**
     * Initialize OpenGL resources. Must be called on GL thread.
     */
    fun initialize() {
        if (isInitialized) return
        
        val vertexSource = readRawTextFile(R.raw.vertex_shader)
        val fragmentSource = readRawTextFile(R.raw.fragment_shader)
        
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        
        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)
        
        // Generate textures
        val textures = IntArray(4)
        GLES30.glGenTextures(4, textures, 0)
        inputTextureId = textures[0]
        lutTextureId = textures[1]
        grainTextureId = textures[2]
        fboTextureId = textures[3]
        
        // Generate FBO
        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        fboId = fbos[0]
        
        // Load grain texture
        loadGrainTexture()
        
        isInitialized = true
    }
    
    private fun loadGrainTexture() {
        try {
            val inputStream = context.assets.open("textures/film_grain.png")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, grainTextureId)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
                bitmap.recycle()
            }
        } catch (e: Exception) {
            // No grain texture
        }
    }
    
    /**
     * Render the image with LUT at full resolution using GPU.
     * Must be called on GL thread.
     */
    fun renderHighRes(
        inputBitmap: Bitmap,
        lut: CubeLUT?,
        intensity: Float,
        grainEnabled: Boolean,
        grainIntensity: Float,
        grainScale: Float
    ): Bitmap {
        if (!isInitialized) initialize()
        
        val width = inputBitmap.width
        val height = inputBitmap.height
        
        // Setup FBO texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTextureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        
        // Attach texture to FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, fboTextureId, 0)
        
        // Upload input texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, inputBitmap, 0)
        
        // Upload LUT if provided
        lut?.let {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTextureId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexImage3D(GLES30.GL_TEXTURE_3D, 0, GLES30.GL_RGB, 
                it.size, it.size, it.size, 0, GLES30.GL_RGB, GLES30.GL_FLOAT, it.data)
        }
        
        // Set viewport to full resolution
        GLES30.glViewport(0, 0, width, height)
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        
        // Render
        GLES30.glUseProgram(programId)
        
        val positionHandle = GLES30.glGetAttribLocation(programId, "aPosition")
        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        
        val texCoordHandle = GLES30.glGetAttribLocation(programId, "aTexCoord")
        GLES30.glEnableVertexAttribArray(texCoordHandle)
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)
        
        // No aspect ratio correction for export (scaleX = scaleY = 1)
        val scaleHandle = GLES30.glGetUniformLocation(programId, "uScale")
        GLES30.glUniform2f(scaleHandle, 1f, 1f)
        
        // Reset zoom and offset for full image export
        val zoomHandle = GLES30.glGetUniformLocation(programId, "uZoom")
        GLES30.glUniform1f(zoomHandle, 1.0f)
        
        val offsetHandle = GLES30.glGetUniformLocation(programId, "uOffset")
        GLES30.glUniform2f(offsetHandle, 0f, 0f)
        
        // Set uniforms
        GLES30.glUniform1f(GLES30.glGetUniformLocation(programId, "uIntensity"), if (lut != null) intensity else 0f)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(programId, "uGrainIntensity"), if (grainEnabled) grainIntensity else 0f)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(programId, "uGrainScale"), grainScale)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(programId, "uTime"), 0f)
        
        // Bind textures
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(programId, "uInputTexture"), 0)
        
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTextureId)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(programId, "uLutTexture"), 1)
        
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, grainTextureId)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(programId, "uGrainTexture"), 2)
        
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        
        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(texCoordHandle)
        
        // Read pixels from FBO
        val buffer = ByteBuffer.allocateDirect(width * height * 4)
        buffer.order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        buffer.rewind()
        
        // Create output bitmap
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outputBitmap.copyPixelsFromBuffer(buffer)
        
        // Unbind FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        
        // No need to flip if the texture coordinates and readout are aligned correctly
        return outputBitmap
    }
    
    fun release() {
        if (isInitialized) {
            GLES30.glDeleteTextures(4, intArrayOf(inputTextureId, lutTextureId, grainTextureId, fboTextureId), 0)
            GLES30.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            GLES30.glDeleteProgram(programId)
            isInitialized = false
        }
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)
        return shader
    }
    
    private fun readRawTextFile(resId: Int): String {
        val inputStream = context.resources.openRawResource(resId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.use { it.readText() }
    }
}
