package com.tqmane.filmsim.gl

import android.content.Context
import java.nio.FloatBuffer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.tqmane.filmsim.R
import com.tqmane.filmsim.util.CubeLUT
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FilmSimRenderer(context: Context) : BaseRenderer(context), GLSurfaceView.Renderer {

    private var programId: Int = 0
    private var inputTextureId: Int = 0
    private var lutTextureId: Int = 0
    private var grainTextureId: Int = 0
    
    private var vertexBuffer: FloatBuffer
    private var texCoordBuffer: FloatBuffer

    private var pendingBitmap: Bitmap? = null
    private var pendingLut: CubeLUT? = null
    private var pendingGrainBitmap: Bitmap? = null
    
    private var viewportWidth = 0
    private var viewportHeight = 0
    
    private var imageWidth = 1
    private var imageHeight = 1
    
    // LUT intensity (0.0 = original, 1.0 = full effect)
    private var intensity: Float = 1.0f
    
    // Film grain settings
    private var grainIntensity: Float = 0.0f
    private var grainScale: Float = 4.0f
    private var grainEnabled: Boolean = false
    private var grainStylePath: String = "textures/Xiaomi/film_grain.png"
    
    // Transform parameters
    private var userZoom: Float = 1.0f
    private var userOffsetX: Float = 0.0f
    private var userOffsetY: Float = 0.0f
    
    private var hasImage: Boolean = false

    // Cached attribute/uniform locations (glGet* in onDrawFrame is expensive)
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

    init {
        val buffers = initQuadBuffers(flipY = false)
        vertexBuffer = buffers.first
        texCoordBuffer = buffers.second
    }

    fun setImage(bitmap: Bitmap) {
        pendingBitmap = bitmap
        hasImage = true
    }

    fun setLut(lut: CubeLUT) {
        pendingLut = lut
    }
    
    fun setIntensity(value: Float) {
        intensity = value.coerceIn(0f, 1f)
    }
    
    fun getIntensity(): Float = intensity
    
    fun hasLoadedImage(): Boolean = hasImage
    
    // Film grain methods
    fun setGrainEnabled(enabled: Boolean) {
        grainEnabled = enabled
    }
    
    fun setGrainIntensity(value: Float) {
        grainIntensity = value.coerceIn(0f, 1f)
    }
    
    fun setGrainScale(value: Float) {
        grainScale = value.coerceIn(1f, 10f)
    }
    
    fun updateTransform(zoom: Float, offsetX: Float, offsetY: Float) {
        userZoom = zoom
        userOffsetX = offsetX
        userOffsetY = offsetY
    }
    
    fun loadGrainTexture(bitmap: Bitmap) {
        pendingGrainBitmap = bitmap
    }
    
    fun setGrainStyle(style: String) {
        val path = when (style) {
            "OnePlus" -> "textures/OnePlus/film_grain.png"
            else -> "textures/Xiaomi/film_grain.png"
        }
        if (path != grainStylePath) {
            grainStylePath = path
            try {
                val inputStream = context.assets.open(grainStylePath)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (bitmap != null) {
                    pendingGrainBitmap = bitmap
                }
            } catch (e: Exception) {
                // Fallback to Xiaomi
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
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
            android.util.Log.e("FilmSimRenderer", "Program link failed: $info")
        }

        // We can delete shaders after linking
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
        
        // Generate textures (3 now: input, LUT, grain)
        val textures = IntArray(3)
        GLES30.glGenTextures(3, textures, 0)
        inputTextureId = textures[0]
        lutTextureId = textures[1]
        grainTextureId = textures[2]
        
        // Try to load default grain texture from assets
        loadDefaultGrainTexture()
    }
    
    private fun loadDefaultGrainTexture() {
        try {
            val inputStream = context.assets.open(grainStylePath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                pendingGrainBitmap = bitmap
            }
        } catch (e: Exception) {
            // No default grain texture, that's ok
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClearColor(0.05f, 0.05f, 0.05f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        
        // Update input texture if needed
        pendingBitmap?.let { bmp ->
            imageWidth = bmp.width
            imageHeight = bmp.height
            
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
            pendingBitmap = null
        }
        
        // Update LUT texture if needed
        pendingLut?.let { lut ->
            GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTextureId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)
            
            // Reset buffer position — it may have been consumed by a previous upload
            lut.data.position(0)
            // Use GL_RGB16F for MediaTek/Mali GPU compatibility
            // Mali requires sized internal format for float texture linear filtering
            GLES30.glTexImage3D(GLES30.GL_TEXTURE_3D, 0, GLES30.GL_RGB16F, 
                lut.size, lut.size, lut.size, 0, 
                GLES30.GL_RGB, GLES30.GL_FLOAT, lut.data)
                
            pendingLut = null
        }
        
        // Update grain texture if needed
        pendingGrainBitmap?.let { bmp ->
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, grainTextureId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
            pendingGrainBitmap = null
        }

        if (inputTextureId != 0 && hasImage) {
            GLES30.glUseProgram(programId)

            GLES30.glEnableVertexAttribArray(aPositionHandle)
            GLES30.glVertexAttribPointer(aPositionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)

            GLES30.glEnableVertexAttribArray(aTexCoordHandle)
            GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)
            
            // Calculate Aspect Ratio Scale
            val imgRatio = imageWidth.toFloat() / imageHeight.toFloat()
            val viewRatio = viewportWidth.toFloat() / viewportHeight.toFloat()
            
            var scaleX = 1f
            var scaleY = 1f
            
            if (imgRatio > viewRatio) {
                scaleY = viewRatio / imgRatio
            } else {
                scaleX = imgRatio / viewRatio
            }
            
            GLES30.glUniform2f(uScaleHandle, scaleX, scaleY)
            
            // Set user transform (zoom & pan)
            GLES30.glUniform1f(uZoomHandle, userZoom)
            GLES30.glUniform2f(uOffsetHandle, userOffsetX, userOffsetY)
            GLES30.glUniform1f(uIntensityHandle, intensity)
            GLES30.glUniform1f(uGrainIntensityHandle, if (grainEnabled) grainIntensity else 0f)
            GLES30.glUniform1f(uGrainScaleHandle, grainScale)
            GLES30.glUniform1f(uTimeHandle, 0f) // Static grain for now

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
    }

    override fun getLogTag(): String = "FilmSimRenderer"
}
