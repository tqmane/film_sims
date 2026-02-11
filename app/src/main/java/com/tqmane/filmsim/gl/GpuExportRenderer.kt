package com.tqmane.filmsim.gl

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import com.tqmane.filmsim.R
import com.tqmane.filmsim.util.CubeLUT

/**
 * GPU-accelerated high-resolution export using OpenGL FBO.
 * This class creates an offscreen rendering context for full-resolution LUT processing.
 * Supports tiled rendering for images exceeding GL_MAX_TEXTURE_SIZE.
 */
class GpuExportRenderer(context: Context) : BaseRenderer(context) {
    
    private var programId: Int = 0
    private var inputTextureId: Int = 0
    private var lutTextureId: Int = 0
    private var grainTextureId: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0
    
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer
    
    private var isInitialized = false

    // Cached handles
    private var aPositionHandle: Int = -1
    private var aTexCoordHandle: Int = -1
    private var uScaleHandle: Int = -1
    private var uZoomHandle: Int = -1
    private var uOffsetHandle: Int = -1
    private var uIntensityHandle: Int = -1
    private var uGrainIntensityHandle: Int = -1
    private var uGrainScaleHandle: Int = -1
    private var uTimeHandle: Int = -1
    private var uInputTextureHandle: Int = -1
    private var uLutTextureHandle: Int = -1
    private var uGrainTextureHandle: Int = -1

    // Skip re-uploading the same LUT when exporting repeatedly
    private var lastUploadedLut: CubeLUT? = null
    
    // Grain style path
    private var grainStylePath: String = "textures/Xiaomi/film_grain.png"
    
    init {
        val buffers = initQuadBuffers(flipY = true)
        vertexBuffer = buffers.first
        texCoordBuffer = buffers.second
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

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val info = GLES30.glGetProgramInfoLog(programId)
            android.util.Log.e("GpuExportRenderer", "Program link failed: $info")
        }

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)

        // Cache locations
        aPositionHandle = GLES30.glGetAttribLocation(programId, "aPosition")
        aTexCoordHandle = GLES30.glGetAttribLocation(programId, "aTexCoord")
        uScaleHandle = GLES30.glGetUniformLocation(programId, "uScale")
        uZoomHandle = GLES30.glGetUniformLocation(programId, "uZoom")
        uOffsetHandle = GLES30.glGetUniformLocation(programId, "uOffset")
        uIntensityHandle = GLES30.glGetUniformLocation(programId, "uIntensity")
        uGrainIntensityHandle = GLES30.glGetUniformLocation(programId, "uGrainIntensity")
        uGrainScaleHandle = GLES30.glGetUniformLocation(programId, "uGrainScale")
        uTimeHandle = GLES30.glGetUniformLocation(programId, "uTime")
        uInputTextureHandle = GLES30.glGetUniformLocation(programId, "uInputTexture")
        uLutTextureHandle = GLES30.glGetUniformLocation(programId, "uLutTexture")
        uGrainTextureHandle = GLES30.glGetUniformLocation(programId, "uGrainTexture")

        // Bind sampler uniforms once
        GLES30.glUseProgram(programId)
        if (uInputTextureHandle >= 0) GLES30.glUniform1i(uInputTextureHandle, 0)
        if (uLutTextureHandle >= 0) GLES30.glUniform1i(uLutTextureHandle, 1)
        if (uGrainTextureHandle >= 0) GLES30.glUniform1i(uGrainTextureHandle, 2)
        
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
            val inputStream = context.assets.open(grainStylePath)
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
    
    fun setGrainStyle(style: String) {
        val path = when (style) {
            "OnePlus" -> "textures/OnePlus/film_grain.png"
            else -> "textures/Xiaomi/film_grain.png"
        }
        if (path != grainStylePath) {
            grainStylePath = path
            if (isInitialized) {
                loadGrainTexture()
            }
        }
    }
    
    /**
     * Render the image with LUT at full resolution using GPU.
     * Must be called on GL thread.
     * Supports tiled rendering for images exceeding GL_MAX_TEXTURE_SIZE.
     * Returns null only if critical GPU error occurs.
     */
    fun renderHighRes(
        inputBitmap: Bitmap,
        lut: CubeLUT?,
        intensity: Float,
        grainEnabled: Boolean,
        grainIntensity: Float,
        grainScale: Float
    ): Bitmap? {
        if (!isInitialized) initialize()
        
        val width = inputBitmap.width
        val height = inputBitmap.height
        
        // Check GPU texture size limit
        val maxSize = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, maxSize, 0)
        val maxTextureSize = maxSize[0]
        
        android.util.Log.d("GpuExportRenderer", "Image size: ${width}x${height}, GPU max texture size: $maxTextureSize")
        
        // Upload LUT if provided (once for all tiles)
        lut?.let {
            if (it !== lastUploadedLut) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTextureId)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)
                // Use GL_RGB16F for MediaTek/Mali GPU compatibility
                it.data.position(0)
                GLES30.glTexImage3D(
                    GLES30.GL_TEXTURE_3D,
                    0,
                    GLES30.GL_RGB16F,
                    it.size,
                    it.size,
                    it.size,
                    0,
                    GLES30.GL_RGB,
                    GLES30.GL_FLOAT,
                    it.data
                )
                lastUploadedLut = it
            }
        } ?: run {
            lastUploadedLut = null
        }
        
        // Determine if tiling is needed
        val needsTiling = width > maxTextureSize || height > maxTextureSize
        
        return if (needsTiling) {
            android.util.Log.d("GpuExportRenderer", "Image exceeds GPU limit, using tiled rendering")
            renderTiled(inputBitmap, lut, intensity, grainEnabled, grainIntensity, grainScale, maxTextureSize)
        } else {
            renderSingle(inputBitmap, lut, intensity, grainEnabled, grainIntensity, grainScale)
        }
    }
    
    /**
     * Render a single image that fits within texture limits.
     */
    private fun renderSingle(
        inputBitmap: Bitmap,
        lut: CubeLUT?,
        intensity: Float,
        grainEnabled: Boolean,
        grainIntensity: Float,
        grainScale: Float
    ): Bitmap? {
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
        
        // Check FBO completeness
        val fboStatus = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (fboStatus != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            android.util.Log.e("GpuExportRenderer", "FBO incomplete: status=$fboStatus. Falling back to CPU.")
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            return null
        }
        
        // Upload input texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, inputBitmap, 0)
        
        // Set viewport to full resolution
        GLES30.glViewport(0, 0, width, height)
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        
        // Render with default tex coords (full image)
        renderQuad(lut, intensity, grainEnabled, grainIntensity, grainScale)
        
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
        
        android.util.Log.d("GpuExportRenderer", "GPU export successful: ${width}x${height}")
        return outputBitmap
    }
    
    /**
     * Render a large image using tiled approach.
     * Splits the image into tiles that fit within GPU texture limits.
     */
    private fun renderTiled(
        inputBitmap: Bitmap,
        lut: CubeLUT?,
        intensity: Float,
        grainEnabled: Boolean,
        grainIntensity: Float,
        grainScale: Float,
        maxTextureSize: Int
    ): Bitmap? {
        val width = inputBitmap.width
        val height = inputBitmap.height
        
        // Create output bitmap
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Calculate tile dimensions
        val tileSize = maxTextureSize
        val tilesX = (width + tileSize - 1) / tileSize
        val tilesY = (height + tileSize - 1) / tileSize
        
        android.util.Log.d("GpuExportRenderer", "Tiled rendering: ${tilesX}x${tilesY} tiles (each up to ${tileSize}x${tileSize})")
        
        // Setup FBO texture (reuse for each tile)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTextureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        
        // Attach to FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, fboTextureId, 0)
        
        for (ty in 0 until tilesY) {
            for (tx in 0 until tilesX) {
                val tileX = tx * tileSize
                val tileY = ty * tileSize
                val tileW = minOf(tileSize, width - tileX)
                val tileH = minOf(tileSize, height - tileY)
                
                // Extract tile from input bitmap
                val tileBitmap = Bitmap.createBitmap(inputBitmap, tileX, tileY, tileW, tileH)
                
                // Resize FBO texture for this tile
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTextureId)
                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, tileW, tileH, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
                
                // Check FBO completeness
                val fboStatus = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
                if (fboStatus != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                    android.util.Log.e("GpuExportRenderer", "FBO incomplete for tile ($tx, $ty): status=$fboStatus")
                    tileBitmap.recycle()
                    outputBitmap.recycle()
                    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                    return null
                }
                
                // Upload tile as input texture
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, tileBitmap, 0)
                
                // Set viewport for this tile
                GLES30.glViewport(0, 0, tileW, tileH)
                GLES30.glClearColor(0f, 0f, 0f, 1f)
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                
                // Render the tile (full texture coords since tile is already cropped)
                renderQuad(lut, intensity, grainEnabled, grainIntensity, grainScale)
                
                // Read tile pixels
                val tileBuffer = ByteBuffer.allocateDirect(tileW * tileH * 4)
                tileBuffer.order(ByteOrder.nativeOrder())
                GLES30.glReadPixels(0, 0, tileW, tileH, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, tileBuffer)
                tileBuffer.rewind()
                
                // Create tile output bitmap and copy to result
                val tileOutput = Bitmap.createBitmap(tileW, tileH, Bitmap.Config.ARGB_8888)
                tileOutput.copyPixelsFromBuffer(tileBuffer)
                
                // Copy tile to output bitmap
                val pixels = IntArray(tileW * tileH)
                tileOutput.getPixels(pixels, 0, tileW, 0, 0, tileW, tileH)
                outputBitmap.setPixels(pixels, 0, tileW, tileX, tileY, tileW, tileH)
                
                // Cleanup tile bitmaps
                tileBitmap.recycle()
                tileOutput.recycle()
                
                android.util.Log.d("GpuExportRenderer", "Tile ($tx, $ty) rendered: ${tileW}x${tileH} at ($tileX, $tileY)")
            }
        }
        
        // Unbind FBO
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        
        android.util.Log.d("GpuExportRenderer", "Tiled GPU export successful: ${width}x${height}")
        return outputBitmap
    }
    
    /**
     * Render a quad with the shader program.
     */
    private fun renderQuad(
        lut: CubeLUT?,
        intensity: Float,
        grainEnabled: Boolean,
        grainIntensity: Float,
        grainScale: Float
    ) {
        GLES30.glUseProgram(programId)

        GLES30.glEnableVertexAttribArray(aPositionHandle)
        GLES30.glVertexAttribPointer(aPositionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)

        GLES30.glEnableVertexAttribArray(aTexCoordHandle)
        GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)
        
        // No aspect ratio correction for export (scaleX = scaleY = 1)
        GLES30.glUniform2f(uScaleHandle, 1f, 1f)
        
        // Reset zoom and offset for full image export
        GLES30.glUniform1f(uZoomHandle, 1.0f)
        GLES30.glUniform2f(uOffsetHandle, 0f, 0f)
        
        // Set uniforms
        GLES30.glUniform1f(uIntensityHandle, if (lut != null) intensity else 0f)
        GLES30.glUniform1f(uGrainIntensityHandle, if (grainEnabled) grainIntensity else 0f)
        GLES30.glUniform1f(uGrainScaleHandle, grainScale)
        GLES30.glUniform1f(uTimeHandle, 0f)
        
        // Bind textures
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
        
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTextureId)
        
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, grainTextureId)
        
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        
        GLES30.glDisableVertexAttribArray(aPositionHandle)
        GLES30.glDisableVertexAttribArray(aTexCoordHandle)
    }
    
    fun release() {
        if (isInitialized) {
            GLES30.glDeleteTextures(4, intArrayOf(inputTextureId, lutTextureId, grainTextureId, fboTextureId), 0)
            GLES30.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            GLES30.glDeleteProgram(programId)
            isInitialized = false
            lastUploadedLut = null
        }
    }

    override fun getLogTag(): String = "GpuExportRenderer"
}
