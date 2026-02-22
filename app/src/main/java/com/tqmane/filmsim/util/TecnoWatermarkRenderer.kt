package com.tqmane.filmsim.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.Log

import kotlin.math.roundToInt

/**
 * TECNO Watermark Renderer — Accurate Template-Driven Implementation
 * 
 * Renders watermarks from parsed TecnoWatermarkTemplate configs (TranssionWM.json).
 * Supports portrait and landscape modes, with proper scaling.
 * 
 * This renderer reads the JSON configuration and renders elements according to their
 * defined positions, fonts, and sizes, ensuring proper alignment.
 */
class TecnoWatermarkRenderer(private val context: Context) {

    companion object {
        private const val TAG = "TecnoWmRenderer"
        // Reference width for the template (1080px)
        private const val TEMPLATE_REF_WIDTH = 1080f
    }

    // Cached typefaces
    private val typefaceCache = mutableMapOf<String, Typeface>()

    /**
     * Main render entry point.
     */
    fun render(
        source: Bitmap,
        template: TecnoWatermarkTemplate,
        modeName: String,
        isLandscape: Boolean,
        config: TecnoRenderConfig
    ): Bitmap {
        return try {
            val parser = TecnoWatermarkConfigParser(context)
            val modeConfig = parser.getMode(template, modeName, isLandscape)
            
            if (modeConfig != null) {
                renderFromConfig(source, modeConfig, isLandscape, config)
            } else {
                // Fallback: render basic watermark
                renderBasic(source, config)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Render failed", e)
            renderBasic(source, config)
        }
    }

    /**
     * Render watermark from parsed configuration.
     */
    private fun renderFromConfig(
        source: Bitmap,
        modeConfig: TecnoModeConfig,
        isLandscape: Boolean,
        config: TecnoRenderConfig
    ): Bitmap {
        val imgWidth = source.width
        val imgHeight = source.height

        // Calculate scale factor based on actual image width vs template reference
        val scale = imgWidth / TEMPLATE_REF_WIDTH

        // Get bar dimensions from config and scale
        val barWidth = modeConfig.barWidth
        val barHeight = (modeConfig.barHeight * scale).roundToInt()

        // Total height includes the white bar
        val totalHeight = imgHeight + barHeight

        val result = Bitmap.createBitmap(imgWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw original image
        canvas.drawBitmap(source, 0f, 0f, null)

        // Draw white bar
        val barPaint = Paint().apply {
            color = modeConfig.barColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, imgHeight.toFloat(), imgWidth.toFloat(), totalHeight.toFloat(), barPaint)

        // Draw backdrop texture if enabled
        if (modeConfig.backdropValid && modeConfig.backdrop != null) {
            drawBackdrop(canvas, modeConfig.backdrop, imgHeight, scale)
        }

        // Draw icons (pass textProfiles for RELY_ON_ELEM calculation)
        for (iconProfile in modeConfig.iconProfiles) {
            drawIcon(canvas, iconProfile, imgHeight, scale, config, modeConfig.textProfiles)
        }

        // Draw text elements with their index for proper content mapping
        for ((index, textProfile) in modeConfig.textProfiles.withIndex()) {
            drawText(canvas, textProfile, imgHeight, scale, config, index, modeConfig.brandName, modeConfig.textProfiles)
        }

        return result
    }

    /**
     * Draw backdrop texture on the right side of the bar.
     */
    private fun drawBackdrop(
        canvas: Canvas,
        backdrop: TecnoBackdropProfile,
        barTop: Int,
        scale: Float
    ) {
        val iconName = backdrop.iconFileName
        if (iconName.isEmpty()) return

        try {
            val bmp = loadTecnoImage(iconName)
            if (bmp != null) {
                val (coordX, coordY) = backdrop.iconCoordinate
                val (sizeW, sizeH) = backdrop.iconSize

                val x = coordX * scale
                val y = barTop + coordY * scale
                val w = sizeW * scale
                val h = sizeH * scale

                val dstRect = Rect(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt())
                canvas.drawBitmap(bmp, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                bmp.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing backdrop", e)
        }
    }

    /**
     * Draw an icon (e.g., yellow dot).
     * Icons can have RELY_ON_ELEM=true which means they position relative to a text element.
     * ICON_COORDINATE is the top-left coordinate of the icon in template pixels.
     */
    private fun drawIcon(
        canvas: Canvas,
        iconProfile: TecnoIconProfile,
        barTop: Int,
        scale: Float,
        config: TecnoRenderConfig,
        textProfiles: List<TecnoTextProfile>
    ) {
        val iconName = iconProfile.iconFileName
        if (iconName.isEmpty()) return

        // Calculate base position (top-left of icon in template pixels)
        var (baseX, baseY) = iconProfile.iconCoordinate
        
        // Handle RELY_ON_ELEM - position relative to a text element
        if (iconProfile.relyOnElem && iconProfile.relyProfile != null) {
            val relyIdx = iconProfile.relyProfile.relyIndex
            if (relyIdx < textProfiles.size) {
                val textProfile = textProfiles[relyIdx]
                val fontProfile = textProfile.fontProfile
                if (fontProfile != null) {
                    // Get the text element's position
                    val (textX, textY) = textProfile.textCoordinate
                    
                    // Measure the text to find where it ends
                    val typeface = getTecnoTypeface(fontProfile.fontFileName)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.typeface = typeface
                        textSize = fontProfile.fontSize * scale
                    }
                    
                    // Get content to measure
                    val content = getTextContent(textProfile, config, relyIdx, "")
                    if (!content.isNullOrEmpty()) {
                        val textWidth = paint.measureText(content)
                        
                        if (iconProfile.relyProfile.reltOnLeftX) {
                            // Position to the LEFT of the text (baseX = center of icon)
                            baseX = textX - iconProfile.iconSize.first / 2f - 5f
                        } else {
                            // Position to the RIGHT of the text (baseX = center of icon)
                            baseX = textX + textWidth / scale + iconProfile.iconSize.first / 2f + 5f
                        }
                        
                        // Align icon Y to text vertical center (not baseline)
                        // paint.ascent() is negative, paint.descent() is positive
                        val textCenterOffset = (paint.ascent() + paint.descent()) / 2f // in screen pixels
                        // Compensate for circle being shifted up within the PNG (~39.6% vs 50% center)
                        val pngCircleCompensation = iconProfile.iconSize.second * 0.10f
                        baseY = textY + textCenterOffset / scale + pngCircleCompensation
                    }
                }
            }
        }

        // Load and draw the icon image from assets (PNG or SVG rasterized)
        try {
            val bmp = loadTecnoImage(iconName)
            if (bmp != null) {
                val (sizeW, sizeH) = iconProfile.iconSize
                val w = sizeW * scale
                val h = sizeH * scale

                // ICON_COORDINATE is the center of the icon, offset by half size for top-left
                val x = baseX * scale - w / 2f
                val y = barTop + baseY * scale - h / 2f

                val dstRect = Rect(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt())
                canvas.drawBitmap(bmp, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                bmp.recycle()
            } else {
                Log.w(TAG, "Could not load icon: $iconName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing icon $iconName", e)
        }
    }

    /**
     * Draw a text element.
     * Supports RELY_ON_ELEM for positioning relative to another text element.
     */
    private fun drawText(
        canvas: Canvas,
        textProfile: TecnoTextProfile,
        barTop: Int,
        scale: Float,
        config: TecnoRenderConfig,
        textIndex: Int,
        fallbackBrandName: String
    ) {
        drawText(canvas, textProfile, barTop, scale, config, textIndex, fallbackBrandName, emptyList())
    }

    /**
     * Draw a text element with access to all text profiles for RELY_ON_ELEM resolution.
     */
    private fun drawText(
        canvas: Canvas,
        textProfile: TecnoTextProfile,
        barTop: Int,
        scale: Float,
        config: TecnoRenderConfig,
        textIndex: Int,
        fallbackBrandName: String,
        allTextProfiles: List<TecnoTextProfile>
    ) {
        val fontProfile = textProfile.fontProfile ?: return

        // Get text content based on render direction (texttype)
        val content = getTextContent(textProfile, config, textIndex, fallbackBrandName) ?: return
        if (content.isEmpty()) return

        // Create paint
        val typeface = getTecnoTypeface(fontProfile.fontFileName)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = fontProfile.fontSize * scale
            color = fontProfile.fontColor
        }

        // Calculate position
        val (coordX, coordY) = textProfile.textCoordinate
        var x = coordX * scale
        val y = barTop + coordY * scale

        // Handle RELY_ON_ELEM - position X relative to another text element
        if (textProfile.relyOnElem && textProfile.relyProfile != null && allTextProfiles.isNotEmpty()) {
            val relyIdx = textProfile.relyProfile.relyIndex
            if (relyIdx < allTextProfiles.size) {
                val reliedText = allTextProfiles[relyIdx]
                val reliedFont = reliedText.fontProfile
                if (reliedFont != null) {
                    val reliedContent = getTextContent(reliedText, config, relyIdx, fallbackBrandName)
                    if (!reliedContent.isNullOrEmpty()) {
                        val reliedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            this.typeface = getTecnoTypeface(reliedFont.fontFileName)
                            textSize = reliedFont.fontSize * scale
                        }
                        val reliedTextWidth = reliedPaint.measureText(reliedContent)
                        val (reliedX, _) = reliedText.textCoordinate

                        if (textProfile.relyProfile.reltOnLeftX) {
                            // Position at the LEFT edge of the relied-upon text
                            if (reliedText.renderDirection == 1) {
                                // Right-aligned text: left edge = coordX*scale - textWidth
                                x = reliedX * scale - reliedTextWidth + coordX * scale
                            } else {
                                x = reliedX * scale + coordX * scale
                            }
                        } else {
                            // Position at the RIGHT edge of the relied-upon text
                            if (reliedText.renderDirection == 1) {
                                x = reliedX * scale + coordX * scale
                            } else {
                                x = reliedX * scale + reliedTextWidth + coordX * scale
                            }
                        }
                    }
                }
            }
        }

        // Handle text alignment based on render direction
        // renderDirection: 0 = left-to-right, 1 = right-to-left (right aligned)
        if (textProfile.renderDirection == 1) {
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(content, x, y, paint)
        } else {
            canvas.drawText(content, x, y, paint)
        }
    }

    /**
     * Resolve text content based on render direction / text type.
     * In TECNO config:
     * - TEXT_PROFILES[0]: device name (renderDirection 0 = left aligned)
     * - TEXT_PROFILES[1]: datetime (renderDirection 1 = right aligned)
     * - TEXT_PROFILES[2]: lens info (renderDirection 0 = left aligned)
     * - TEXT_PROFILES[3]: location (renderDirection 0 = left aligned, relies on datetime)
     */
    private fun getTextContent(textProfile: TecnoTextProfile, config: TecnoRenderConfig, textIndex: Int, fallbackBrandName: String): String? {
        // Determine content based on text profile index in the array
        return when (textIndex) {
            0 -> {
                // First text: device name (left aligned)
                config.deviceName ?: fallbackBrandName.ifEmpty { "TECNO" }
            }
            1 -> {
                // Second text: datetime (right aligned)
                config.timeText
            }
            2 -> {
                // Third text: lens info (secondary text below device name)
                config.lensInfo
            }
            3 -> {
                // Fourth text: location (below datetime, relies on datetime)
                config.locationText
            }
            else -> config.deviceName
        }
    }

    /**
     * Load TECNO watermark image from assets.
     */
    private fun loadTecnoImage(imageName: String): Bitmap? {
        if (imageName.isEmpty()) return null

        // Prioritize PNG over SVG since BitmapFactory cannot decode SVG
        val baseName = imageName.substringBefore('.')
        val searchPaths = listOf(
            "watermark/TECNO/icons/$baseName.png",
            "watermark/TECNO/icons/$imageName"
        )

        for (path in searchPaths) {
            try {
                com.tqmane.filmsim.util.AssetUtil.openAsset(context, path).use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) return bmp
                }
            } catch (_: Exception) {
                // Try next path
            }
        }
        Log.w(TAG, "Could not load image: $imageName")
        return null
    }

    /**
     * Get typeface for TECNO fonts, with caching.
     */
    private fun getTecnoTypeface(fontFileName: String): Typeface {
        typefaceCache[fontFileName]?.let { return it }

        val typeface = try {
            Typeface.createFromAsset(context.assets, "watermark/TECNO/fonts/$fontFileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading font $fontFileName", e)
            Typeface.DEFAULT
        }

        typefaceCache[fontFileName] = typeface
        return typeface
    }

    /**
     * Basic fallback rendering when config parsing fails.
     */
    private fun renderBasic(source: Bitmap, config: TecnoRenderConfig): Bitmap {
        val imgWidth = source.width
        val imgHeight = source.height
        val scale = imgWidth / TEMPLATE_REF_WIDTH

        // Standard bar height
        val barHeight = (113f * scale).roundToInt()
        val totalHeight = imgHeight + barHeight

        val result = Bitmap.createBitmap(imgWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw original image
        canvas.drawBitmap(source, 0f, 0f, null)

        // Draw white bar
        val barPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, imgHeight.toFloat(), imgWidth.toFloat(), totalHeight.toFloat(), barPaint)

        // Draw device name on left
        val deviceName = config.deviceName ?: "TECNO"
        val brandTypeface = getTecnoTypeface("Transota0226-Regular.ttf")
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = brandTypeface
            textSize = 29f * scale
            color = Color.BLACK
        }
        canvas.drawText(deviceName, 39f * scale, (imgHeight + 72f * scale), brandPaint)

        // Draw datetime on right
        val timeText = config.timeText
        if (!timeText.isNullOrEmpty()) {
            val dateTypeface = getTecnoTypeface("tos_regular.ttf")
            val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = dateTypeface
                textSize = 23f * scale
                color = Color.parseColor("#020202")
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(timeText, 1041f * scale, (imgHeight + 70f * scale), datePaint)
        }

        return result
    }
}

/**
 * Configuration data for TECNO watermark rendering.
 */
data class TecnoRenderConfig(
    val deviceName: String? = null,
    val timeText: String? = null,
    val locationText: String? = null,
    val lensInfo: String? = null,
    val brandName: String = ""
)
