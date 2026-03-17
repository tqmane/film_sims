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
    private var bgProgramId: Int = 0
    private var inputTextureId: Int = 0
    private var lutTextureId: Int = 0
    private var overlayLutTextureId: Int = 0
    private var grainTextureId: Int = 0
    
    // Time variable for shaders
    private var startTimeMillis: Long = System.currentTimeMillis()

    private var vertexBuffer: FloatBuffer
    private var texCoordBuffer: FloatBuffer

    private var pendingBitmap: Bitmap? = null
    private var pendingLut: CubeLUT? = null
    private var pendingOverlayLut: CubeLUT? = null
    private var pendingGrainBitmap: Bitmap? = null
    
    private var viewportWidth = 0
    private var viewportHeight = 0
    
    private var imageWidth = 1
    private var imageHeight = 1
    
    // LUT intensity (0.0 = original, 1.0 = full effect)
    private var intensity: Float = 1.0f
    private var overlayIntensity: Float = 0.35f
    
    // Film grain settings
    private var grainIntensity: Float = 0.0f
    private var grainScale: Float = 4.0f
    private var grainEnabled: Boolean = false
    private var grainStylePath: String = "textures/Xiaomi/film_grain.png"
    
    // Basic adjustments
    private var exposure: Float = 0.0f
    private var contrast: Float = 0.0f
    private var highlights: Float = 0.0f
    private var shadows: Float = 0.0f
    private var colorTemp: Float = 0.0f
    private var hue: Float = 0.0f
    private var saturation: Float = 0.0f
    private var luminance: Float = 0.0f

    // Before/after compare preview
    private var compareEnabled: Boolean = false
    private var compareSplit: Float = 0.5f
    private var compareVertical: Boolean = true
    
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
    private var uOverlayIntensityHandle: Int = -1
    private var uGrainIntensityHandle: Int = -1
    private var uGrainScaleHandle: Int = -1
    private var uTimeHandle: Int = -1
    private var uInputTextureHandle: Int = -1
    private var uLutTextureHandle: Int = -1
    private var uOverlayLutTextureHandle: Int = -1
    private var uGrainTextureHandle: Int = -1

    // Adjustment uniform handles
    private var uExposureHandle: Int = -1
    private var uContrastHandle: Int = -1
    private var uHighlightsHandle: Int = -1
    private var uShadowsHandle: Int = -1
    private var uColorTempHandle: Int = -1
    private var uHueHandle: Int = -1
    private var uSaturationHandle: Int = -1
    private var uLuminanceHandle: Int = -1
    private var uCompareSplitHandle: Int = -1
    private var uCompareEnabledHandle: Int = -1
    private var uCompareVerticalHandle: Int = -1
    private var uResolutionHandle: Int = -1

    // Background shader attribute/uniform locations
    private var bgPositionHandle: Int = -1
    private var bgTexCoordHandle: Int = -1
    private var bgTimeHandle: Int = -1
    private var bgResolutionHandle: Int = -1

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

    fun setOverlayLut(lut: CubeLUT) {
        pendingOverlayLut = lut
    }
    
    fun setIntensity(value: Float) {
        intensity = value.coerceIn(0f, 1f)
    }

    fun setOverlayIntensity(value: Float) {
        overlayIntensity = value.coerceIn(0f, 1f)
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
                val inputStream = com.tqmane.filmsim.util.AssetUtil.openAsset(context, grainStylePath)
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
    
    // Basic adjustment setters
    fun setExposure(value: Float) { exposure = value.coerceIn(-2f, 2f) }
    fun setContrast(value: Float) { contrast = value.coerceIn(-1f, 1f) }
    fun setHighlights(value: Float) { highlights = value.coerceIn(-1f, 1f) }
    fun setShadows(value: Float) { shadows = value.coerceIn(-1f, 1f) }
    fun setColorTemp(value: Float) { colorTemp = value.coerceIn(-1f, 1f) }
    fun setHue(value: Float) { hue = value.coerceIn(-1f, 1f) }
    fun setSaturation(value: Float) { saturation = value.coerceIn(-1f, 1f) }
    fun setLuminance(value: Float) { luminance = value.coerceIn(-1f, 1f) }
    fun setCompareEnabled(enabled: Boolean) { compareEnabled = enabled }
    fun setCompareSplit(value: Float) { compareSplit = value.coerceIn(0f, 1f) }
    fun setCompareVertical(vertical: Boolean) { compareVertical = vertical }

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

        // Compile background shader
        val bgVertexSource = readRawTextFile(R.raw.bg_vertex_shader)
        val bgFragmentSource = readRawTextFile(R.raw.bg_fragment_shader)
        
        val bgVertexShader = loadShader(GLES30.GL_VERTEX_SHADER, bgVertexSource)
        val bgFragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, bgFragmentSource)
        
        bgProgramId = GLES30.glCreateProgram()
        GLES30.glAttachShader(bgProgramId, bgVertexShader)
        GLES30.glAttachShader(bgProgramId, bgFragmentShader)
        GLES30.glLinkProgram(bgProgramId)

        val bgLinkStatus = IntArray(1)
        GLES30.glGetProgramiv(bgProgramId, GLES30.GL_LINK_STATUS, bgLinkStatus, 0)
        if (bgLinkStatus[0] == 0) {
            val info = GLES30.glGetProgramInfoLog(bgProgramId)
            android.util.Log.e("FilmSimRenderer", "BG Program link failed: $info")
        }

        GLES30.glDeleteShader(bgVertexShader)
        GLES30.glDeleteShader(bgFragmentShader)

        // Cache locations
        aPositionHandle = GLES30.glGetAttribLocation(programId, "aPosition")
        aTexCoordHandle = GLES30.glGetAttribLocation(programId, "aTexCoord")
        uScaleHandle = GLES30.glGetUniformLocation(programId, "uScale")
        uZoomHandle = GLES30.glGetUniformLocation(programId, "uZoom")
        uOffsetHandle = GLES30.glGetUniformLocation(programId, "uOffset")
        uIntensityHandle = GLES30.glGetUniformLocation(programId, "uIntensity")
        uOverlayIntensityHandle = GLES30.glGetUniformLocation(programId, "uOverlayIntensity")
        uGrainIntensityHandle = GLES30.glGetUniformLocation(programId, "uGrainIntensity")
        uGrainScaleHandle = GLES30.glGetUniformLocation(programId, "uGrainScale")
        uTimeHandle = GLES30.glGetUniformLocation(programId, "uTime")
        uInputTextureHandle = GLES30.glGetUniformLocation(programId, "uInputTexture")
        uLutTextureHandle = GLES30.glGetUniformLocation(programId, "uLutTexture")
        uOverlayLutTextureHandle = GLES30.glGetUniformLocation(programId, "uOverlayLutTexture")
        uGrainTextureHandle = GLES30.glGetUniformLocation(programId, "uGrainTexture")
        uExposureHandle = GLES30.glGetUniformLocation(programId, "uExposure")
        uContrastHandle = GLES30.glGetUniformLocation(programId, "uContrast")
        uHighlightsHandle = GLES30.glGetUniformLocation(programId, "uHighlights")
        uShadowsHandle = GLES30.glGetUniformLocation(programId, "uShadows")
        uColorTempHandle = GLES30.glGetUniformLocation(programId, "uColorTemp")
        uHueHandle = GLES30.glGetUniformLocation(programId, "uHue")
        uSaturationHandle = GLES30.glGetUniformLocation(programId, "uSaturation")
        uLuminanceHandle = GLES30.glGetUniformLocation(programId, "uLuminance")
        uCompareSplitHandle = GLES30.glGetUniformLocation(programId, "uCompareSplit")
        uCompareEnabledHandle = GLES30.glGetUniformLocation(programId, "uCompareEnabled")
        uCompareVerticalHandle = GLES30.glGetUniformLocation(programId, "uCompareVertical")
        uResolutionHandle = GLES30.glGetUniformLocation(programId, "uResolution")
        
        bgPositionHandle = GLES30.glGetAttribLocation(bgProgramId, "aPosition")
        bgTexCoordHandle = GLES30.glGetAttribLocation(bgProgramId, "aTexCoord")
        bgTimeHandle = GLES30.glGetUniformLocation(bgProgramId, "uTime")
        bgResolutionHandle = GLES30.glGetUniformLocation(bgProgramId, "uResolution")

        // Bind sampler uniforms once
        GLES30.glUseProgram(programId)
        if (uInputTextureHandle >= 0) GLES30.glUniform1i(uInputTextureHandle, 0)
        if (uLutTextureHandle >= 0) GLES30.glUniform1i(uLutTextureHandle, 1)
        if (uOverlayLutTextureHandle >= 0) GLES30.glUniform1i(uOverlayLutTextureHandle, 2)
        if (uGrainTextureHandle >= 0) GLES30.glUniform1i(uGrainTextureHandle, 3)
        
        val textures = IntArray(4)
        GLES30.glGenTextures(4, textures, 0)
        inputTextureId = textures[0]
        lutTextureId = textures[1]
        overlayLutTextureId = textures[2]
        grainTextureId = textures[3]
        
        // Try to load default grain texture from assets
        loadDefaultGrainTexture()
    }
    
    private fun loadDefaultGrainTexture() {
        try {
            val inputStream = com.tqmane.filmsim.util.AssetUtil.openAsset(context, grainStylePath)
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
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        
        val currentTime = (System.currentTimeMillis() - startTimeMillis) / 1000f

        // Draw background pass
        GLES30.glUseProgram(bgProgramId)
        
        GLES30.glEnableVertexAttribArray(bgPositionHandle)
        GLES30.glVertexAttribPointer(bgPositionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        
        GLES30.glEnableVertexAttribArray(bgTexCoordHandle)
        GLES30.glVertexAttribPointer(bgTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)
        
        GLES30.glUniform1f(bgTimeHandle, currentTime * 1000f)
        GLES30.glUniform2f(bgResolutionHandle, viewportWidth.toFloat(), viewportHeight.toFloat())

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        
        GLES30.glDisableVertexAttribArray(bgPositionHandle)
        GLES30.glDisableVertexAttribArray(bgTexCoordHandle)
        
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

        pendingOverlayLut?.let { overlayLut ->
            GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, overlayLutTextureId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)

            overlayLut.data.position(0)
            GLES30.glTexImage3D(
                GLES30.GL_TEXTURE_3D,
                0,
                GLES30.GL_RGB16F,
                overlayLut.size,
                overlayLut.size,
                overlayLut.size,
                0,
                GLES30.GL_RGB,
                GLES30.GL_FLOAT,
                overlayLut.data
            )

            pendingOverlayLut = null
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
            GLES30.glUniform1f(uOverlayIntensityHandle, overlayIntensity)
            GLES30.glUniform1f(uGrainIntensityHandle, if (grainEnabled) grainIntensity else 0f)
            GLES30.glUniform1f(uGrainScaleHandle, grainScale)
            GLES30.glUniform1f(uTimeHandle, currentTime)
            GLES30.glUniform1f(uExposureHandle, exposure)
            GLES30.glUniform1f(uContrastHandle, contrast)
            GLES30.glUniform1f(uHighlightsHandle, highlights)
            GLES30.glUniform1f(uShadowsHandle, shadows)
            GLES30.glUniform1f(uColorTempHandle, colorTemp)
            GLES30.glUniform1f(uHueHandle, hue)
            GLES30.glUniform1f(uSaturationHandle, saturation)
            GLES30.glUniform1f(uLuminanceHandle, luminance)
            GLES30.glUniform1f(uCompareSplitHandle, compareSplit)
            GLES30.glUniform1f(uCompareEnabledHandle, if (compareEnabled) 1f else 0f)
            GLES30.glUniform1f(uCompareVerticalHandle, if (compareVertical) 1f else 0f)
            GLES30.glUniform2f(uResolutionHandle, viewportWidth.toFloat(), viewportHeight.toFloat())

            // Bind textures
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
            
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, lutTextureId)
            
            GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_3D, overlayLutTextureId)

            GLES30.glActiveTexture(GLES30.GL_TEXTURE3)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, grainTextureId)
            
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            
            GLES30.glDisableVertexAttribArray(aPositionHandle)
            GLES30.glDisableVertexAttribArray(aTexCoordHandle)
        }
    }

    override fun getLogTag(): String = "FilmSimRenderer"
}
