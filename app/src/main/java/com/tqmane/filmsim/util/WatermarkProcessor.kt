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
import com.tqmane.filmsim.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Applies Honor-style watermarks to exported images.
 *
 * Faithfully reproduces the Honor watermark templates (FrameWatermark & TextWatermark)
 * using the exact font (HONORSansVFCN.ttf), dimensions, and layout from content.json.
 *
 * All dimensions are scaled proportionally based on a 6144px reference width
 * (matching the original Honor template `baseOnValue`).
 */
object WatermarkProcessor {

    private const val PHI = 1.61803398875f

    private fun rint(v: Float): Int = v.roundToInt()

    private fun rectF(l: Float, t: Float, r: Float, b: Float): Rect =
        Rect(rint(l), rint(t), rint(r), rint(b))

    enum class WatermarkStyle {
        NONE, FRAME, TEXT, FRAME_YG, TEXT_YG,
        MEIZU_NORM, MEIZU_PRO,
        MEIZU_Z1, MEIZU_Z2, MEIZU_Z3, MEIZU_Z4, MEIZU_Z5, MEIZU_Z6, MEIZU_Z7,
        VIVO_ZEISS, VIVO_CLASSIC, VIVO_PRO, VIVO_IQOO,
        VIVO_ZEISS_V1, VIVO_ZEISS_SONNAR, VIVO_ZEISS_HUMANITY,
        VIVO_IQOO_V1, VIVO_IQOO_HUMANITY,
        VIVO_ZEISS_FRAME, VIVO_ZEISS_OVERLAY, VIVO_ZEISS_CENTER,
        VIVO_FRAME, VIVO_FRAME_TIME,
        VIVO_IQOO_FRAME, VIVO_IQOO_FRAME_TIME,
        VIVO_OS, VIVO_OS_CORNER, VIVO_OS_SIMPLE,
        VIVO_EVENT,
        // TECNO watermarks
        TECNO_1, TECNO_2, TECNO_3, TECNO_4,
        // Config-driven watermarks (new accurate implementation)
        VIVO_ZEISS_0, VIVO_ZEISS_1, VIVO_ZEISS_2, VIVO_ZEISS_3, VIVO_ZEISS_4,
        VIVO_ZEISS_5, VIVO_ZEISS_6, VIVO_ZEISS_7, VIVO_ZEISS_8,
        VIVO_IQOO_4, VIVO_COMMON_IQOO4,
        VIVO_1, VIVO_2, VIVO_3, VIVO_4, VIVO_5
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.NONE,
        val deviceName: String? = null,   // e.g. "HONOR Magic6 Pro"
        val timeText: String? = null,
        val locationText: String? = null,
        val lensInfo: String? = null,      // e.g. "27mm  f/1.9  1/100s  ISO1600"
        val templatePath: String? = null   // Custom template path for config-driven watermarks
    )

    // Reference width from Honor template baseOnValue
    private const val BASE_WIDTH = 6144f
    // Frame border height at reference width (from backgroundElements)
    private const val FRAME_BORDER_HEIGHT = 688f

    // Cached typeface
    private var honorTypeface: Typeface? = null

    /**
     * Load the HONORSansVFCN.ttf from assets, with caching.
     */
    private fun getHonorTypeface(context: Context): Typeface {
        honorTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, context.getString(R.string.honor_font_path)).also {
                honorTypeface = it
            }
        } catch (e: Exception) {
            Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    /**
     * Create a Paint with the Honor font at a specific weight.
     * HONORSansVFCN.ttf is a variable font; weight 300 = Light, weight 400 = Regular.
     */
    private fun createHonorPaint(context: Context, weight: Int): Paint {
        val baseTypeface = getHonorTypeface(context)
        val typeface = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            Typeface.create(baseTypeface, weight, false)
        } else {
            baseTypeface
        }
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
        }
    }

    /**
     * Apply watermark to the given bitmap. Returns a new bitmap with watermark applied.
     * The source bitmap is NOT modified or recycled.
     */
    fun applyWatermark(
        context: Context,
        source: Bitmap,
        config: WatermarkConfig
    ): Bitmap {
        return when (config.style) {
            WatermarkStyle.NONE -> source
            WatermarkStyle.FRAME -> applyFrameWatermark(context, source, config)
            WatermarkStyle.TEXT -> applyTextWatermark(context, source, config)
            WatermarkStyle.FRAME_YG -> applyFrameWatermarkYG(context, source, config)
            WatermarkStyle.TEXT_YG -> applyTextWatermarkYG(context, source, config)
            WatermarkStyle.MEIZU_NORM -> applyMeizuNorm(context, source, config)
            WatermarkStyle.MEIZU_PRO -> applyMeizuPro(context, source, config)
            WatermarkStyle.MEIZU_Z1 -> applyMeizuZ1(context, source, config)
            WatermarkStyle.MEIZU_Z2 -> applyMeizuZ2(context, source, config)
            WatermarkStyle.MEIZU_Z3 -> applyMeizuZ3(context, source, config)
            WatermarkStyle.MEIZU_Z4 -> applyMeizuZ4(context, source, config)
            WatermarkStyle.MEIZU_Z5 -> applyMeizuZ5(context, source, config)
            WatermarkStyle.MEIZU_Z6 -> applyMeizuZ6(context, source, config)
            WatermarkStyle.MEIZU_Z7 -> applyMeizuZ7(context, source, config)
            WatermarkStyle.VIVO_ZEISS -> applyVivoZeiss(context, source, config)
            WatermarkStyle.VIVO_CLASSIC -> applyVivoClassic(context, source, config)
            WatermarkStyle.VIVO_PRO -> applyVivoPro(context, source, config)
            WatermarkStyle.VIVO_IQOO -> applyVivoIqoo(context, source, config)
            WatermarkStyle.VIVO_ZEISS_V1 -> applyVivoZeissV1(context, source, config)
            WatermarkStyle.VIVO_ZEISS_SONNAR -> applyVivoZeissSonnar(context, source, config)
            WatermarkStyle.VIVO_ZEISS_HUMANITY -> applyVivoZeissHumanity(context, source, config)
            WatermarkStyle.VIVO_IQOO_V1 -> applyVivoIqooV1(context, source, config)
            WatermarkStyle.VIVO_IQOO_HUMANITY -> applyVivoIqooHumanity(context, source, config)
            WatermarkStyle.VIVO_ZEISS_FRAME -> applyVivoZeissFrameWm(context, source, config)
            WatermarkStyle.VIVO_ZEISS_OVERLAY -> applyVivoZeissOverlay(context, source, config)
            WatermarkStyle.VIVO_ZEISS_CENTER -> applyVivoZeissCenter(context, source, config)
            WatermarkStyle.VIVO_FRAME -> applyVivoFrameWm(context, source, config)
            WatermarkStyle.VIVO_FRAME_TIME -> applyVivoFrameTime(context, source, config)
            WatermarkStyle.VIVO_IQOO_FRAME -> applyVivoIqooFrameWm(context, source, config)
            WatermarkStyle.VIVO_IQOO_FRAME_TIME -> applyVivoIqooFrameTime(context, source, config)
            WatermarkStyle.VIVO_OS -> applyVivoOS(context, source, config)
            WatermarkStyle.VIVO_OS_CORNER -> applyVivoOSCorner(context, source, config)
            WatermarkStyle.VIVO_OS_SIMPLE -> applyVivoOSSimple(context, source, config)
            WatermarkStyle.VIVO_EVENT -> applyVivoEvent(context, source, config)
            // TECNO watermarks
            WatermarkStyle.TECNO_1 -> applyTecnoConfigDrivenWatermark(context, source, config, 1)
            WatermarkStyle.TECNO_2 -> applyTecnoConfigDrivenWatermark(context, source, config, 2)
            WatermarkStyle.TECNO_3 -> applyTecnoConfigDrivenWatermark(context, source, config, 3)
            WatermarkStyle.TECNO_4 -> applyTecnoConfigDrivenWatermark(context, source, config, 4)
            // Config-driven watermarks
            WatermarkStyle.VIVO_ZEISS_0 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/zeiss0.txt")
            WatermarkStyle.VIVO_ZEISS_1 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/zeiss1.txt")
            WatermarkStyle.VIVO_ZEISS_2 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/zeiss2.txt")
            WatermarkStyle.VIVO_ZEISS_3 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/zeiss3.txt")
            WatermarkStyle.VIVO_ZEISS_4 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/zeiss4.txt")
            WatermarkStyle.VIVO_ZEISS_5 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/zeiss5.txt")
            WatermarkStyle.VIVO_ZEISS_6 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/zeiss6.txt")
            WatermarkStyle.VIVO_ZEISS_7 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/zeiss7.txt")
            WatermarkStyle.VIVO_ZEISS_8 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/zeiss8.txt")
            WatermarkStyle.VIVO_IQOO_4 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/iqoo4.txt")
            WatermarkStyle.VIVO_COMMON_IQOO4 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/common_iqoo4.txt")
            WatermarkStyle.VIVO_1 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/vivo1.txt")
            WatermarkStyle.VIVO_2 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/vivo2.txt")
            WatermarkStyle.VIVO_3 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/vivo3.txt")
            WatermarkStyle.VIVO_4 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/vivo4.txt")
            WatermarkStyle.VIVO_5 -> applyVivoConfigDriven(context, source, config, "vivo_watermark_full2/assets/zeiss_editors/vivo5.txt")
        }
    }

    /**
     * Frame watermark: adds a white border at the bottom of the image.
     * Layout faithfully follows FrameWatermark/content.json.
     *
     * Right side block (right|bottom, marginRight=192):
     *   With logo: [logo (h=388, centered)] [88px gap] [text column]
     *   Without logo: [text column at right|bottom, marginBottom=184/220]
     *
     * Text column:
     *   Narrow (device<=2680): lens size=136/baseline=126, secondary size=104/baseline=110
     *   Wide   (device>2680):  lens size=120/baseline=126, secondary size=93/baseline=110
     *
     * Left side: device name (height=416, margin=[192,0,0,136])
     */
    private fun applyFrameWatermark(
        context: Context,
        source: Bitmap,
        config: WatermarkConfig
    ): Bitmap {
        val imgWidth = source.width
        val imgHeight = source.height
        val scale = imgWidth / BASE_WIDTH

        val borderHeight = (FRAME_BORDER_HEIGHT * scale).toInt()
        val totalHeight = imgHeight + borderHeight

        val result = Bitmap.createBitmap(imgWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw original image
        canvas.drawBitmap(source, 0f, 0f, null)

        // Draw white border (backgroundElements: color=#FFFFFF, alpha=1)
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, imgHeight.toFloat(), imgWidth.toFloat(), totalHeight.toFloat(), borderPaint)

        // Load Honor logo
        val logoBitmap = try {
            context.assets.open("watermark/Honor/FrameWatermark/logo.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }

        // Determine layout variant based on device element width range
        val isWideLayout = imgWidth > (2680 * scale)

        // Template dimensions (at BASE_WIDTH=6144)
        val marginRight = 192f
        val logoHeight = 388f
        val logoMarginGap = 88f

        // Text dimensions from template
        val lensFontSize: Float
        val lensBaseline: Float
        val lensBlockHeight: Float
        val lensTopMargin: Float
        val secondaryFontSize: Float
        val secondaryBaseline: Float
        val secondaryTopMargin: Float
        val timeLocationGap = 46f

        // Dimensions for lens-only without logo variant
        val lensOnlyFontSize: Float
        val lensOnlyBaseline: Float

        if (isWideLayout) {
            lensFontSize = 120f; lensBaseline = 126f; lensBlockHeight = 140f; lensTopMargin = 192f
            secondaryFontSize = 93f; secondaryBaseline = 110f; secondaryTopMargin = 24f
            lensOnlyFontSize = 116f; lensOnlyBaseline = 123f
        } else {
            lensFontSize = 136f; lensBaseline = 126f; lensBlockHeight = 159f; lensTopMargin = 201f
            secondaryFontSize = 104f; secondaryBaseline = 110f; secondaryTopMargin = 4f
            lensOnlyFontSize = 150f; lensOnlyBaseline = 159f
        }

        // Create paints with Honor font
        val lensPaint = createHonorPaint(context, 400).apply {
            color = Color.BLACK
            textSize = lensFontSize * scale
            textAlign = Paint.Align.LEFT
        }

        val secondaryPaint = createHonorPaint(context, 300).apply {
            color = Color.parseColor("#999999")
            textSize = secondaryFontSize * scale
            textAlign = Paint.Align.LEFT
        }

        // Prepare text content
        val lensText = config.lensInfo ?: ""
        val timeText = config.timeText ?: ""
        val locText = config.locationText ?: ""

        val hasLens = lensText.isNotEmpty()
        val hasTime = timeText.isNotEmpty()
        val hasLoc = locText.isNotEmpty()
        val hasSecondary = hasTime || hasLoc
        val hasLogo = logoBitmap != null

        // Measure text widths
        val lensWidth = if (hasLens) lensPaint.measureText(lensText) else 0f
        val timeWidth = if (hasTime) secondaryPaint.measureText(timeText) else 0f
        val gapWidth = if (hasTime && hasLoc) timeLocationGap * scale else 0f
        val locWidth = if (hasLoc) secondaryPaint.measureText(locText) else 0f
        val secondaryTotalWidth = timeWidth + gapWidth + locWidth
        val textBlockWidth = maxOf(lensWidth, secondaryTotalWidth)

        val scaledMarginRight = marginRight * scale
        val borderTop = imgHeight.toFloat()

        if (hasLogo && (hasLens || hasSecondary)) {
            // --- Layout: logo + text column, right|bottom aligned ---
            val textBlockRight = imgWidth - scaledMarginRight
            val textBlockLeft = textBlockRight - textBlockWidth

            // Draw logo (vertically centered in border)
            val scaledLogoHeight = logoHeight * scale
            val logoScale = scaledLogoHeight / logoBitmap!!.height.toFloat()
            val logoDrawWidth = logoBitmap.width * logoScale

            val logoX = textBlockLeft - (logoMarginGap * scale) - logoDrawWidth
            val logoY = borderTop + (borderHeight - scaledLogoHeight) / 2f

            val logoRect = Rect(
                logoX.toInt(), logoY.toInt(),
                (logoX + logoDrawWidth).toInt(), (logoY + scaledLogoHeight).toInt()
            )
            canvas.drawBitmap(logoBitmap, null, logoRect, Paint(Paint.FILTER_BITMAP_FLAG))
            logoBitmap.recycle()

            // Draw lens text (using baseline from template)
            if (hasLens) {
                val lensY = borderTop + (lensTopMargin * scale) + (lensBaseline * scale)
                canvas.drawText(lensText, textBlockLeft, lensY, lensPaint)
            }

            // Draw time and location
            if (hasSecondary) {
                val secondaryY = if (hasLens) {
                    borderTop + (lensTopMargin * scale) + (lensBlockHeight * scale) + (secondaryTopMargin * scale) + (secondaryBaseline * scale)
                } else {
                    // Center vertically if no lens
                    borderTop + borderHeight / 2f + (secondaryBaseline * scale) / 3f
                }

                var currentX = textBlockLeft
                if (hasTime) {
                    canvas.drawText(timeText, currentX, secondaryY, secondaryPaint)
                    currentX += timeWidth + gapWidth
                }
                if (hasLoc) {
                    canvas.drawText(locText, currentX, secondaryY, secondaryPaint)
                }
            }
        } else if (!hasLogo && hasLens) {
            // --- Layout: text only, no logo (vertical layout, right|bottom) ---
            // Template: margin=[0,0,192,184/220], layout_gravity=right|bottom
            val noLogoBottomMargin = (if (isWideLayout) 220f else 184f) * scale

            if (hasSecondary) {
                // Lens + secondary text
                val lensY = borderTop + borderHeight - noLogoBottomMargin - (lensBlockHeight * scale) - (secondaryTopMargin * scale) + (lensBaseline * scale)
                lensPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(lensText, imgWidth - scaledMarginRight, lensY, lensPaint)

                val secondaryY = borderTop + borderHeight - noLogoBottomMargin + (secondaryBaseline * scale)
                secondaryPaint.textAlign = Paint.Align.RIGHT
                var currentX = imgWidth - scaledMarginRight
                // Right-align, draw location first then time to the left
                if (hasLoc && hasTime) {
                    canvas.drawText(locText, currentX, secondaryY, secondaryPaint)
                    currentX -= locWidth + gapWidth
                    canvas.drawText(timeText, currentX, secondaryY, secondaryPaint)
                } else if (hasTime) {
                    canvas.drawText(timeText, currentX, secondaryY, secondaryPaint)
                } else if (hasLoc) {
                    canvas.drawText(locText, currentX, secondaryY, secondaryPaint)
                }
            } else {
                // Lens only: use larger standalone font
                val lensOnlyPaint = createHonorPaint(context, 400).apply {
                    color = Color.BLACK
                    textSize = lensOnlyFontSize * scale
                    textAlign = Paint.Align.RIGHT
                }
                val lensOnlyMarginBottom = (if (isWideLayout) 263f else 239f) * scale
                val lensY = borderTop + borderHeight - lensOnlyMarginBottom
                canvas.drawText(lensText, imgWidth - scaledMarginRight, lensY, lensOnlyPaint)
            }
        } else if (hasLogo) {
            // Logo only (lens-only with logo uses bigger font)
            val scaledLogoHeight = logoHeight * scale
            val logoScale = scaledLogoHeight / logoBitmap!!.height.toFloat()
            val logoDrawWidth = logoBitmap.width * logoScale

            val logoX = imgWidth - scaledMarginRight - logoDrawWidth
            val logoY = borderTop + (borderHeight - scaledLogoHeight) / 2f

            val logoRect = Rect(
                logoX.toInt(), logoY.toInt(),
                (logoX + logoDrawWidth).toInt(), (logoY + scaledLogoHeight).toInt()
            )
            canvas.drawBitmap(logoBitmap, null, logoRect, Paint(Paint.FILTER_BITMAP_FLAG))
            logoBitmap.recycle()
        }

        // Draw device name on left side
        // Template: device height=416, margin=[192,0,0,136], layout_gravity=left|bottom
        // Element is vertically centered in the 688px border (136 top + 416 element + 136 bottom)
        if (!config.deviceName.isNullOrEmpty()) {
            val deviceMarginLeft = 192f * scale

            val devicePaint = createHonorPaint(context, 800).apply {
                color = Color.BLACK
                textSize = 150f * scale
                textAlign = Paint.Align.LEFT
            }

            // Vertically center text within the 416px element box
            val elementTop = borderTop + 136f * scale
            val elementBottom = totalHeight.toFloat() - 136f * scale
            val elementCenterY = (elementTop + elementBottom) / 2f
            val deviceY = elementCenterY - (devicePaint.ascent() + devicePaint.descent()) / 2f
            canvas.drawText(config.deviceName, deviceMarginLeft, deviceY, devicePaint)
        }

        return result
    }

    /**
     * Text watermark: overlays time and location on the bottom-right of the image.
     * Faithfully follows TextWatermark/content.json.
     *
     * Narrow (<=3072): time size=168 baseline=156, location size=152 baseline=161,
     *                  margin=[0,0,304,112], locationMarginTop=-21
     * Wide   (>3072):  time size=144 baseline=134, location size=128 baseline=136,
     *                  margin=[0,0,304,152], locationMarginTop=-4
     * Device: left|bottom, height=464, margin=[304,0,0,176]
     */
    private fun applyTextWatermark(
        context: Context,
        source: Bitmap,
        config: WatermarkConfig
    ): Bitmap {
        val imgWidth = source.width
        val imgHeight = source.height
        val scale = imgWidth / BASE_WIDTH

        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Determine layout variant based on device width range
        val isWideLayout = imgWidth > (3072 * scale)

        // Template values from content.json
        val timeFontSize: Float
        val timeBaseline: Float
        val timeBlockHeight: Float
        val locationFontSize: Float
        val locationBaseline: Float
        val marginRight = 304f
        val marginBottom: Float
        val locationMarginTop: Float

        if (isWideLayout) {
            timeFontSize = 144f; timeBaseline = 134f; timeBlockHeight = 169f
            locationFontSize = 128f; locationBaseline = 136f
            marginBottom = 152f; locationMarginTop = -4f
        } else {
            timeFontSize = 168f; timeBaseline = 156f; timeBlockHeight = 197f
            locationFontSize = 152f; locationBaseline = 161f
            marginBottom = 112f; locationMarginTop = -21f
        }

        val timeText = config.timeText ?: ""
        val locText = config.locationText ?: ""

        // Time paint with Honor font (wght 300, #FFFFFF)
        val timePaint = createHonorPaint(context, 300).apply {
            color = Color.WHITE
            textSize = timeFontSize * scale
            textAlign = Paint.Align.RIGHT
            setShadowLayer(4 * scale, 1 * scale, 1 * scale, Color.argb(80, 0, 0, 0))
        }

        // Location paint with Honor font (wght 300, #FFFFFF)
        val locationPaint = createHonorPaint(context, 300).apply {
            color = Color.WHITE
            textSize = locationFontSize * scale
            textAlign = Paint.Align.RIGHT
            setShadowLayer(4 * scale, 1 * scale, 1 * scale, Color.argb(80, 0, 0, 0))
        }

        val rightX = imgWidth - (marginRight * scale)

        // Vertical layout: time on top, location below (layout_gravity=right|bottom)
        if (timeText.isNotEmpty() && locText.isNotEmpty()) {
            // Location element bottom aligns to marginBottom from image bottom
            // Location baseline is at top of location element + locationBaseline
            // Time+Location vertical stack: time block (height) + locationMarginTop + location block
            
            // Calculate location block bottom
            val locBlockBottom = imgHeight - (marginBottom * scale)
            val locBaselineY = locBlockBottom - locationPaint.descent()
            canvas.drawText(locText, rightX, locBaselineY, locationPaint)

            // Time block sits above location, with locationMarginTop gap
            val timeBlockBottom = locBlockBottom - locationPaint.textSize + (locationMarginTop * scale)
            val timeBaselineY = timeBlockBottom - timePaint.descent()
            canvas.drawText(timeText, rightX, timeBaselineY, timePaint)
        } else if (timeText.isNotEmpty()) {
            // Time only: use larger size variant from template
            val timeOnlyPaint = createHonorPaint(context, 300).apply {
                color = Color.WHITE
                textSize = (if (isWideLayout) 144f else 184f) * scale
                textAlign = Paint.Align.RIGHT
                setShadowLayer(4 * scale, 1 * scale, 1 * scale, Color.argb(80, 0, 0, 0))
            }
            val bottomMargin = (if (isWideLayout) 232f else 192f) * scale
            canvas.drawText(timeText, rightX, imgHeight - bottomMargin, timeOnlyPaint)
        } else if (locText.isNotEmpty()) {
            // Location only: use larger size variant from template
            val locOnlyPaint = createHonorPaint(context, 300).apply {
                color = Color.WHITE
                textSize = (if (isWideLayout) 140f else 184f) * scale
                textAlign = Paint.Align.RIGHT
                setShadowLayer(4 * scale, 1 * scale, 1 * scale, Color.argb(80, 0, 0, 0))
            }
            val bottomMargin = (if (isWideLayout) 216f else 192f) * scale
            canvas.drawText(locText, rightX, imgHeight - bottomMargin, locOnlyPaint)
        }

        // Draw device name on left side
        // Template: device height=464, margin=[304,0,0,176], left|bottom
        // Vertically center text within the 464px element box
        if (!config.deviceName.isNullOrEmpty()) {
            val deviceMarginLeft = 304f * scale

            val devicePaint = createHonorPaint(context, 1000).apply {
                color = Color.WHITE
                textSize = 140f * scale
                textAlign = Paint.Align.LEFT
                setShadowLayer(4 * scale, 1 * scale, 1 * scale, Color.argb(80, 0, 0, 0))
            }

            val elementBottom = imgHeight.toFloat() - 176f * scale
            val elementTop = elementBottom - 464f * scale
            val elementCenterY = (elementTop + elementBottom) / 2f
            val deviceY = elementCenterY - (devicePaint.ascent() + devicePaint.descent()) / 2f
            canvas.drawText(config.deviceName, deviceMarginLeft, deviceY, devicePaint)
        }

        return result
    }

    /**
     * Generates a default time string matching Honor format.
     */
    fun getDefaultTimeString(): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Generates a time string from EXIF datetime.
     */
    fun formatExifDateTime(exifDateTime: String?): String? {
        if (exifDateTime.isNullOrEmpty()) return null
        return try {
            val inputFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            val date = inputFormat.parse(exifDateTime)
            date?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Builds lens info string from EXIF data.
     * Format: "27mm  f/1.9  1/100s  ISO1600"
     */
    fun buildLensInfoFromExif(
        focalLength: String?,
        fNumber: String?,
        exposureTime: String?,
        iso: String?
    ): String {
        val parts = mutableListOf<String>()
        focalLength?.let { parts.add("${it}mm") }
        fNumber?.let { parts.add("f/$it") }
        exposureTime?.let { parts.add("${it}s") }
        iso?.let { parts.add("ISO$it") }
        return parts.joinToString("  ")
    }

    /**
     * Frame watermark YG variant (Harcourt Touch Paris collaboration).
     * Based on FrameWatermarkYG/content.json:
     *   - White border at bottom (688px at 6144 base, same as standard Frame)
     *   - Device name on left: height=416, margin=[192,0,0,136], left|bottom
     *   - YG logo (672×504 @6144 width) at right-bottom, margin=[0,0,188,92]
     */
    private fun applyFrameWatermarkYG(
        context: Context,
        source: Bitmap,
        config: WatermarkConfig
    ): Bitmap {
        val imgWidth = source.width
        val imgHeight = source.height
        val scale = imgWidth / BASE_WIDTH

        val borderHeight = (FRAME_BORDER_HEIGHT * scale).toInt()
        val totalHeight = imgHeight + borderHeight

        val result = Bitmap.createBitmap(imgWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw original image
        canvas.drawBitmap(source, 0f, 0f, null)

        // Draw white border
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, imgHeight.toFloat(), imgWidth.toFloat(), totalHeight.toFloat(), borderPaint)

        val borderTop = imgHeight.toFloat()

        // Draw device name on left (same positioning as standard FrameWatermark)
        // Template: device height=416, margin=[192,0,0,136], layout_gravity=left|bottom
        if (!config.deviceName.isNullOrEmpty()) {
            val devicePaint = createHonorPaint(context, 1000).apply {
                color = Color.BLACK
                textSize = 150f * scale
                textAlign = Paint.Align.LEFT
            }
            val deviceMarginLeft = 192f * scale
            val elementTop = borderTop + 136f * scale
            val elementBottom = totalHeight.toFloat() - 136f * scale
            val elementCenterY = (elementTop + elementBottom) / 2f
            val deviceY = elementCenterY - (devicePaint.ascent() + devicePaint.descent()) / 2f
            canvas.drawText(config.deviceName, deviceMarginLeft, deviceY, devicePaint)
        }

        // Load and draw YG logo
        // From content.json: width=672, height=504, margin=[0,0,188,92], right|bottom
        val ygBitmap = try {
            context.assets.open("watermark/Honor/FrameWatermarkYG/yg.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }

        ygBitmap?.let { yg ->
            val logoDrawWidth = 672f * scale
            val logoDrawHeight = 504f * scale
            val marginRight = 188f * scale
            val marginBottom = 92f * scale

            val logoX = imgWidth - logoDrawWidth - marginRight
            val logoY = totalHeight - logoDrawHeight - marginBottom

            val logoRect = Rect(
                logoX.toInt(), logoY.toInt(),
                (logoX + logoDrawWidth).toInt(), (logoY + logoDrawHeight).toInt()
            )
            canvas.drawBitmap(yg, null, logoRect, Paint(Paint.FILTER_BITMAP_FLAG))
            yg.recycle()
        }

        return result
    }

    /**
     * Text watermark YG variant (Harcourt Touch Paris collaboration).
     * Based on TextWatermarkYG/content.json:
     *   - Device name on left (overlaid on image): height=464, margin=[304,0,0,176], left|bottom
     *   - YG logo (672×504 @6144 width) at right-bottom, margin=[0,0,299,86]
     */
    private fun applyTextWatermarkYG(
        context: Context,
        source: Bitmap,
        config: WatermarkConfig
    ): Bitmap {
        val imgWidth = source.width
        val imgHeight = source.height
        val scale = imgWidth / BASE_WIDTH

        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Draw device name on left (same positioning as standard TextWatermark)
        // Template: device height=464, margin=[304,0,0,176], left|bottom
        if (!config.deviceName.isNullOrEmpty()) {
            val devicePaint = createHonorPaint(context, 1000).apply {
                color = Color.WHITE
                textSize = 140f * scale
                textAlign = Paint.Align.LEFT
                setShadowLayer(4 * scale, 1 * scale, 1 * scale, Color.argb(80, 0, 0, 0))
            }
            val deviceMarginLeft = 304f * scale
            val elementBottom = imgHeight.toFloat() - 176f * scale
            val elementTop = elementBottom - 464f * scale
            val elementCenterY = (elementTop + elementBottom) / 2f
            val deviceY = elementCenterY - (devicePaint.ascent() + devicePaint.descent()) / 2f
            canvas.drawText(config.deviceName, deviceMarginLeft, deviceY, devicePaint)
        }

        // Load and draw YG logo
        // From content.json: width=672, height=504, margin=[0,0,299,86], right|bottom
        val ygBitmap = try {
            context.assets.open("watermark/Honor/TextWatermarkYG/yg.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }

        ygBitmap?.let { yg ->
            val logoDrawWidth = 672f * scale
            val logoDrawHeight = 504f * scale
            val marginRight = 299f * scale
            val marginBottom = 86f * scale

            val logoX = imgWidth - logoDrawWidth - marginRight
            val logoY = imgHeight - logoDrawHeight - marginBottom

            val logoRect = Rect(
                logoX.toInt(), logoY.toInt(),
                (logoX + logoDrawWidth).toInt(), (logoY + logoDrawHeight).toInt()
            )
            canvas.drawBitmap(yg, null, logoRect, Paint(Paint.FILTER_BITMAP_FLAG))
            yg.recycle()
        }

        return result
    }

    // ==================== Meizu Watermarks ====================

    // Cached Meizu typefaces
    private var meizuDeviceTypeface: Typeface? = null  // MEIZUCamera-Medium (typeface="-1")
    private var meizuTextMedium: Typeface? = null       // TT Fors Medium
    private var meizuTextRegular: Typeface? = null      // TT Fors Regular

    private fun getMeizuDeviceTypeface(context: Context): Typeface {
        meizuDeviceTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/Meizu/fonts/MEIZUCamera-Medium.otf").also {
                meizuDeviceTypeface = it
            }
        } catch (_: Exception) {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }

    private fun getMeizuTextMedium(context: Context): Typeface {
        meizuTextMedium?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/Meizu/fonts/TTForsMedium.ttf").also {
                meizuTextMedium = it
            }
        } catch (_: Exception) {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }

    private fun getMeizuTextRegular(context: Context): Typeface {
        meizuTextRegular?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/Meizu/fonts/TTForsRegular.ttf").also {
                meizuTextRegular = it
            }
        } catch (_: Exception) {
            Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    private fun loadMeizuLogo(context: Context, name: String): Bitmap? {
        return try {
            context.assets.open("watermark/Meizu/logos/$name").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) { null }
    }

    /** Meizu brand accent: small filled red circle. */
    private fun drawMeizuRedDot(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 65, 50)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 8f * s, dotPaint)
    }

    /**
     * Split lens info into discrete parts for separator-style rendering.
     * Splits by double-space or explicit "|" delimiter in user input.
     */
    private fun splitDiscreteParts(lensInfo: String?): List<String> {
        if (lensInfo.isNullOrBlank()) return emptyList()
        // If user already included "|", split by that
        if ("|" in lensInfo) return lensInfo.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        // Otherwise split by 2+ spaces
        return lensInfo.split(Regex("\\s{2,}")).filter { it.isNotEmpty() }
    }

    /**
     * Draws discrete text parts separated by thin "|" lines, centered horizontally.
     * Used by z3, z5, z7 for lensInfo_discrete / lensInfo_location rendering.
     * @param separatorColor color for the "|" line
     */
    private fun drawDiscreteText(
        canvas: Canvas,
        parts: List<String>,
        centerX: Float,
        baselineY: Float,
        textPaint: Paint,
        s: Float,
        separatorColor: Int = Color.parseColor("#D9D9D9"),
        gap: Float = 20f * s  // gap on each side of the separator
    ) {
        if (parts.isEmpty()) return
        if (parts.size == 1) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(parts[0], centerX, baselineY, textPaint)
            return
        }

        val sepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = separatorColor
            strokeWidth = 1f * s
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        // Measure total width
        textPaint.textAlign = Paint.Align.LEFT
        var totalWidth = 0f
        for (i in parts.indices) {
            totalWidth += textPaint.measureText(parts[i])
            if (i < parts.size - 1) totalWidth += gap * 2f // gap + sep + gap
        }

        // Draw from left, centered
        var x = centerX - totalWidth / 2f
        // Separator should visually match the text box height.
        val sepTop = baselineY + textPaint.ascent() * 0.92f
        val sepBottom = baselineY + textPaint.descent() * 0.92f

        for (i in parts.indices) {
            canvas.drawText(parts[i], x, baselineY, textPaint)
            x += textPaint.measureText(parts[i])
            if (i < parts.size - 1) {
                x += gap
                canvas.drawLine(x, sepTop, x, sepBottom, sepPaint)
                x += gap
            }
        }
    }

    // ---- Norm ----
    /**
     * Norm: Transparent overlay on image bottom. Horizontal container with
     * device (44sp, white, bold) + nickName (30sp, white 0.8α) + time (30sp, white 0.8α)
     * XML: type=1, basePortWidth=1530, container height=122, transY=-122, marginL/R=78
     */
    private fun applyMeizuNorm(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgWidth = source.width
        val imgHeight = source.height
        val s = imgWidth / 1530f

        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val containerH = 122f * s
        val marginLR = 78f * s
        val containerTop = imgHeight - containerH

        val deviceText = config.deviceName ?: ""
        val timeText = config.timeText ?: ""

        val devicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuDeviceTypeface(context)
            textSize = 44f * s
            color = Color.WHITE
            textAlign = Paint.Align.LEFT
            setShadowLayer(4f * s, 1f * s, 1f * s, Color.argb(80, 0, 0, 0))
        }

        val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuTextRegular(context)
            textSize = 30f * s
            color = Color.WHITE
            alpha = (255 * 0.8f).toInt()
            textAlign = Paint.Align.LEFT
            setShadowLayer(4f * s, 1f * s, 1f * s, Color.argb(80, 0, 0, 0))
        }

        val centerY = containerTop + containerH / 2f
        var currentX = marginLR
        val itemGap = 8f * s

        if (deviceText.isNotEmpty()) {
            val y = centerY - (devicePaint.ascent() + devicePaint.descent()) / 2f
            canvas.drawText(deviceText, currentX, y, devicePaint)
            currentX += devicePaint.measureText(deviceText) + itemGap
        }

        if (timeText.isNotEmpty()) {
            val y = centerY - (secondaryPaint.ascent() + secondaryPaint.descent()) / 2f
            canvas.drawText(timeText, currentX, y, secondaryPaint)
        }

        return result
    }

    // ---- Pro ----
    /**
     * Pro: White bottom bar. Left: device (45sp, black, MEIZUCamera) + red dot.
     * Right: vertical stack of lensInfo (35sp, black) + time (24sp, #A6A6A6 letterSpacing=0.1).
     * XML: type=1, basePortWidth=1530, background=#FFFFFF, container height=160, marginL/R=85
     */
    private fun applyMeizuPro(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgWidth = source.width
        val imgHeight = source.height
        val s = imgWidth / 1530f

        val barHeightF = 160f * s
        val barHeight = rint(barHeightF)
        val totalHeight = imgHeight + barHeight

        val result = Bitmap.createBitmap(imgWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        // White bar
        val whitePaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        canvas.drawRect(0f, imgHeight.toFloat(), imgWidth.toFloat(), totalHeight.toFloat(), whitePaint)

        val barTop = imgHeight.toFloat()
        val marginL = 85f * s
        val marginR = 85f * s
        val barCenterY = barTop + barHeightF / 2f

        // Device (left, vertically centered)
        val deviceText = config.deviceName ?: ""
        val devicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuDeviceTypeface(context)
            textSize = 45f * s
            color = Color.BLACK
            textAlign = Paint.Align.LEFT
        }

        var deviceEndX = marginL
        if (deviceText.isNotEmpty()) {
            val y = barCenterY - (devicePaint.ascent() + devicePaint.descent()) / 2f
            canvas.drawText(deviceText, marginL, y, devicePaint)
            deviceEndX = marginL + devicePaint.measureText(deviceText) + 20f * s
            // Red dot after device name
            drawMeizuRedDot(canvas, deviceEndX, barCenterY, s)
        }

        // Right column: lensInfo (top) + time (below)
        val lensText = config.lensInfo ?: ""
        val timeText = config.timeText ?: ""
        val rightX = imgWidth - marginR

        val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuTextMedium(context)
            textSize = 35f * s
            color = Color.BLACK
            textAlign = Paint.Align.RIGHT
        }

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuTextRegular(context)
            textSize = 24f * s
            color = Color.parseColor("#A6A6A6")
            letterSpacing = 0.1f
            textAlign = Paint.Align.RIGHT
        }

        if (lensText.isNotEmpty() && timeText.isNotEmpty()) {
            val lensH = lensPaint.descent() - lensPaint.ascent()
            val gapH = 3f * s
            val timeH = timePaint.descent() - timePaint.ascent()
            val totalH = lensH + gapH + timeH
            val groupTop = barTop + (barHeightF - totalH) / 2f

            canvas.drawText(lensText, rightX, groupTop - lensPaint.ascent(), lensPaint)
            canvas.drawText(timeText, rightX, groupTop + lensH + gapH - timePaint.ascent(), timePaint)
        } else if (lensText.isNotEmpty()) {
            val y = barCenterY - (lensPaint.ascent() + lensPaint.descent()) / 2f
            canvas.drawText(lensText, rightX, y, lensPaint)
        } else if (timeText.isNotEmpty()) {
            val y = barCenterY - (timePaint.ascent() + timePaint.descent()) / 2f
            canvas.drawText(timeText, rightX, y, timePaint)
        }

        return result
    }

    // ---- Z1 ----
    /**
     * Z1: White frame, photo inset. Centered device name + lens info below.
     * XML: type=2, basePortWidth=1470, image margin L/T/R=30,
     *      device marginT=40 size=45 black, lensInfo marginT=16 marginB=51 size=32 gray 0.6α
     * Reference: upper text row (device, black bold) + lower text row (lens, light gray 4 sub-groups)
     */
    private fun applyMeizuZ1(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val s = source.width / 1470f
        val marginSide = 30f * s
        val marginTop = 30f * s

        val deviceText = config.deviceName ?: ""
        val lensText = config.lensInfo ?: ""

        val devicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuDeviceTypeface(context)
            textSize = 45f * s
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuTextRegular(context)
            textSize = 32f * s
            color = Color.parseColor("#49454F")
            alpha = (255 * 0.6f).toInt()
            textAlign = Paint.Align.CENTER
        }

        val deviceH = if (deviceText.isNotEmpty()) (devicePaint.descent() - devicePaint.ascent()) else 0f
        val lensH = if (lensText.isNotEmpty()) (lensPaint.descent() - lensPaint.ascent()) else 0f
        val textAreaH = 40f * s + deviceH +
            (if (lensText.isNotEmpty()) 16f * s + lensH else 0f) + 51f * s

        val photoW = rint(source.width - 2 * marginSide)
        val photoH = rint(source.height * (photoW / source.width.toFloat()))
        val totalW = source.width
        val totalH = rint(marginTop + photoH + textAreaH)

        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val photoRect = rectF(marginSide, marginTop, marginSide + photoW, marginTop + photoH)
        canvas.drawBitmap(source, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG))

        val centerX = totalW / 2f
        var currentY = marginTop + photoH + 40f * s

        if (deviceText.isNotEmpty()) {
            currentY -= devicePaint.ascent()
            canvas.drawText(deviceText, centerX, currentY, devicePaint)
            currentY += devicePaint.descent()
        }

        if (lensText.isNotEmpty()) {
            currentY += 16f * s
            currentY -= lensPaint.ascent()
            canvas.drawText(lensText, centerX, currentY, lensPaint)
        }

        return result
    }

    // ---- Z2 ----
    /**
     * Z2: Polaroid-style wide white frame. Adaptive icon (left) + lens info (right) at bottom.
     * XML: type=2, basePortWidth=1130, image margin=200,
     *      bottom container marginT=247 marginB=245 marginLR=200,
     *      adaptiveIcon 462×48 left, lensInfo 28sp #3C3C43 0.6α right
     * Reference: large uniform frame, device text (left) + light gray lens groups (right)
     */
    private fun applyMeizuZ2(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val s = source.width / 1130f
        val margin = 200f * s
        val marginT = 200f * s

        val lensText = config.lensInfo ?: ""
        val deviceText = config.deviceName ?: ""

        val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuTextRegular(context)
            textSize = 28f * s
            color = Color.parseColor("#3C3C43")
            alpha = (255 * 0.6f).toInt()
            textAlign = Paint.Align.RIGHT
        }

        // Device as substitute for encrypted adaptive icon (462×48)
        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuDeviceTypeface(context)
            textSize = 38f * s
            color = Color.BLACK
            textAlign = Paint.Align.LEFT
        }

        val iconH = 48f * s
        val bottomBarH = 247f * s + iconH + 245f * s

        val photoW = rint(source.width - 2 * margin)
        val photoH = rint(source.height * (photoW / source.width.toFloat()))
        val totalW = source.width
        val totalH = rint(marginT + photoH + bottomBarH)

        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val photoRect = rectF(margin, marginT, margin + photoW, marginT + photoH)
        canvas.drawBitmap(source, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG))

        // Bottom: device text (left) + lens info (right), vertically centered in icon row
        val barCenterY = marginT + photoH + 247f * s + iconH / 2f

        if (deviceText.isNotEmpty()) {
            val y = barCenterY - (logoPaint.ascent() + logoPaint.descent()) / 2f
            canvas.drawText(deviceText, margin, y, logoPaint)
        }

        if (lensText.isNotEmpty()) {
            val y = barCenterY - (lensPaint.ascent() + lensPaint.descent()) / 2f
            canvas.drawText(lensText, totalW - margin, y, lensPaint)
        }

        return result
    }

    // ---- Z3 ----
    /**
     * Z3: White frame, photo inset. Device name (50sp, black, centered) above.
     * Discrete lens info with "|" separators below (30sp, gray 0.6α).
     * XML: type=2, basePortWidth=1470, image margin L/T/R=30,
     *      device marginT=53 size=50, lensInfo_discrete w=859 h=107 marginT=53 marginB=75 size=30
     * Reference: TEXT ····· TEXT | TEXT | TEXT pattern with 1px gray separators
     */
    private fun applyMeizuZ3(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val s = source.width / 1470f
        val marginSide = 30f * s
        val marginTop = 30f * s

        val deviceText = config.deviceName ?: ""
        val lensParts = splitDiscreteParts(config.lensInfo)

        val devicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuDeviceTypeface(context)
            textSize = 50f * s
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuTextRegular(context)
            textSize = 30f * s
            color = Color.parseColor("#49454F")
            alpha = (255 * 0.6f).toInt()
        }

        val deviceH = if (deviceText.isNotEmpty()) (devicePaint.descent() - devicePaint.ascent()) else 0f
        val discreteAreaH = 107f * s
        val textAreaH = 53f * s + deviceH +
            (if (lensParts.isNotEmpty()) 53f * s + discreteAreaH else 0f) + 75f * s

        val photoW = rint(source.width - 2 * marginSide)
        val photoH = rint(source.height * (photoW / source.width.toFloat()))
        val totalW = source.width
        val totalH = rint(marginTop + photoH + textAreaH)

        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val photoRect = rectF(marginSide, marginTop, marginSide + photoW, marginTop + photoH)
        canvas.drawBitmap(source, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG))

        val centerX = totalW / 2f
        var currentY = marginTop + photoH + 53f * s

        if (deviceText.isNotEmpty()) {
            currentY -= devicePaint.ascent()
            canvas.drawText(deviceText, centerX, currentY, devicePaint)
            currentY += devicePaint.descent()
        }

        if (lensParts.isNotEmpty()) {
            currentY += 53f * s
            val baselineY = currentY + discreteAreaH / 2f - (lensPaint.ascent() + lensPaint.descent()) / 2f
            drawDiscreteText(canvas, lensParts, centerX, baselineY, lensPaint, s)
        }

        return result
    }

    // ---- Z4 ----
    /**
     * Z4: Photo fills left side, white panel on right with rotated text + red dot.
     * XML: type=2, basePortWidth=1530, background=#FFFFFF, orientation=horizontal,
     *      lensInfo marginB=100 marginL=40 size=32 rotation=90 gray 0.6α,
     *      device marginB=143 marginL=15 marginR=40 size=45 black rotation=90
     * Reference: 50px right panel at 383px scale, text reads bottom-to-top,
     *            device (black) + lens (black) stacked, red dot at bottom.
     * Right panel width = marginL(40) + lensTextH(32) + gap(15) + deviceTextH(45) + marginR(40) = 172
     */
    private fun applyMeizuZ4(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val s = source.width / 1530f   // NOTE: scale from basePortWidth minus panel would be more accurate
        // but using full base width for consistent scaling

        // Right panel dimensions
        val panelMarginL = 40f * s    // gap between photo and first text
        val lensTextH = 32f * s       // font size → horizontal width when rotated
        val textGap = 15f * s         // gap between lens and device text columns
        val deviceTextH = 45f * s
        val panelMarginR = 40f * s
        val panelWidth = panelMarginL + lensTextH + textGap + deviceTextH + panelMarginR

        // Photo occupies the remaining width
        val photoW = rint(source.width - panelWidth)
        val photoH = rint(source.height * (photoW / source.width.toFloat()))
        val totalW = source.width
        val totalH = photoH

        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        // Photo on left
        val photoRect = Rect(0, 0, photoW, photoH)
        canvas.drawBitmap(source, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG))

        val deviceText = config.deviceName ?: ""
        val lensText = config.lensInfo ?: ""

        val devicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuDeviceTypeface(context)
            textSize = 45f * s
            color = Color.BLACK
            textAlign = Paint.Align.LEFT
        }
        val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuTextRegular(context)
            textSize = 32f * s
            color = Color.parseColor("#49454F")
            alpha = (255 * 0.6f).toInt()
            textAlign = Paint.Align.LEFT
        }

        // Device text: rotated 90° CW, reads bottom-to-top
        // Positioned at rightmost column, marginB=143 from bottom, marginR=40 from right
        // Device text: rotated 90° CW, reads bottom-to-top
        // In rightmost column, marginB=143 from bottom, marginR=40 from right
        if (deviceText.isNotEmpty()) {
            canvas.save()
            val colCenterX = totalW - panelMarginR - deviceTextH / 2f
            val textStartY = totalH - 143f * s
            canvas.translate(colCenterX, textStartY)
            canvas.rotate(-90f)
            val centerOffset = -(devicePaint.ascent() + devicePaint.descent()) / 2f
            canvas.drawText(deviceText, 0f, centerOffset, devicePaint)
            canvas.restore()
        }

        // Lens text: rotated 90° CW, to the LEFT of device column
        // marginB=100, marginL=40 from image
        if (lensText.isNotEmpty()) {
            canvas.save()
            val colCenterX = totalW - panelMarginR - deviceTextH - textGap - lensTextH / 2f
            val textStartY = totalH - 100f * s
            canvas.translate(colCenterX, textStartY)
            canvas.rotate(-90f)
            val centerOffset = -(lensPaint.ascent() + lensPaint.descent()) / 2f
            canvas.drawText(lensText, 0f, centerOffset, lensPaint)
            canvas.restore()
        }

        // Red dot near bottom of right panel, centered in device column
        val dotColX = totalW - panelMarginR - deviceTextH / 2f
        drawMeizuRedDot(canvas, dotColX, totalH - 40f * s, s)

        return result
    }

    // ---- Z5 ----
    /**
     * Z5: White frame, photo inset. Device name centered + lens+location info below.
     * XML: type=2, basePortWidth=1220, image marginLR=155 marginT=170,
     *      device marginT=150 size=45 black, lensInfo_location marginT=16 marginB=183 size=32 gray 0.6α
     * Reference: 6 light gray text groups below a thin decorative line,
     *            lens parts + gap + location parts
     */
    private fun applyMeizuZ5(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val s = source.width / 1220f
        val marginSide = 155f * s
        val marginTop = 170f * s

        val deviceText = config.deviceName ?: ""
        // Build combined parts: lens info parts + location
        val lensParts = splitDiscreteParts(config.lensInfo)
        val locationParts = if (!config.locationText.isNullOrBlank())
            listOf(config.locationText!!) else emptyList()
        val allParts = lensParts + locationParts

        val devicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuDeviceTypeface(context)
            textSize = 45f * s
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuTextRegular(context)
            textSize = 32f * s
            color = Color.parseColor("#49454F")
            alpha = (255 * 0.6f).toInt()
        }

        val deviceH = if (deviceText.isNotEmpty()) (devicePaint.descent() - devicePaint.ascent()) else 0f
        val infoH = if (allParts.isNotEmpty()) (infoPaint.descent() - infoPaint.ascent()) else 0f
        val textAreaH = 150f * s + deviceH +
            (if (allParts.isNotEmpty()) 16f * s + infoH else 0f) + 183f * s

        val photoW = rint(source.width - 2 * marginSide)
        val photoH = rint(source.height * (photoW / source.width.toFloat()))
        val totalW = source.width
        val totalH = rint(marginTop + photoH + textAreaH)

        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val photoRect = rectF(marginSide, marginTop, marginSide + photoW, marginTop + photoH)
        canvas.drawBitmap(source, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG))

        val centerX = totalW / 2f
        var currentY = marginTop + photoH + 150f * s

        if (deviceText.isNotEmpty()) {
            currentY -= devicePaint.ascent()
            canvas.drawText(deviceText, centerX, currentY, devicePaint)
            currentY += devicePaint.descent()
        }

        if (allParts.isNotEmpty()) {
            currentY += 16f * s
            currentY -= infoPaint.ascent()
            drawDiscreteText(canvas, allParts, centerX, currentY, infoPaint, s)
        }

        return result
    }

    // ---- Z6 ----
    /**
     * Z6: Thin uniform white frame. Flyme logo (white) + lens info OVERLAID on photo.
     * XML: type=2, basePortWidth=1530, image margin=38 on all sides,
     *      icon flyme.png 321×60 center_horizontal transY=-200,
     *      lensInfo center_horizontal transY=-124 size=32 gray 0.6α
     * transY is relative offset from natural position (below image).
     * Negative transY pushes the element upward, overlaying on the photo.
     * Reference: pure thin white frame, logo/text invisible at thumbnail scale (white on photo).
     */
    private fun applyMeizuZ6(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val s = source.width / 1530f
        val margin = 38f * s

        val lensText = config.lensInfo ?: ""

        val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuTextRegular(context)
            textSize = 32f * s
            color = Color.WHITE   // White text overlaid on photo
            alpha = (255 * 0.7f).toInt()
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(60, 0, 0, 0))
        }

        val flymeLogo = loadMeizuLogo(context, "flyme_z6.png")  // white logo for dark overlay

        val photoW = rint(source.width - 2 * margin)
        val photoH = rint(source.height * (photoW / source.width.toFloat()))
        val totalW = source.width
        // Thin uniform frame: margin on all 4 sides
        val totalH = rint(margin + photoH + margin)

        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val photoTop = margin
        val photoRect = rectF(margin, photoTop, margin + photoW, photoTop + photoH)
        canvas.drawBitmap(source, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG))

        val centerX = totalW / 2f
        // Natural Y position of elements = bottom of photo + margin (at the watermark bottom)
        val naturalY = photoTop + photoH + margin

        // Flyme logo overlaid on photo: transY=-200 from natural position
        flymeLogo?.let { logo ->
            val logoW = 321f * s
            val logoH = 60f * s
            val logoCenterY = naturalY - 200f * s
            val logoRect = rectF(
                centerX - logoW / 2f, logoCenterY - logoH / 2f,
                centerX + logoW / 2f, logoCenterY + logoH / 2f
            )
            canvas.drawBitmap(logo, null, logoRect, Paint(Paint.FILTER_BITMAP_FLAG))
            logo.recycle()
        }

        // Lens info overlaid on photo: transY=-124 from natural position
        if (lensText.isNotEmpty()) {
            val textY = naturalY - 124f * s
            canvas.drawText(lensText, centerX, textY, lensPaint)
        }

        return result
    }

    // ---- Z7 ----
    /**
     * Z7: Flyme logo (black) at top + photo + discrete lens info at bottom.
     * XML: type=2, basePortWidth=1470, icon flyme.png 321×60 marginT=134,
     *      image marginLR=30 marginT=106, lensInfo_discrete w=859 h=107 marginT=92 marginB=100 size=30
     *      textColor=#FF000000 alpha=0.6
     * Reference: top has 75px border with centered black logo text,
     *            bottom has 4 text groups separated by 3 "|" separators
     */
    private fun applyMeizuZ7(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val s = source.width / 1470f
        val lensParts = splitDiscreteParts(config.lensInfo)

        val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getMeizuTextRegular(context)
            textSize = 30f * s
            color = Color.BLACK
            alpha = (255 * 0.6f).toInt()
        }

        val flymeLogo = loadMeizuLogo(context, "flyme_z7.png")  // black logo

        val logoTopMargin = 134f * s
        val logoH = 60f * s
        val photoTopMargin = 106f * s
        val photoMarginSide = 30f * s

        val photoW = rint(source.width - 2 * photoMarginSide)
        val photoH = rint(source.height * (photoW / source.width.toFloat()))

        val discreteAreaH = 107f * s
        val lensMarginT = if (lensParts.isNotEmpty()) 92f * s else 0f
        val lensMarginB = if (lensParts.isNotEmpty()) 100f * s else 30f * s

        val totalW = source.width
        val totalH = rint(logoTopMargin + logoH + photoTopMargin + photoH +
            lensMarginT + discreteAreaH + lensMarginB)

        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val centerX = totalW / 2f

        // Flyme logo at top
        var currentY = logoTopMargin
        flymeLogo?.let { logo ->
            val logoW = 321f * s
            val logoRect = rectF(
                centerX - logoW / 2f, currentY,
                centerX + logoW / 2f, currentY + logoH
            )
            canvas.drawBitmap(logo, null, logoRect, Paint(Paint.FILTER_BITMAP_FLAG))
            logo.recycle()
        }
        currentY += logoH + photoTopMargin

        // Photo
        val photoRect = rectF(photoMarginSide, currentY, photoMarginSide + photoW, currentY + photoH)
        canvas.drawBitmap(source, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG))
        currentY += photoH

        // Discrete lens info with separators
        if (lensParts.isNotEmpty()) {
            currentY += lensMarginT
            val baselineY = currentY + discreteAreaH / 2f - (lensPaint.ascent() + lensPaint.descent()) / 2f
            drawDiscreteText(canvas, lensParts, centerX, baselineY, lensPaint, s,
                separatorColor = Color.parseColor("#B0B0B0"))
        }

        return result
    }

    // ==================== vivo Watermarks ====================
    // Based on vivo Camera APK watermark layout definitions (zeiss_editors/*.txt)
    //
    // Template system: coordinates in dp at 360dp = 1080px, bar height ≈ 108dp
    // In actual photos: barH = 0.143 * min(w,h), dpScale = barH / 108
    //
    // Standard layout (ZEISS/vivo/iQOO):
    //   LEFT:  [Logo] [Device Name] [│] [ZEISS logo]
    //   RIGHT: [3A camera info]          (top line)
    //          [datetime  location]      (bottom line)

    // Bar height ratio relative to short side.
    // Template defines 108dp bar on 360dp-wide canvas (= 0.3), but real vivo
    // camera renders at device-density pixels.  0.15 matches pixel-validated
    // references from actual vivo photos.
    private const val VIVO_BAR_RATIO = 0.15f
    // Template bar height in dp (for dpScale calculation).
    // Text sizes in the template are designed for a 108dp-tall bar, but we
    // render smaller bars (0.15 ratio). To keep text readable we treat the
    // template as if its bar were only 78dp, which scales all dp-based font
    // sizes up by ~1.38× inside the same physical bar height.
    private const val VIVO_BAR_DP = 78f

    // Colors from templates
    private val VIVO_3A_ZEISS = Color.argb(204, 0, 0, 0)    // #CC000000 (ZEISS style)
    private val VIVO_3A_STD = Color.parseColor("#666666")     // #FF666666 (vivo/iQOO style)
    private val VIVO_TIME_GRAY = Color.parseColor("#757575")  // #FF757575 (datetime/location)

    // Overlay style ratios (relative to short side / image height)
    private const val VIVO_OV_MARGIN_BOT = 0.065f
    private const val VIVO_OV_MARGIN_LR = 0.040f
    private const val VIVO_OV_FS_DEVICE = 0.032f
    private const val VIVO_OV_FS_SUB = 0.022f

    // Cached vivo typefaces
    private var vivoHeavyTypeface: Typeface? = null
    private var vivoSansExpTypeface: Typeface? = null
    private var vivoRegularTypeface: Typeface? = null
    private var vivoCameraTypeface: Typeface? = null
    private var zeissBoldTypeface: Typeface? = null
    private var iqooBoldTypeface: Typeface? = null
    private var robotoBoldTypeface: Typeface? = null
    private var vivoTypeSimpleBoldTypeface: Typeface? = null

    private fun getVivoHeavy(context: Context): Typeface {
        vivoHeavyTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/vivo/fonts/vivotype-Heavy.ttf").also {
                vivoHeavyTypeface = it
            }
        } catch (_: Exception) { Typeface.create("sans-serif", Typeface.BOLD) }
    }

    /** typeface 7 in templates — primary font for newer vivo watermarks. */
    private fun getVivoSansExp(context: Context): Typeface {
        vivoSansExpTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/vivo/fonts/vivoSansExpVF.ttf").also {
                vivoSansExpTypeface = it
            }
        } catch (_: Exception) { getZeissBold(context) }
    }

    private fun getVivoRegular(context: Context): Typeface {
        vivoRegularTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/vivo/fonts/vivo-Regular.otf").also {
                vivoRegularTypeface = it
            }
        } catch (_: Exception) { Typeface.create("sans-serif", Typeface.NORMAL) }
    }

    private fun getVivoCamera(context: Context): Typeface {
        vivoCameraTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/vivo/fonts/vivoCameraVF.ttf").also {
                vivoCameraTypeface = it
            }
        } catch (_: Exception) { Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    }

    private fun getZeissBold(context: Context): Typeface {
        zeissBoldTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/vivo/fonts/ZEISSFrutigerNextW1G-Bold.ttf").also {
                zeissBoldTypeface = it
            }
        } catch (_: Exception) { Typeface.create("sans-serif-medium", Typeface.BOLD) }
    }

    private fun getIqooBold(context: Context): Typeface {
        iqooBoldTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/vivo/fonts/IQOOTYPE-Bold.ttf").also {
                iqooBoldTypeface = it
            }
        } catch (_: Exception) { Typeface.create("sans-serif", Typeface.BOLD) }
    }

    private fun getRobotoBold(context: Context): Typeface {
        robotoBoldTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/vivo/fonts/Roboto-Bold.ttf").also {
                robotoBoldTypeface = it
            }
        } catch (_: Exception) { Typeface.create("sans-serif", Typeface.BOLD) }
    }

    /** typeface 9 in templates — vivotypeSimple-Bold, used by newer iQOO/common watermarks. */
    private fun getVivoTypeSimpleBold(context: Context): Typeface {
        vivoTypeSimpleBoldTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/vivo/fonts/vivotypeSimple-Bold.ttf").also {
                vivoTypeSimpleBoldTypeface = it
            }
        } catch (_: Exception) { getIqooBold(context) }
    }

    private fun loadVivoLogo(context: Context, name: String): Bitmap? {
        return try {
            context.assets.open("watermark/vivo/logos/$name").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) { null }
    }

    /**
     * Create a Paint with a specific font weight using variable font support (API 28+).
     */
    private fun createWeightedPaint(typeface: Typeface, weight: Int): Paint {
        val tf = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            Typeface.create(typeface, weight, false)
        } else {
            typeface
        }
        return Paint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = tf }
    }

    /**
     * Draw a date string with separator characters (/, ., -) in an accent color.
     * Used by Sonnar watermark for the distinctive pink-accent date style.
     * Text is drawn right-aligned from [rightX].
     */
    private fun drawAccentedDate(
        canvas: Canvas, text: String, rightX: Float, y: Float,
        paint: Paint, accentColor: Int
    ) {
        val baseColor = paint.color
        // Split into segments: normal text vs separator chars
        val segments = mutableListOf<Pair<String, Boolean>>() // (text, isAccent)
        val sb = StringBuilder()
        for (c in text) {
            if (c == '/' || c == '.' || c == '-') {
                if (sb.isNotEmpty()) {
                    segments.add(sb.toString() to false)
                    sb.clear()
                }
                segments.add(c.toString() to true)
            } else {
                sb.append(c)
            }
        }
        if (sb.isNotEmpty()) segments.add(sb.toString() to false)

        // Measure total width
        var totalW = 0f
        for ((seg, _) in segments) totalW += paint.measureText(seg)

        // Draw from left, offset so right edge aligns at rightX
        val savedAlign = paint.textAlign
        paint.textAlign = Paint.Align.LEFT
        var x = rightX - totalW
        for ((seg, isAccent) in segments) {
            paint.color = if (isAccent) accentColor else baseColor
            canvas.drawText(seg, x, y, paint)
            x += paint.measureText(seg)
        }
        paint.textAlign = savedAlign
        paint.color = baseColor
    }

    // ---- ZEISS ----
    /**
     * ZEISS Professional white-bar watermark.
     * Based on zeiss7.txt template (newest generation):
     *
     *   LEFT:  [vivo logo] [Device Name] [│ divider] [ZEISS logo]
     *   RIGHT: [3A camera info (lensInfo)]           ← top line, #CC000000
     *          [datetime  location]                  ← bottom line, #757575
     *
     * Template: 1080×1719, bar=324px (108dp), marginStart=14.7, marginEnd=15.5
     */
    private fun applyVivoZeiss(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val shortSide = min(imgW, imgH).toFloat()

        val barHF = maxOf(shortSide * VIVO_BAR_RATIO, 80f)
        val barH = rint(barHF)
        val dp = barHF / VIVO_BAR_DP  // 1 template dp in pixels

        val totalH = imgH + barH
        val result = Bitmap.createBitmap(imgW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        // White bar
        val barPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        canvas.drawRect(0f, imgH.toFloat(), imgW.toFloat(), totalH.toFloat(), barPaint)

        val barTop = imgH.toFloat()
        val barCY = barTop + barHF / 2f
        val marginL = 14.7f * dp
        val marginR = 15.5f * dp

        // === LEFT GROUP (center_vertical) ===
        // 1. vivo logo (vivo_logo_special.png — already dark for white bar, per zeiss7 template)
        var curX = marginL
        val vivoLogo = loadVivoLogo(context, "vivo_logo_special.png")
            ?: loadVivoLogo(context, "vivo_logo_wm_xml.png")
        vivoLogo?.let {
            val logoH = 13.7f * dp
            val logoW = logoH * it.width / it.height
            val logoY = barCY - logoH / 2f
            val logoRect = rectF(curX, logoY, curX + logoW, logoY + logoH)
            // vivo_logo_special.png is designed for white bar; use SRC_IN to ensure black
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = android.graphics.PorterDuffColorFilter(
                    Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            canvas.drawBitmap(it, null, logoRect, tintPaint)
            curX += logoW + 3.9f * dp  // gap to model text (template: 54.5 - 50.6 = 3.9dp)
            it.recycle()
        }

        // 2. Device/model name — match VIVO_CLASSIC: heavy/bold font
        val deviceText = config.deviceName ?: ""
        val modelPaint = createWeightedPaint(getVivoHeavy(context), 800).apply {
            textSize = 15.3f * dp
            color = Color.BLACK
            textAlign = Paint.Align.LEFT
        }
        if (deviceText.isNotEmpty()) {
            val modelY = barCY - (modelPaint.ascent() + modelPaint.descent()) / 2f
            canvas.drawText(deviceText, curX, modelY, modelPaint)
            curX += modelPaint.measureText(deviceText) + 7.5f * dp
        }

        // 3. Thin black divider (1dp wide)
        val divW = maxOf(1f, dp)
        val divTop = barCY - 5.7f * dp   // ≈ (32.7-21.3)/2 dp from center
        val divBot = barCY + 5.7f * dp
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawRect(curX, divTop, curX + divW, divBot, divPaint)
        curX += divW

        // 4. ZEISS logo (39×41 dp, nearly square)
        val zeissLogo = loadVivoLogo(context, "zeiss_logo_special.png")
            ?: loadVivoLogo(context, "zeiss_logo.png")
        zeissLogo?.let {
            val zH = 41f * dp
            val zW = 39f * dp
            val zY = barCY - zH / 2f
            val zRect = rectF(curX, zY, curX + zW, zY + zH)
            canvas.drawBitmap(it, null, zRect,
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            it.recycle()
        }

        // === RIGHT GROUP (center_vertical, right-aligned) ===
        val rightX = imgW - marginR
        val lensText = config.lensInfo ?: ""
        val timeText = config.timeText ?: ""
        val locText = config.locationText ?: ""

        // Line 1: 3A camera info — match VIVO_CLASSIC: zeiss bold secondary font
        val infoPaint = createWeightedPaint(getZeissBold(context), 600).apply {
            textSize = 9.7f * dp
            color = VIVO_3A_ZEISS
            textAlign = Paint.Align.RIGHT
        }

        // Line 2: datetime + location — match VIVO_CLASSIC: zeiss bold secondary font
        val timePaint = createWeightedPaint(getZeissBold(context), 550).apply {
            textSize = 7.5f * dp
            color = VIVO_TIME_GRAY
            textAlign = Paint.Align.RIGHT
        }
        val locPaint = createWeightedPaint(getZeissBold(context), 500).apply {
            textSize = 7.3f * dp
            color = VIVO_TIME_GRAY
            textAlign = Paint.Align.RIGHT
        }

        val hasInfo = lensText.isNotEmpty()
        val hasTime = timeText.isNotEmpty()
        val hasLoc = locText.isNotEmpty()
        val hasBottom = hasTime || hasLoc

        if (hasInfo && hasBottom) {
            // Two-line stack, vertically centered
            val infoH = infoPaint.descent() - infoPaint.ascent()
            val botH = timePaint.descent() - timePaint.ascent()
            val lineGap = 1.5f * dp
            val stackH = infoH + lineGap + botH
            val topY = barCY - stackH / 2f

            canvas.drawText(lensText, rightX, topY - infoPaint.ascent(), infoPaint)

            // Bottom line: datetime then location (or just one)
            val botY = topY + infoH + lineGap - timePaint.ascent()
            if (hasTime && hasLoc) {
                // Location at right edge, datetime to its left
                canvas.drawText(locText, rightX, botY, locPaint)
                val locW = locPaint.measureText(locText)
                val gap = 4f * dp
                canvas.drawText(timeText, rightX - locW - gap, botY, timePaint)
            } else if (hasTime) {
                canvas.drawText(timeText, rightX, botY, timePaint)
            } else {
                canvas.drawText(locText, rightX, botY, locPaint)
            }
        } else if (hasInfo) {
            // Single line: 3A info centered vertically
            val y = barCY - (infoPaint.ascent() + infoPaint.descent()) / 2f
            canvas.drawText(lensText, rightX, y, infoPaint)
        } else if (hasBottom) {
            // Single line: datetime/location centered vertically
            val p = if (hasTime) timePaint else locPaint
            val text = if (hasTime) timeText else locText
            val y = barCY - (p.ascent() + p.descent()) / 2f
            canvas.drawText(text, rightX, y, p)
        }

        return result
    }

    // ---- vivo Classic / Overlay ----
    /**
     * Overlay watermark — white text with shadow on the photo, no bar.
     *
     * Bottom-left layout:
     *   Line 1: [vivo logo] [Device Name]  (white, bold, with shadow)
     *   Line 2: [lens info  |  datetime]   (white, lighter weight)
     */
    private fun applyVivoClassic(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val s = min(imgW, imgH).toFloat()

        val marginLR = maxOf(s * VIVO_OV_MARGIN_LR, 20f)
        val marginBot = maxOf(imgH * VIVO_OV_MARGIN_BOT, 40f)
        val fsDev = maxOf(s * VIVO_OV_FS_DEVICE, 16f)
        val fsSub = maxOf(s * VIVO_OV_FS_SUB, 12f)

        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val shadowR = maxOf(4f, s / 300f)
        val shadowOff = maxOf(1f, s / 1200f)

        // --- Line 2 (bottom): Lens info | Time ---
        val lensText = config.lensInfo ?: ""
        val timeText = config.timeText ?: ""
        val line2Text = when {
            lensText.isNotEmpty() && timeText.isNotEmpty() -> "$lensText  |  $timeText"
            lensText.isNotEmpty() -> lensText
            else -> timeText
        }

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getZeissBold(context)
            textSize = fsSub
            color = Color.argb(220, 255, 255, 255)
            textAlign = Paint.Align.LEFT
            setShadowLayer(shadowR, shadowOff, shadowOff, Color.argb(100, 0, 0, 0))
        }

        val line2Y = imgH - marginBot
        if (line2Text.isNotEmpty()) {
            canvas.drawText(line2Text, marginLR, line2Y, subPaint)
        }

        // --- Line 1 (above): Logo + Device name ---
        val deviceText = config.deviceName ?: ""
        val devPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = getVivoHeavy(context)
            textSize = fsDev
            color = Color.argb(250, 255, 255, 255)
            textAlign = Paint.Align.LEFT
            setShadowLayer(shadowR, shadowOff, shadowOff, Color.argb(100, 0, 0, 0))
        }

        val l2H = subPaint.descent() - subPaint.ascent()
        val lgap = maxOf(fsSub * 0.4f, 3f)
        val line1Y = line2Y - l2H - lgap

        // Logo (white/shadow version for dark backgrounds)
        var textX = marginLR
        val logoSize = maxOf(rint(fsDev * 1.1f), 16)
        val logo = loadVivoLogo(context, "vivo_logo_shadow_wm_xml.webp")
            ?: loadVivoLogo(context, "vivo_logo_wm_xml.png")
        logo?.let {
            val logoW = logoSize.toFloat() * it.width / it.height
            val logoY = line1Y - logoSize
            val logoRect = rectF(marginLR, logoY, marginLR + logoW, logoY + logoSize)
            canvas.drawBitmap(it, null, logoRect,
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            textX = marginLR + logoW + maxOf(fsDev * 0.3f, 4f)
            it.recycle()
        }

        if (deviceText.isNotEmpty()) {
            canvas.drawText(deviceText, textX, line1Y, devPaint)
        }

        return result
    }

    // ---- vivo Pro ----
    /**
     * vivo branded white-bar watermark (non-ZEISS).
     * Based on vivo1.txt template:
     *
     *   LEFT:  [vivo logo] [Device Name]
     *   RIGHT: [3A camera info]              ← top line, #666666
     *          [datetime | location]         ← bottom line, #757575
     *
     * Template: marginStart=13, marginEnd=12, model typeface=3 size=13 weight=700
     *           3A typeface=5 size=9 weight=400 #666666
     *           datetime/location typeface=0 size=7 weight=450 #757575
     */
    private fun applyVivoPro(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val shortSide = min(imgW, imgH).toFloat()

        val barHF = maxOf(shortSide * VIVO_BAR_RATIO, 80f)
        val barH = rint(barHF)
        val dp = barHF / VIVO_BAR_DP

        val totalH = imgH + barH
        val result = Bitmap.createBitmap(imgW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        // White bar
        val barPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        canvas.drawRect(0f, imgH.toFloat(), imgW.toFloat(), totalH.toFloat(), barPaint)

        val barTop = imgH.toFloat()
        val barCY = barTop + barHF / 2f
        val marginL = 13f * dp
        val marginR = 12f * dp

        // === LEFT: vivo logo + device name ===
        var curX = marginL
        // vivo1.txt: vivo_logo_new.png with issvg=true (tint to match bar)
        val vivoLogo = loadVivoLogo(context, "vivo_logo_new.png")
            ?: loadVivoLogo(context, "vivo_logo_wm_xml.png")
        vivoLogo?.let {
            val logoH = 11f * dp   // logo from template: (13,25)-(55,36) → h=11dp
            val logoW = logoH * it.width / it.height
            val logoY = barCY - logoH / 2f
            val logoRect = rectF(curX, logoY, curX + logoW, logoY + logoH)
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = android.graphics.PorterDuffColorFilter(
                    Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            canvas.drawBitmap(it, null, logoRect, tintPaint)
            curX += logoW  // logo end at x=55 in template, text starts at x=55 too
            it.recycle()
        }

        val deviceText = config.deviceName ?: ""
        // typeface 3 = vivo-Regular
        val modelPaint = createWeightedPaint(getVivoRegular(context), 700).apply {
            textSize = 13f * dp
            color = Color.BLACK
            textAlign = Paint.Align.LEFT
        }
        if (deviceText.isNotEmpty()) {
            val modelY = barCY - (modelPaint.ascent() + modelPaint.descent()) / 2f
            canvas.drawText(deviceText, curX, modelY, modelPaint)
        }

        // === RIGHT: 3A info (top) + datetime | location (bottom) ===
        val rightX = imgW - marginR
        val lensText = config.lensInfo ?: ""
        val timeText = config.timeText ?: ""
        val locText = config.locationText ?: ""

        // 3A text (typeface 5 = Roboto-Bold, size 9, weight 400, #666666)
        val infoPaint = createWeightedPaint(getRobotoBold(context), 400).apply {
            textSize = 9f * dp
            color = VIVO_3A_STD
            textAlign = Paint.Align.RIGHT
        }

        // datetime/location (typeface 0 = Roboto, size 7, weight 450, #757575)
        val timePaint = createWeightedPaint(getRobotoBold(context), 450).apply {
            textSize = 7f * dp
            color = VIVO_TIME_GRAY
            textAlign = Paint.Align.RIGHT
        }

        val hasInfo = lensText.isNotEmpty()
        val hasTime = timeText.isNotEmpty()
        val hasLoc = locText.isNotEmpty()
        val hasBottom = hasTime || hasLoc

        if (hasInfo && hasBottom) {
            val infoH = infoPaint.descent() - infoPaint.ascent()
            val botH = timePaint.descent() - timePaint.ascent()
            val lineGap = 4f * dp
            val stackH = infoH + lineGap + botH
            val topY = barCY - stackH / 2f

            canvas.drawText(lensText, rightX, topY - infoPaint.ascent(), infoPaint)

            val botY = topY + infoH + lineGap - timePaint.ascent()
            if (hasTime && hasLoc) {
                canvas.drawText(locText, rightX, botY, timePaint)
                val locW = timePaint.measureText(locText)
                // Thin gray divider between datetime and location
                val divGap = 1f * dp
                val divX = rightX - locW - divGap
                val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = VIVO_TIME_GRAY; strokeWidth = maxOf(1f, dp * 0.33f)
                }
                canvas.drawLine(divX, botY + timePaint.ascent() * 0.9f,
                    divX, botY + timePaint.descent() * 0.9f, divPaint)
                canvas.drawText(timeText, divX - divGap, botY, timePaint)
            } else if (hasTime) {
                canvas.drawText(timeText, rightX, botY, timePaint)
            } else {
                canvas.drawText(locText, rightX, botY, timePaint)
            }
        } else if (hasInfo) {
            val y = barCY - (infoPaint.ascent() + infoPaint.descent()) / 2f
            canvas.drawText(lensText, rightX, y, infoPaint)
        } else if (hasBottom) {
            val text = if (hasTime) timeText else locText
            val y = barCY - (timePaint.ascent() + timePaint.descent()) / 2f
            canvas.drawText(text, rightX, y, timePaint)
        }

        return result
    }

    // ---- iQOO ----
    /**
     * iQOO branded white-bar watermark.
     * Based on iqoo7.txt template (newest generation):
     *
     *   LEFT:  [iQOO logo] [Device Name]
     *   RIGHT: [3A camera info]              ← top line, #666666
     *          [datetime  location]          ← bottom line, #757575
     *
     * Template: marginStart=16, marginEnd=14, model typeface=9 size=15.3 weight=700
     *           3A typeface=7 size=9.7 weight=600 #FF666666
     *           datetime typeface=7 size=7.5 weight=550 #757575
     */
    private fun applyVivoIqoo(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val shortSide = min(imgW, imgH).toFloat()

        val barHF = maxOf(shortSide * VIVO_BAR_RATIO, 80f)
        val barH = rint(barHF)
        val dp = barHF / VIVO_BAR_DP

        val totalH = imgH + barH
        val result = Bitmap.createBitmap(imgW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        // White bar
        val barPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        canvas.drawRect(0f, imgH.toFloat(), imgW.toFloat(), totalH.toFloat(), barPaint)

        val barTop = imgH.toFloat()
        val barCY = barTop + barHF / 2f
        val marginL = 16f * dp
        val marginR = 14f * dp

        // === LEFT: iQOO logo + device name ===
        var curX = marginL
        val iqooLogo = loadVivoLogo(context, "iqoo_logo_special_white.png")
            ?: loadVivoLogo(context, "iqoo_logo_wm_xml.png")
        iqooLogo?.let {
            val logoH = 12.3f * dp   // from template: (11,17)-(54,29.3) → h=12.3dp
            val logoW = logoH * it.width / it.height
            val logoY = barCY - logoH / 2f
            val logoRect = rectF(curX, logoY, curX + logoW, logoY + logoH)
            // Logo is white on transparent; tint to black for white bar
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = android.graphics.PorterDuffColorFilter(
                    Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            canvas.drawBitmap(it, null, logoRect, tintPaint)
            curX += logoW + 3.5f * dp  // gap: template 57.5 - 54 = 3.5dp
            it.recycle()
        }

        val deviceText = config.deviceName ?: ""
        // typeface=9 (vivotypeSimple-Bold), size=15.3dp, weight=700 per iqoo7.txt
        val modelPaint = createWeightedPaint(getVivoTypeSimpleBold(context), 700).apply {
            textSize = 15.3f * dp
            color = Color.BLACK
            textAlign = Paint.Align.LEFT
        }
        if (deviceText.isNotEmpty()) {
            val modelY = barCY - (modelPaint.ascent() + modelPaint.descent()) / 2f
            canvas.drawText(deviceText, curX, modelY, modelPaint)
        }

        // === RIGHT: 3A info (top) + datetime/location (bottom) ===
        val rightX = imgW - marginR
        val lensText = config.lensInfo ?: ""
        val timeText = config.timeText ?: ""
        val locText = config.locationText ?: ""

        // 3A text (typeface 7 = vivoSansExpVF, size 9.7, weight 600, #666666)
        val infoPaint = createWeightedPaint(getVivoSansExp(context), 600).apply {
            textSize = 9.7f * dp
            color = VIVO_3A_STD
            textAlign = Paint.Align.RIGHT
        }

        // datetime (typeface 7, size 7.5, weight 550, #757575)
        val timePaint = createWeightedPaint(getVivoSansExp(context), 550).apply {
            textSize = 7.5f * dp
            color = VIVO_TIME_GRAY
            textAlign = Paint.Align.RIGHT
        }

        // location (typeface 7, size 7.3, weight 500, #757575)
        val locPaint = createWeightedPaint(getVivoSansExp(context), 500).apply {
            textSize = 7.3f * dp
            color = VIVO_TIME_GRAY
            textAlign = Paint.Align.RIGHT
        }

        val hasInfo = lensText.isNotEmpty()
        val hasTime = timeText.isNotEmpty()
        val hasLoc = locText.isNotEmpty()
        val hasBottom = hasTime || hasLoc

        if (hasInfo && hasBottom) {
            val infoH = infoPaint.descent() - infoPaint.ascent()
            val botH = timePaint.descent() - timePaint.ascent()
            val lineGap = 2.3f * dp
            val stackH = infoH + lineGap + botH
            val topY = barCY - stackH / 2f

            canvas.drawText(lensText, rightX, topY - infoPaint.ascent(), infoPaint)

            val botY = topY + infoH + lineGap - timePaint.ascent()
            if (hasTime && hasLoc) {
                canvas.drawText(locText, rightX, botY, locPaint)
                val locW = locPaint.measureText(locText)
                val gap = 4f * dp
                canvas.drawText(timeText, rightX - locW - gap, botY, timePaint)
            } else if (hasTime) {
                canvas.drawText(timeText, rightX, botY, timePaint)
            } else {
                canvas.drawText(locText, rightX, botY, locPaint)
            }
        } else if (hasInfo) {
            val y = barCY - (infoPaint.ascent() + infoPaint.descent()) / 2f
            canvas.drawText(lensText, rightX, y, infoPaint)
        } else if (hasBottom) {
            val p = if (hasTime) timePaint else locPaint
            val text = if (hasTime) timeText else locText
            val y = barCY - (p.ascent() + p.descent()) / 2f
            canvas.drawText(text, rightX, y, p)
        }

        return result
    }

    // ---- Generic helpers for fixed-frame (isfixed=true) watermarks ----

    /**
     * Load a baseboard frame image and composite the photo into it.
     * The photo is scaled to fill the photo region defined by (pathL,pathT,pathR,pathB)
     * within the template of dimensions (tmplW × tmplH).
     * Returns the result bitmap and canvas, already composited.
     */
    private fun buildFrameComposite(
        context: Context, source: Bitmap,
        frameName: String,
        tmplW: Int, tmplH: Int,
        pathL: Int, pathT: Int, pathR: Int, pathB: Int
    ): Pair<Bitmap, Canvas>? {
        val frameBmp = try {
            context.assets.open("watermark/vivo/frames/$frameName").use {
                BitmapFactory.decodeStream(it)
            }
        } catch (_: Exception) { return null }

        val photoW = pathR - pathL   // photo area width in template px
        val photoH = pathB - pathT   // photo area height in template px
        val photoAR = photoW.toFloat() / photoH  // frame photo-area aspect ratio
        val sourceAR = source.width.toFloat() / source.height

        // Center-crop source to match the frame's photo-area aspect ratio
        val srcRect: Rect
        val cropW: Int
        val cropH: Int
        if (sourceAR > photoAR + 0.01f) {
            // Source wider → crop left/right
            cropW = (source.height * photoAR).toInt()
            cropH = source.height
            val x = (source.width - cropW) / 2
            srcRect = Rect(x, 0, x + cropW, cropH)
        } else if (sourceAR < photoAR - 0.01f) {
            // Source taller → crop top/bottom
            cropW = source.width
            cropH = (source.width / photoAR).toInt()
            val y = (source.height - cropH) / 2
            srcRect = Rect(0, y, cropW, y + cropH)
        } else {
            cropW = source.width
            cropH = source.height
            srcRect = Rect(0, 0, cropW, cropH)
        }

        // Scale: map cropped source pixels → template photo-area pixels → output
        val realScale = cropW.toFloat() / photoW
        val outW = rint(tmplW * realScale)
        val outH = rint(tmplH * realScale)

        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw the frame baseboard, scaled to fill output
        canvas.drawBitmap(frameBmp, null, Rect(0, 0, outW, outH),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        frameBmp.recycle()

        // Draw the cropped source photo into the frame's photo region
        val dstL = rint(pathL * realScale)
        val dstT = rint(pathT * realScale)
        val dstR = rint(pathR * realScale)
        val dstB = rint(pathB * realScale)
        canvas.drawBitmap(source, srcRect, Rect(dstL, dstT, dstR, dstB),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        return Pair(result, canvas)
    }

    // ---- ZEISS V1 (zeiss1) ----
    /**
     * Older ZEISS bar watermark.
     * LEFT:  [vivo logo] [Device Name](typeface3,14dp,w600) [│] [ZEISS logo]
     * RIGHT: 4 separate 3A params (typeface5,9dp,w400-700,#666666) → top line
     *        datetime + | + location (typeface5,6dp,#757575) → bottom line
     */
    private fun applyVivoZeissV1(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val shortSide = min(imgW, imgH).toFloat()
        val barHF = maxOf(shortSide * VIVO_BAR_RATIO, 80f)
        val barH = rint(barHF)
        val dp = barHF / VIVO_BAR_DP

        val totalH = imgH + barH
        val result = Bitmap.createBitmap(imgW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        canvas.drawRect(0f, imgH.toFloat(), imgW.toFloat(), totalH.toFloat(),
            Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })

        val barTop = imgH.toFloat()
        val barCY = barTop + barHF / 2f
        val marginL = 11f * dp
        val marginR = 11f * dp

        // LEFT: vivo logo + model + divider + ZEISS logo
        var curX = marginL
        loadVivoLogo(context, "vivo_logo.png")?.let {
            val logoH = 11f * dp
            val logoW = logoH * it.width / it.height
            val logoY = barCY - logoH / 2f
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = android.graphics.PorterDuffColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN)
            }
            canvas.drawBitmap(it, null, rectF(curX, logoY, curX + logoW, logoY + logoH), tintPaint)
            curX += logoW + 1f * dp
            it.recycle()
        }

        val deviceText = config.deviceName ?: ""
        // typeface 3 = vivo-Regular (common1.txt uses typeface3, but zeiss1 variant uses typeface8→vivoCamera)
        val modelPaint = createWeightedPaint(getVivoRegular(context), 600).apply {
            textSize = 14f * dp; color = Color.BLACK; textAlign = Paint.Align.LEFT
        }
        if (deviceText.isNotEmpty()) {
            canvas.drawText(deviceText, curX, barCY - (modelPaint.ascent() + modelPaint.descent()) / 2f, modelPaint)
            curX += modelPaint.measureText(deviceText) + 7f * dp
        }

        // Divider
        val divW = maxOf(1f, dp * 0.3f)
        canvas.drawRect(curX, barCY - 5f * dp, curX + divW, barCY + 5f * dp,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL })
        curX += divW

        // ZEISS logo — template: (170,8)-(208,46) = 38×38dp
        loadVivoLogo(context, "zeiss_logo.png")?.let {
            val zH = 38f * dp; val zW = 38f * dp
            val zY = barCY - zH / 2f
            canvas.drawBitmap(it, null, rectF(curX, zY, curX + zW, zY + zH),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            it.recycle()
        }

        // RIGHT: 3A info (4 separate params) + datetime/location
        val rightX = imgW - marginR
        val lensText = config.lensInfo ?: ""
        val timeText = config.timeText ?: ""
        val locText = config.locationText ?: ""

        // typeface 5 = Roboto-Bold
        val infoPaint = createWeightedPaint(getRobotoBold(context), 500).apply {
            textSize = 9f * dp; color = VIVO_3A_STD; textAlign = Paint.Align.RIGHT
        }
        // typeface 0 = Roboto-Bold, size 7dp, weight 450
        val timePaint = createWeightedPaint(getRobotoBold(context), 450).apply {
            textSize = 7f * dp; color = VIVO_TIME_GRAY; textAlign = Paint.Align.RIGHT
        }

        val hasInfo = lensText.isNotEmpty()
        val hasTime = timeText.isNotEmpty()
        val hasLoc = locText.isNotEmpty()
        val hasBottom = hasTime || hasLoc

        if (hasInfo && hasBottom) {
            val infoH = infoPaint.descent() - infoPaint.ascent()
            val botH = timePaint.descent() - timePaint.ascent()
            val lineGap = 4f * dp
            val stackH = infoH + lineGap + botH
            val topY = barCY - stackH / 2f
            canvas.drawText(lensText, rightX, topY - infoPaint.ascent(), infoPaint)
            val botY = topY + infoH + lineGap - timePaint.ascent()
            if (hasTime && hasLoc) {
                canvas.drawText(locText, rightX, botY, timePaint)
                val locW = timePaint.measureText(locText)
                val divGap = 1f * dp
                val divX = rightX - locW - divGap
                canvas.drawLine(divX, botY + timePaint.ascent() * 0.9f, divX, botY + timePaint.descent() * 0.9f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = VIVO_TIME_GRAY; strokeWidth = maxOf(1f, dp * 0.33f) })
                canvas.drawText(timeText, divX - divGap, botY, timePaint)
            } else {
                canvas.drawText(if (hasTime) timeText else locText, rightX, botY, timePaint)
            }
        } else if (hasInfo) {
            canvas.drawText(lensText, rightX, barCY - (infoPaint.ascent() + infoPaint.descent()) / 2f, infoPaint)
        } else if (hasBottom) {
            val t = if (hasTime) timeText else locText
            canvas.drawText(t, rightX, barCY - (timePaint.ascent() + timePaint.descent()) / 2f, timePaint)
        }

        return result
    }

    // ---- ZEISS Sonnar (zeiss3) ----
    /**
     * ZEISS Sonnar bar watermark.
     * LEFT large ZEISS logo (46dp) + below: model|ZEISS (typeface4,14dp) + 3A (typeface1,8dp,#757575)
     * RIGHT: date (12dp, #2A3844 with pink accent) + location below
     */
    private fun applyVivoZeissSonnar(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val shortSide = min(imgW, imgH).toFloat()
        val barHF = maxOf(shortSide * VIVO_BAR_RATIO, 80f)
        val barH = rint(barHF)
        val dp = barHF / VIVO_BAR_DP

        val totalH = imgH + barH
        val result = Bitmap.createBitmap(imgW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        canvas.drawRect(0f, imgH.toFloat(), imgW.toFloat(), totalH.toFloat(),
            Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })

        val barTop = imgH.toFloat()
        val barCY = barTop + barHF / 2f
        val marginL = 3f * dp
        val marginR = 13f * dp

        // LEFT: big ZEISS logo
        var curX = marginL
        loadVivoLogo(context, "zeiss_logo.png")?.let {
            val zH = 46f * dp
            val zW = zH * it.width / it.height
            val zY = barCY - zH / 2f
            canvas.drawBitmap(it, null, rectF(curX, zY, curX + zW, zY + zH),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            curX += zW
            it.recycle()
        }

        // Model | ZEISS text (starting after ZEISS logo at x=50dp per template)
        val textStartX = 50f * dp
        val deviceText = config.deviceName ?: ""
        // typeface=4 (ZEISSFrutigerNextW1G-Bold), size=14dp, weight=700
        val modelPaint = createWeightedPaint(getZeissBold(context), 700).apply {
            textSize = 14f * dp; color = Color.BLACK; textAlign = Paint.Align.LEFT
        }
        val lensText = config.lensInfo ?: ""

        // Top sub-row: model [|divider|] ZEISS — 2dp margin above (linemarginbottom=2)
        val modelRowY = barCY - 8f * dp  // above center
        if (deviceText.isNotEmpty()) {
            canvas.drawText(deviceText, textStartX, modelRowY - (modelPaint.ascent() + modelPaint.descent()) / 2f, modelPaint)
            val mw = modelPaint.measureText(deviceText)
            // Thin divider: template (139,17)-(141,26) = 2×9dp
            val divX = textStartX + mw + 5f * dp
            val divW = maxOf(1f, 2f * dp)
            canvas.drawRect(divX, modelRowY - 4.5f * dp, divX + divW, modelRowY + 4.5f * dp,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL })
            val zeissText = "ZEISS"
            val zeissPaint = createWeightedPaint(getZeissBold(context), 700).apply {
                textSize = 14f * dp; color = Color.BLACK; textAlign = Paint.Align.LEFT
            }
            canvas.drawText(zeissText, divX + divW + 5f * dp,
                modelRowY - (zeissPaint.ascent() + zeissPaint.descent()) / 2f, zeissPaint)
        }

        // Bottom sub-row: 3A params (typeface=1 = vivoCameraVF, 8dp, w550, #757575)
        val threaPaint = createWeightedPaint(getVivoCamera(context), 550).apply {
            textSize = 8f * dp; color = VIVO_TIME_GRAY; textAlign = Paint.Align.LEFT
        }
        if (lensText.isNotEmpty()) {
            val threeAY = barCY + 8f * dp
            canvas.drawText(lensText, textStartX, threeAY - (threaPaint.ascent() + threaPaint.descent()) / 2f, threaPaint)
        }

        // RIGHT: date (colored with pink accent separators) + location
        val rightX = imgW - marginR
        val timeText = config.timeText ?: ""
        val locText = config.locationText ?: ""
        val dateColor = Color.parseColor("#2A3844")
        val accentColor = Color.parseColor("#fd5471")

        val datePaint = createWeightedPaint(getVivoSansExp(context), 600).apply {
            textSize = 12f * dp; color = dateColor; textAlign = Paint.Align.RIGHT; letterSpacing = 0.05f
        }
        val locPaint2 = createWeightedPaint(getVivoSansExp(context), 600).apply {
            textSize = 8f * dp; color = VIVO_TIME_GRAY; textAlign = Paint.Align.RIGHT
        }

        val hasTime = timeText.isNotEmpty()
        val hasLoc = locText.isNotEmpty()
        if (hasTime && hasLoc) {
            val dateH = datePaint.descent() - datePaint.ascent()
            val locH = locPaint2.descent() - locPaint2.ascent()
            val gap = 3f * dp
            val stackH = dateH + gap + locH
            val topY = barCY - stackH / 2f
            drawAccentedDate(canvas, timeText, rightX, topY - datePaint.ascent(), datePaint, accentColor)
            canvas.drawText(locText, rightX, topY + dateH + gap - locPaint2.ascent(), locPaint2)
        } else if (hasTime) {
            drawAccentedDate(canvas, timeText, rightX,
                barCY - (datePaint.ascent() + datePaint.descent()) / 2f, datePaint, accentColor)
        } else if (hasLoc) {
            canvas.drawText(locText, rightX, barCY - (locPaint2.ascent() + locPaint2.descent()) / 2f, locPaint2)
        }

        return result
    }

    // ---- ZEISS Humanity (zeiss8) ----
    /**
     * Minimalist bar: just "model | ZEISS" or "ZEISS" text.
     * typeface7 (vivoSansExpVF), 22.6dp, weight 500, #666666
     */
    private fun applyVivoZeissHumanity(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val shortSide = min(imgW, imgH).toFloat()
        val barHF = maxOf(shortSide * VIVO_BAR_RATIO, 80f)
        val barH = rint(barHF)
        val dp = barHF / VIVO_BAR_DP

        val totalH = imgH + barH
        val result = Bitmap.createBitmap(imgW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        canvas.drawRect(0f, imgH.toFloat(), imgW.toFloat(), totalH.toFloat(),
            Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })

        val barTop = imgH.toFloat()
        val barCY = barTop + barHF / 2f
        val marginL = 22f * dp

        val textPaint = createWeightedPaint(getVivoSansExp(context), 500).apply {
            textSize = 22.6f * dp; color = VIVO_3A_STD; textAlign = Paint.Align.LEFT
        }

        val deviceText = config.deviceName ?: ""
        val displayText = if (deviceText.isNotEmpty()) {
            deviceText
        } else {
            "ZEISS"
        }
        val y = barCY - (textPaint.ascent() + textPaint.descent()) / 2f

        if (deviceText.isNotEmpty()) {
            // Draw "model" then wide divider then "ZEISS" (zeiss8 subgroup 1)
            canvas.drawText(deviceText, marginL, y, textPaint)
            val mw = textPaint.measureText(deviceText)
            val divX = marginL + mw + 7f * dp
            // divider_wide_black: template (324,33)-(328,52) = 4×19dp
            val divW = maxOf(2f, 4f * dp)
            val divHalf = 9.5f * dp
            canvas.drawRect(divX, barCY - divHalf, divX + divW, barCY + divHalf,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL })
            canvas.drawText("ZEISS", divX + divW + 7f * dp, y, textPaint)
        } else {
            canvas.drawText("ZEISS", marginL, y, textPaint)
        }

        return result
    }

    // ---- iQOO V1 (iqoo1) ----
    /**
     * Older iQOO bar watermark.
     * LEFT: iqoo_logo + model (typeface6/IQOOTYPE-Bold, 13dp, w500, letterSpacing 0.05)
     * RIGHT: 4 separate 3A (typeface0/Roboto, 10dp, w700, #666666) + datetime | location
     */
    private fun applyVivoIqooV1(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val shortSide = min(imgW, imgH).toFloat()
        val barHF = maxOf(shortSide * VIVO_BAR_RATIO, 80f)
        val barH = rint(barHF)
        val dp = barHF / VIVO_BAR_DP

        val totalH = imgH + barH
        val result = Bitmap.createBitmap(imgW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        canvas.drawRect(0f, imgH.toFloat(), imgW.toFloat(), totalH.toFloat(),
            Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })

        val barTop = imgH.toFloat()
        val barCY = barTop + barHF / 2f
        val marginL = 13f * dp
        val marginR = 12f * dp

        // LEFT: iQOO logo + model (iqoo1.txt)
        var curX = marginL
        loadVivoLogo(context, "iqoo_logo.png")?.let {
            val logoH = 17f * dp  // template: (11,17)-(54,34) → h=17dp
            val logoW = logoH * it.width / it.height
            val logoY = barCY - logoH / 2f
            // Tint to black for white bar
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = android.graphics.PorterDuffColorFilter(
                    Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            canvas.drawBitmap(it, null, rectF(curX, logoY, curX + logoW, logoY + logoH), tintPaint)
            curX += logoW + 1f * dp
            it.recycle()
        }

        val deviceText = config.deviceName ?: ""
        val modelPaint = createWeightedPaint(getIqooBold(context), 500).apply {
            textSize = 13f * dp; color = Color.BLACK; textAlign = Paint.Align.LEFT; letterSpacing = 0.05f
        }
        if (deviceText.isNotEmpty()) {
            canvas.drawText(deviceText, curX, barCY - (modelPaint.ascent() + modelPaint.descent()) / 2f, modelPaint)
        }

        // RIGHT: 3A info (Roboto Bold, 10dp, w700, #666666) + datetime/location
        val rightX = imgW - marginR
        val lensText = config.lensInfo ?: ""
        val timeText = config.timeText ?: ""
        val locText = config.locationText ?: ""

        val infoPaint = createWeightedPaint(getRobotoBold(context), 700).apply {
            textSize = 10f * dp; color = VIVO_3A_STD; textAlign = Paint.Align.RIGHT
        }
        val timePaint = createWeightedPaint(getRobotoBold(context), 550).apply {
            textSize = 7f * dp; color = VIVO_TIME_GRAY; textAlign = Paint.Align.RIGHT
        }

        val hasInfo = lensText.isNotEmpty()
        val hasTime = timeText.isNotEmpty()
        val hasLoc = locText.isNotEmpty()
        val hasBottom = hasTime || hasLoc

        if (hasInfo && hasBottom) {
            val infoH = infoPaint.descent() - infoPaint.ascent()
            val botH = timePaint.descent() - timePaint.ascent()
            val lineGap = 2f * dp
            val stackH = infoH + lineGap + botH
            val topY = barCY - stackH / 2f
            canvas.drawText(lensText, rightX, topY - infoPaint.ascent(), infoPaint)
            val botY = topY + infoH + lineGap - timePaint.ascent()
            if (hasTime && hasLoc) {
                canvas.drawText(locText, rightX, botY, timePaint)
                val locW = timePaint.measureText(locText)
                val divGap = 1f * dp
                val divX = rightX - locW - divGap
                canvas.drawLine(divX, botY + timePaint.ascent() * 0.9f, divX, botY + timePaint.descent() * 0.9f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = VIVO_TIME_GRAY; strokeWidth = maxOf(1f, dp * 0.33f) })
                canvas.drawText(timeText, divX - divGap, botY, timePaint)
            } else {
                canvas.drawText(if (hasTime) timeText else locText, rightX, botY, timePaint)
            }
        } else if (hasInfo) {
            canvas.drawText(lensText, rightX, barCY - (infoPaint.ascent() + infoPaint.descent()) / 2f, infoPaint)
        } else if (hasBottom) {
            val t = if (hasTime) timeText else locText
            canvas.drawText(t, rightX, barCY - (timePaint.ascent() + timePaint.descent()) / 2f, timePaint)
        }

        return result
    }

    // ---- iQOO Humanity (common_iqoo_humanity) ----
    /**
     * Minimalist bar: just "iQOO model" text.
     * typeface7 (vivoSansExpVF), 22.6dp, weight 500, #666666
     */
    private fun applyVivoIqooHumanity(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val shortSide = min(imgW, imgH).toFloat()
        val barHF = maxOf(shortSide * VIVO_BAR_RATIO, 80f)
        val barH = rint(barHF)
        val dp = barHF / VIVO_BAR_DP

        val totalH = imgH + barH
        val result = Bitmap.createBitmap(imgW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        canvas.drawRect(0f, imgH.toFloat(), imgW.toFloat(), totalH.toFloat(),
            Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })

        val barTop = imgH.toFloat()
        val barCY = barTop + barHF / 2f
        val marginL = 22f * dp

        val textPaint = createWeightedPaint(getVivoSansExp(context), 500).apply {
            textSize = 22.6f * dp; color = VIVO_3A_STD; textAlign = Paint.Align.LEFT
        }

        val deviceText = config.deviceName ?: "iQOO"
        canvas.drawText(deviceText, marginL,
            barCY - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)

        return result
    }

    // ---- ZEISS Frame (zeiss2) — Polaroid centered ----
    /**
     * Fixed frame zeiss2.png (1080×1710), photo in (27,27)-(1053,1395).
     * Centered below photo: model | ZEISS text + 3A params.
     */
    private fun applyVivoZeissFrameWm(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val tmplW = 1080; val tmplH = 1710
        val (result, canvas) = buildFrameComposite(context, source, "zeiss2.png",
            tmplW, tmplH, 27, 27, 1053, 1395) ?: return source

        val photoW = 1053 - 27
        val realScale = source.width.toFloat() / photoW
        val dp = realScale  // 1 template px = realScale real px; 1 dp = 3 px → dp = 3 * realScale / 3 = realScale

        // Center X of frame in real coords
        val cX = result.width / 2f
        // Area below photo for text: template Y 1395 to 1710, center ≈ 1552
        // ZEISS T* logo area: centered at about (225.5, 494.5) in dp → template pixel (676, 1483)
        // We'll draw model|ZEISS centered + 3A below

        // model | ZEISS (typeface4 = ZEISSFrutigerNextW1G-Bold, 14dp → 42px template, centered)
        val modelPaint = createWeightedPaint(getZeissBold(context), 700).apply {
            textSize = 14f * dp * 3; color = Color.BLACK; textAlign = Paint.Align.CENTER
        }
        // 3A (typeface1 = vivoCameraVF, 8dp → 24px)
        val threePaint = createWeightedPaint(getVivoCamera(context), 550).apply {
            textSize = 8f * dp * 3; color = VIVO_TIME_GRAY; textAlign = Paint.Align.CENTER
        }

        val deviceText = config.deviceName ?: ""
        val lensText = config.lensInfo ?: ""
        val zeissText = "ZEISS"

        // Centered Y for text block: roughly at template y=531 dp (center of info area below photo)
        val textCY = rint(1552f * realScale).toFloat()

        val modelStr = if (deviceText.isNotEmpty()) "$deviceText  |  $zeissText" else zeissText
        val modelH = modelPaint.descent() - modelPaint.ascent()
        val threeH = threePaint.descent() - threePaint.ascent()
        val gap = 2f * dp * 3

        val hasLens = lensText.isNotEmpty()
        val totalH = modelH + if (hasLens) gap + threeH else 0f
        val topY = textCY - totalH / 2f

        canvas.drawText(modelStr, cX, topY - modelPaint.ascent(), modelPaint)
        if (hasLens) {
            canvas.drawText(lensText, cX, topY + modelH + gap - threePaint.ascent(), threePaint)
        }

        return result
    }

    // ---- ZEISS Overlay — Planar/Portrait (zeiss4) & Distagon/Landscape (zeiss5) ----
    /**
     * Full-frame overlay: photo fills entire template, text drawn in white on image.
     * For portrait (zeiss4.png 1080×1920): model top-left, date top-right, 3 params at bottom
     * For landscape (zeiss5_new.png 1920×1080): same layout
     * Auto-selects based on source aspect ratio.
     */
    private fun applyVivoZeissOverlay(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val isPortrait = imgH >= imgW

        // Overlay directly on photo
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val shortSide = min(imgW, imgH).toFloat()
        val dp = shortSide / 360f  // template base is 360dp

        val shadowR = maxOf(3f, dp)
        val shadowOff = maxOf(1f, dp * 0.3f)

        // Load frame overlay on top
        val frameName = if (isPortrait) "zeiss4.png" else "zeiss5_new.png"
        try {
            context.assets.open("watermark/vivo/frames/$frameName").use { stream ->
                BitmapFactory.decodeStream(stream)?.let { frameBmp ->
                    canvas.drawBitmap(frameBmp, null, Rect(0, 0, imgW, imgH),
                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    frameBmp.recycle()
                }
            }
        } catch (_: Exception) {}

        // Text params: white, typeface4 = ZEISSFrutigerNextW1G-Bold for model,
        // typeface1 = vivoCameraVF for params
        val modelPaint = createWeightedPaint(getZeissBold(context), 700).apply {
            textSize = 12f * dp; color = Color.WHITE; textAlign = Paint.Align.LEFT
            setShadowLayer(shadowR, shadowOff, shadowOff, Color.argb(80, 0, 0, 0))
        }
        val datePaint = createWeightedPaint(getVivoCamera(context), 600).apply {
            textSize = 11f * dp; color = Color.WHITE; textAlign = Paint.Align.RIGHT; letterSpacing = 0.04f
            setShadowLayer(shadowR, shadowOff, shadowOff, Color.argb(80, 0, 0, 0))
        }
        val paramPaint = createWeightedPaint(getVivoCamera(context), 600).apply {
            textSize = 11f * dp; color = Color.WHITE; letterSpacing = 0.04f
            setShadowLayer(shadowR, shadowOff, shadowOff, Color.argb(80, 0, 0, 0))
        }

        val marginLR = 36f * dp
        val topY = 21f * dp

        // Top left: device model
        val deviceText = config.deviceName ?: ""
        if (deviceText.isNotEmpty()) {
            canvas.drawText(deviceText, marginLR, topY - modelPaint.ascent(), modelPaint)
        }

        // Top right: date
        val timeText = config.timeText ?: ""
        if (timeText.isNotEmpty()) {
            canvas.drawText(timeText, imgW - marginLR, topY - datePaint.ascent(), datePaint)
        }

        // Bottom: 3A params spread (zeiss4: ISO left, aperture center, shutter right)
        val lensText = config.lensInfo ?: ""
        if (lensText.isNotEmpty()) {
            val parts = lensText.split("  ", " ").filter { it.isNotEmpty() }
            val bottomY = if (isPortrait) imgH - 29f * dp else imgH - 19f * dp
            when {
                parts.size >= 4 -> {
                    // Format: "focal  aperture  shutter  ISO" → show ISO(L), aperture(C), shutter(R)
                    paramPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(parts[3], marginLR, bottomY, paramPaint)
                    paramPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(parts[1], imgW / 2f, bottomY, paramPaint)
                    paramPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(parts[2], imgW - marginLR, bottomY, paramPaint)
                }
                parts.size == 3 -> {
                    paramPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(parts[0], marginLR, bottomY, paramPaint)
                    paramPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(parts[1], imgW / 2f, bottomY, paramPaint)
                    paramPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(parts[2], imgW - marginLR, bottomY, paramPaint)
                }
                else -> {
                    paramPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(lensText, imgW / 2f, bottomY, paramPaint)
                }
            }
        }

        return result
    }

    // ---- ZEISS Center Frame (zeiss6) ----
    /**
     * Fixed frame zeiss6_new.png (1080×1476), photo in (279,300)-(801,996).
     * Centered below photo: model | ZEISS + 3A.
     */
    private fun applyVivoZeissCenter(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val tmplW = 1080; val tmplH = 1476
        val (result, canvas) = buildFrameComposite(context, source, "zeiss6_new.png",
            tmplW, tmplH, 279, 300, 801, 996) ?: return source

        val photoW = 801 - 279
        val realScale = source.width.toFloat() / photoW

        val cX = result.width / 2f
        // Text area center: between photo bottom (996px) and template bottom (1476px) → ~1236
        val textCY = 1236f * realScale

        // typeface4 = ZEISSFrutigerNextW1G-Bold
        val modelPaint = createWeightedPaint(getZeissBold(context), 700).apply {
            textSize = 14f * 3f * realScale; color = Color.BLACK; textAlign = Paint.Align.CENTER
        }
        // typeface1 = vivoCameraVF
        val threePaint = createWeightedPaint(getVivoCamera(context), 550).apply {
            textSize = 8f * 3f * realScale; color = VIVO_TIME_GRAY; textAlign = Paint.Align.CENTER
        }

        val deviceText = config.deviceName ?: ""
        val lensText = config.lensInfo ?: ""
        val modelStr = if (deviceText.isNotEmpty()) "$deviceText  |  ZEISS" else "ZEISS"

        val modelH = modelPaint.descent() - modelPaint.ascent()
        val threeH = threePaint.descent() - threePaint.ascent()
        val gap = 2f * 3f * realScale
        val hasLens = lensText.isNotEmpty()
        val totalH = modelH + if (hasLens) gap + threeH else 0f
        val topY = textCY - totalH / 2f

        canvas.drawText(modelStr, cX, topY - modelPaint.ascent(), modelPaint)
        if (hasLens) {
            canvas.drawText(lensText, cX, topY + modelH + gap - threePaint.ascent(), threePaint)
        }

        return result
    }

    // ---- vivo Frame (vivo2/3/4) — portrait/landscape/square ----
    /**
     * Simple frame: baseboard + centered vivo logo + model name.
     * Auto-selects frame based on aspect ratio:
     *   Portrait → vivo2.png (1080×1590), photo (27,27)-(1053,1395)
     *   Landscape → vivo3.png (1596×1080), photo (27,27)-(1569,894)
     *   Square → vivo4.png (1080×1413), photo (192,291)-(888,987)
     */
    private fun applyVivoFrameWm(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val ar = imgW.toFloat() / imgH

        // Select frame
        val (frame, tmplW, tmplH, pL, pT, pR, pB) = when {
            ar > 1.2f -> FrameSpec("vivo3.png", 1596, 1080, 27, 27, 1569, 894)
            ar < 0.85f -> FrameSpec("vivo2.png", 1080, 1590, 27, 27, 1053, 1395)
            else -> FrameSpec("vivo4.png", 1080, 1413, 192, 291, 888, 987)
        }

        val (result, canvas) = buildFrameComposite(context, source, frame,
            tmplW, tmplH, pL, pT, pR, pB) ?: return source

        val photoW = pR - pL
        val realScale = source.width.toFloat() / photoW
        val cX = result.width / 2f

        // Center Y for logo + text: midpoint of area below photo
        val textCY = ((pB + tmplH) / 2f) * realScale

        // Logo
        loadVivoLogo(context, "vivo_logo_new.png")?.let {
            val logoH = 16f * 3f * realScale
            val logoW = logoH * it.width / it.height
            val deviceText = config.deviceName ?: ""
            val devPaint = createWeightedPaint(getVivoCamera(context), 700).apply {
                textSize = 14f * 3f * realScale; color = Color.BLACK; textAlign = Paint.Align.LEFT
                letterSpacing = 0.05f
            }
            val textW = if (deviceText.isNotEmpty()) devPaint.measureText(deviceText) else 0f
            val totalW = logoW + textW
            val startX = cX - totalW / 2f

            val logoY = textCY - logoH / 2f
            canvas.drawBitmap(it, null, rectF(startX, logoY, startX + logoW, logoY + logoH),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            it.recycle()

            if (deviceText.isNotEmpty()) {
                canvas.drawText(deviceText, startX + logoW,
                    textCY - (devPaint.ascent() + devPaint.descent()) / 2f, devPaint)
            }
        }

        return result
    }

    // ---- vivo Frame + Time (vivo5) ----
    /**
     * Portrait frame with datetime: vivo5.png (1080×1590), photo (27,27)-(1053,1395).
     * Centered: vivo logo + model + datetime below.
     */
    private fun applyVivoFrameTime(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val tmplW = 1080; val tmplH = 1590
        val (result, canvas) = buildFrameComposite(context, source, "vivo5.png",
            tmplW, tmplH, 27, 27, 1053, 1395) ?: return source

        val photoW = 1053 - 27
        val realScale = source.width.toFloat() / photoW
        val cX = result.width / 2f
        val textCY = ((1395 + tmplH) / 2f - 10f) * realScale

        // Logo + model (line 1)
        val deviceText = config.deviceName ?: ""
        val devPaint = createWeightedPaint(getVivoCamera(context), 700).apply {
            textSize = 14f * 3f * realScale; color = Color.BLACK; textAlign = Paint.Align.LEFT
            letterSpacing = 0.05f
        }

        loadVivoLogo(context, "vivo_logo_new.png")?.let {
            val logoH = 16f * 3f * realScale
            val logoW = logoH * it.width / it.height
            val textW = if (deviceText.isNotEmpty()) devPaint.measureText(deviceText) else 0f
            val totalW = logoW + textW
            val startX = cX - totalW / 2f
            val logoY = textCY - logoH / 2f
            canvas.drawBitmap(it, null, rectF(startX, logoY, startX + logoW, logoY + logoH),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            if (deviceText.isNotEmpty()) {
                canvas.drawText(deviceText, startX + logoW,
                    textCY - (devPaint.ascent() + devPaint.descent()) / 2f, devPaint)
            }
            it.recycle()
        }

        // Datetime (line 2)
        val timeText = config.timeText ?: ""
        if (timeText.isNotEmpty()) {
            val timePaint = createWeightedPaint(getZeissBold(context), 400).apply {
                textSize = 7f * 3f * realScale; color = VIVO_TIME_GRAY; textAlign = Paint.Align.CENTER
                letterSpacing = 0.03f
            }
            val timeY = textCY + 12f * 3f * realScale
            canvas.drawText(timeText, cX, timeY, timePaint)
        }

        return result
    }

    // ---- iQOO Frame (iqoo2/3/4) ----
    /**
     * Same as vivo frame but with iQOO logo and font.
     * Auto-selects frame by aspect ratio (reuses vivo2/3/4 baseboards).
     */
    private fun applyVivoIqooFrameWm(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val ar = imgW.toFloat() / imgH

        val (frame, tmplW, tmplH, pL, pT, pR, pB) = when {
            ar > 1.2f -> FrameSpec("vivo3.png", 1596, 1080, 27, 27, 1569, 894)
            ar < 0.85f -> FrameSpec("vivo2.png", 1080, 1590, 27, 27, 1053, 1395)
            else -> FrameSpec("vivo4.png", 1080, 1413, 192, 291, 888, 987)
        }

        val (result, canvas) = buildFrameComposite(context, source, frame,
            tmplW, tmplH, pL, pT, pR, pB) ?: return source

        val photoW = pR - pL
        val realScale = source.width.toFloat() / photoW
        val cX = result.width / 2f
        val textCY = ((pB + tmplH) / 2f) * realScale

        loadVivoLogo(context, "iqoo_logo.png")?.let {
            val logoH = 17f * 3f * realScale
            val logoW = logoH * it.width / it.height
            val deviceText = config.deviceName ?: ""
            val devPaint = createWeightedPaint(getIqooBold(context), 500).apply {
                textSize = 13f * 3f * realScale; color = Color.BLACK; textAlign = Paint.Align.LEFT
                letterSpacing = 0.05f
            }
            val textW = if (deviceText.isNotEmpty()) devPaint.measureText(deviceText) else 0f
            val totalW = logoW + textW
            val startX = cX - totalW / 2f

            val logoY = textCY - logoH / 2f
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = android.graphics.PorterDuffColorFilter(
                    Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            canvas.drawBitmap(it, null, rectF(startX, logoY, startX + logoW, logoY + logoH), tintPaint)
            it.recycle()

            if (deviceText.isNotEmpty()) {
                canvas.drawText(deviceText, startX + logoW,
                    textCY - (devPaint.ascent() + devPaint.descent()) / 2f, devPaint)
            }
        }

        return result
    }

    // ---- iQOO Frame + Time (iqoo5) ----
    /**
     * Portrait frame with datetime + iQOO branding: vivo5.png (1080×1590).
     */
    private fun applyVivoIqooFrameTime(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val tmplW = 1080; val tmplH = 1590
        val (result, canvas) = buildFrameComposite(context, source, "vivo5.png",
            tmplW, tmplH, 27, 27, 1053, 1395) ?: return source

        val photoW = 1053 - 27
        val realScale = source.width.toFloat() / photoW
        val cX = result.width / 2f
        val textCY = ((1395 + tmplH) / 2f - 10f) * realScale

        val deviceText = config.deviceName ?: ""
        val devPaint = createWeightedPaint(getIqooBold(context), 500).apply {
            textSize = 13f * 3f * realScale; color = Color.BLACK; textAlign = Paint.Align.LEFT
            letterSpacing = 0.05f
        }

        loadVivoLogo(context, "iqoo_logo.png")?.let {
            val logoH = 17f * 3f * realScale
            val logoW = logoH * it.width / it.height
            val textW = if (deviceText.isNotEmpty()) devPaint.measureText(deviceText) else 0f
            val totalW = logoW + textW
            val startX = cX - totalW / 2f
            val logoY = textCY - logoH / 2f
            val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = android.graphics.PorterDuffColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN)
            }
            canvas.drawBitmap(it, null, rectF(startX, logoY, startX + logoW, logoY + logoH), logoPaint)
            if (deviceText.isNotEmpty()) {
                canvas.drawText(deviceText, startX + logoW,
                    textCY - (devPaint.ascent() + devPaint.descent()) / 2f, devPaint)
            }
            it.recycle()
        }

        val timeText = config.timeText ?: ""
        if (timeText.isNotEmpty()) {
            val timePaint = createWeightedPaint(getRobotoBold(context), 550).apply {
                textSize = 7f * 3f * realScale; color = VIVO_TIME_GRAY; textAlign = Paint.Align.CENTER
                letterSpacing = 0.05f
            }
            canvas.drawText(timeText, cX, textCY + 12f * 3f * realScale, timePaint)
        }

        return result
    }

    // ---- OriginOS (os1/os2) ----
    /**
     * OriginOS frame with 3A info, location, datetime + origin_os_logo.
     * Auto-selects portrait (os1.png 1080×1620) or landscape (os2.png 1350×1056).
     */
    private fun applyVivoOS(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val isPortrait = imgH >= imgW

        val (frame, tmplW, tmplH, pL, pT, pR, pB) = if (isPortrait) {
            FrameSpec("os1.png", 1080, 1620, 162, 153, 918, 1161)
        } else {
            FrameSpec("os2.png", 1350, 1056, 51, 51, 1299, 753)
        }

        val (result, canvas) = buildFrameComposite(context, source, frame,
            tmplW, tmplH, pL, pT, pR, pB) ?: return source

        val photoW = pR - pL
        val realScale = source.width.toFloat() / photoW
        val textColor = Color.parseColor("#231916")

        // LEFT: 3A info
        val marginL = (if (isPortrait) 54f else 18f) * 3f * realScale
        val lensText = config.lensInfo ?: ""
        val locText = config.locationText ?: ""
        val timeText = config.timeText ?: ""

        val infoPaint = createWeightedPaint(getVivoSansExp(context), 700).apply {
            textSize = 12f * 3f * realScale; color = textColor; textAlign = Paint.Align.LEFT
        }
        val subPaint = createWeightedPaint(getVivoSansExp(context), 550).apply {
            textSize = 8f * 3f * realScale; color = VIVO_TIME_GRAY; textAlign = Paint.Align.LEFT
        }

        // Text area center Y: midpoint below photo
        val textAreaTop = pB.toFloat() * realScale
        val textAreaBot = tmplH.toFloat() * realScale
        val areaH = textAreaBot - textAreaTop
        val textCY = textAreaTop + areaH * 0.45f  // slightly above center for text

        // Line 1: 3A info
        if (lensText.isNotEmpty()) {
            canvas.drawText(lensText, marginL, textCY - (infoPaint.ascent() + infoPaint.descent()) / 2f - 8f * realScale, infoPaint)
        }

        // Line 2: location + datetime
        if (locText.isNotEmpty() || timeText.isNotEmpty()) {
            val line2Y = textCY - (subPaint.ascent() + subPaint.descent()) / 2f + 12f * realScale
            var x = marginL
            if (locText.isNotEmpty()) {
                canvas.drawText(locText, x, line2Y, subPaint)
                x += subPaint.measureText(locText) + 10f * realScale
            }
            if (timeText.isNotEmpty()) {
                canvas.drawText(timeText, x, line2Y, subPaint)
            }
        }

        // RIGHT(bottom): origin_os_logo
        loadVivoLogo(context, "origin_os_logo.png")?.let {
            val logoH = 24f * 3f * realScale
            val logoW = logoH * it.width / it.height
            val logoX = result.width - marginL - logoW
            val logoY = textAreaTop + areaH * 0.7f - logoH / 2f
            canvas.drawBitmap(it, null, rectF(logoX, logoY, logoX + logoW, logoY + logoH),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            it.recycle()
        }

        return result
    }

    // ---- OriginOS Corner (os3/os4) ----
    /**
     * Simple frame with corner icons + centered origin_os_logo.
     * Auto-selects portrait (os3.png 1080×1590) or landscape (os4.png 1461×1080).
     */
    private fun applyVivoOSCorner(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val isPortrait = imgH >= imgW

        val (frame, tmplW, tmplH, pL, pT, pR, pB) = if (isPortrait) {
            FrameSpec("os3.png", 1080, 1590, 153, 165, 927, 1197)
        } else {
            FrameSpec("os4.png", 1461, 1080, 111, 123, 1350, 819)
        }

        val (result, canvas) = buildFrameComposite(context, source, frame,
            tmplW, tmplH, pL, pT, pR, pB) ?: return source

        // Frame already has corner icons and logo baked in. Nothing else to draw.
        return result
    }

    // ---- OriginOS Simple (os5/os6) ----
    /**
     * Simple frame with centered os_logo only.
     * Auto-selects portrait (os5.png 1080×1614) or landscape (os6.png 1677×1155).
     */
    private fun applyVivoOSSimple(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val isPortrait = imgH >= imgW

        val (frame, tmplW, tmplH, pL, pT, pR, pB) = if (isPortrait) {
            FrameSpec("os5.png", 1080, 1614, 36, 36, 1044, 1380)
        } else {
            FrameSpec("os6.png", 1677, 1155, 36, 36, 1641, 939)
        }

        val (result, canvas) = buildFrameComposite(context, source, frame,
            tmplW, tmplH, pL, pT, pR, pB) ?: return source

        // Frame already has os_logo baked in. Nothing else to draw.
        return result
    }

    // ---- Event Frame (event1/event2) ----
    /**
     * Event-themed frame with model name, 3A params, location, datetime.
     * Auto-selects portrait (event1.webp 1080×1620) or landscape (event2.webp 1350×1056).
     */
    private fun applyVivoEvent(
        context: Context, source: Bitmap, config: WatermarkConfig
    ): Bitmap {
        val imgW = source.width
        val imgH = source.height
        val isPortrait = imgH >= imgW

        val (frame, tmplW, tmplH, pL, pT, pR, pB) = if (isPortrait) {
            FrameSpec("event1.webp", 1080, 1620, 0, 0, 1080, 1355)
        } else {
            FrameSpec("event2.webp", 1350, 1056, 0, 0, 1350, 856)
        }

        val (result, canvas) = buildFrameComposite(context, source, frame,
            tmplW, tmplH, pL, pT, pR, pB) ?: return source

        val photoW = pR - pL
        val realScale = source.width.toFloat() / photoW

        // Text area below photo
        val marginL = 16f * 3f * realScale

        val deviceText = config.deviceName ?: ""
        val lensText = config.lensInfo ?: ""
        val locText = config.locationText ?: ""
        val timeText = config.timeText ?: ""

        val textColor = Color.BLACK
        val subColor = Color.parseColor("#5F5F5F")

        // Model name (19dp, typeface10 ≈ vivoSansExpVF, w600)
        val modelPaint = createWeightedPaint(getVivoSansExp(context), 600).apply {
            textSize = 19f * 3f * realScale; color = textColor; textAlign = Paint.Align.LEFT
        }
        // 3A params (7dp, w400)
        val threePaint = createWeightedPaint(getVivoSansExp(context), 400).apply {
            textSize = 7f * 3f * realScale; color = subColor; textAlign = Paint.Align.LEFT
        }
        // Location/datetime (5dp, w400)
        val locPaintE = createWeightedPaint(getVivoSansExp(context), 400).apply {
            textSize = 5f * 3f * realScale; color = subColor; textAlign = Paint.Align.LEFT
        }

        val textAreaTop = pB.toFloat() * realScale
        var curY = textAreaTop + 20f * 3f * realScale

        // Line 1: model
        if (deviceText.isNotEmpty()) {
            canvas.drawText(deviceText, marginL + 10f * 3f * realScale, curY, modelPaint)
            curY += modelPaint.descent() - modelPaint.ascent() + 2f * 3f * realScale
        }

        // Line 2: 3A params
        if (lensText.isNotEmpty()) {
            canvas.drawText(lensText, marginL, curY, threePaint)
            curY += threePaint.descent() - threePaint.ascent() + 1f * 3f * realScale
        }

        // Line 3: location + datetime
        if (locText.isNotEmpty() || timeText.isNotEmpty()) {
            var x = marginL
            if (locText.isNotEmpty()) {
                canvas.drawText(locText, x, curY, locPaintE)
                x += locPaintE.measureText(locText) + 3f * 3f * realScale
            }
            if (timeText.isNotEmpty()) {
                canvas.drawText(timeText, x, curY, locPaintE)
            }
        }

        return result
    }

    /**
     * Data class for frame template specifications.
     */
    private data class FrameSpec(
        val frame: String,
        val tmplW: Int, val tmplH: Int,
        val pL: Int, val pT: Int, val pR: Int, val pB: Int
    )

    // ==================== CONFIG-DRIVEN VIVO WATERMARKS ====================

    private fun applyVivoConfigDriven(
        context: Context, source: Bitmap, config: WatermarkConfig, templatePath: String
    ): Bitmap {
        return try {
            val parser = VivoWatermarkConfigParser(context)
            val template = parser.parseConfig(templatePath)

            if (template != null) {
                val renderer = ZeissWatermarkRenderer(context)
                val renderConfig = VivoRenderConfig(
                    deviceName = config.deviceName,
                    timeText = config.timeText,
                    locationText = config.locationText,
                    lensInfo = config.lensInfo
                )
                renderer.render(source, template, renderConfig)
            } else {
                // Fallback to classic ZEISS if template loading fails
                applyVivoZeiss(context, source, config)
            }
        } catch (e: Exception) {
            // Fallback to classic implementation on error
            Log.w("WatermarkProcessor", "Config-driven watermark failed, using fallback", e)
            applyVivoZeiss(context, source, config)
        }
    }

    // ─── TECNO Watermark ────────────────────────────────

    // Reference bar dimensions (from TranssionWM.json, portrait mode at 1080px width)
    private const val TECNO_REF_WIDTH = 1080f
    private const val TECNO_BAR_HEIGHT_PORTRAIT = 113f
    private const val TECNO_BAR_HEIGHT_LANDSCAPE = 95f

    // Cached TECNO typefaces
    private var tecnoBrandTypeface: Typeface? = null
    private var tecnoDateTypeface: Typeface? = null

    private fun getTecnoBrandTypeface(context: Context): Typeface {
        tecnoBrandTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/TECNO/fonts/Transota0226-Regular.ttf").also {
                tecnoBrandTypeface = it
            }
        } catch (e: Exception) {
            Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    private fun getTecnoDateTypeface(context: Context): Typeface {
        tecnoDateTypeface?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "watermark/TECNO/fonts/tos_regular.ttf").also {
                tecnoDateTypeface = it
            }
        } catch (e: Exception) {
            Typeface.create("sans-serif", Typeface.NORMAL)
        }
    }

    /**
     * Apply TECNO watermark using config-driven approach (modes 1-4).
     * This reads from TranssionWM.json for accurate positioning.
     */
    private fun applyTecnoConfigDrivenWatermark(
        context: Context,
        source: Bitmap,
        config: WatermarkConfig,
        mode: Int
    ): Bitmap {
        return try {
            val parser = TecnoWatermarkConfigParser(context)
            val template = parser.parseConfig()
            
            if (template != null) {
                val isLandscape = source.width > source.height
                val modeName = getTecnoModeName(mode, isLandscape)
                
                val renderer = TecnoWatermarkRenderer(context)
                val renderConfig = TecnoRenderConfig(
                    deviceName = config.deviceName,
                    timeText = config.timeText,
                    locationText = config.locationText,
                    lensInfo = config.lensInfo
                )
                renderer.render(source, template, modeName, isLandscape, renderConfig)
            } else {
                // Fallback to legacy implementation
                applyTecnoWatermark(context, source, config, mode)
            }
        } catch (e: Exception) {
            Log.w("WatermarkProcessor", "TECNO config-driven watermark failed, using legacy", e)
            applyTecnoWatermark(context, source, config, mode)
        }
    }
    
    /**
     * Get the TECNO mode name based on mode number and orientation.
     * Uses the 'a' variant which is most common.
     */
    private fun getTecnoModeName(mode: Int, isLandscape: Boolean): String {
        val suffix = if (isLandscape) "_LANDSCAPE" else "_PORTRAIT"
        
        // Map mode to sub-mode (a, b, c variants)
        // Using 'a' variant as default (most common)
        return "Mode_${mode}a$suffix"
    }

    /**
     * Apply TECNO watermark (modes 1-4) - Legacy implementation.
     *
     * Mode 1: Brand name only + date/time on right
     * Mode 2: Brand name (smaller) + date/time on right + secondary text below brand
     * Mode 3: Brand name + date/time on right (2 lines) + secondary text below brand
     * Mode 4: Brand name + date/time right + secondary text + location text
     *
     * All modes auto-detect portrait vs landscape based on image aspect ratio.
     * Dimensions are scaled proportionally from the 1080px reference width.
     */
    private fun applyTecnoWatermark(
        context: Context,
        source: Bitmap,
        config: WatermarkConfig,
        mode: Int
    ): Bitmap {
        val imgWidth = source.width
        val imgHeight = source.height
        val isLandscape = imgWidth > imgHeight

        val scale = imgWidth / TECNO_REF_WIDTH
        val barHeight = if (isLandscape) TECNO_BAR_HEIGHT_LANDSCAPE else TECNO_BAR_HEIGHT_PORTRAIT
        val scaledBarHeight = (barHeight * scale).toInt()

        val totalHeight = imgHeight + scaledBarHeight

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

        val barTop = imgHeight.toFloat()

        // Draw backdrop texture on the right side
        try {
            context.assets.open("watermark/TECNO/icons/TriangleTexture.png").use { stream ->
                val texBitmap = BitmapFactory.decodeStream(stream)
                if (texBitmap != null) {
                    val texWidth = (429f * scale).toInt()
                    val texHeight = scaledBarHeight
                    val texX = (651f * scale).toInt()
                    val texRect = Rect(texX, barTop.toInt(), texX + texWidth, barTop.toInt() + texHeight)
                    canvas.drawBitmap(texBitmap, null, texRect, Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 255 })
                    texBitmap.recycle()
                }
            }
        } catch (_: Exception) {}

        // Prepare paints
        val brandTypeface = getTecnoBrandTypeface(context)
        val dateTypeface = getTecnoDateTypeface(context)

        val deviceText = config.deviceName ?: "TECNO"
        val timeText = config.timeText ?: ""
        val lensText = config.lensInfo ?: ""
        val locText = config.locationText ?: ""

        val rightXLimit = (if (isLandscape) 1050f else 1041f) * scale

        when (mode) {
            1 -> {
                // Mode 1: Brand name on left, date on right
                val brandFontSize = (if (isLandscape) 22f else 29f) * scale
                val brandY = (if (isLandscape) 59f else 72f) * scale
                val brandX = (if (isLandscape) 30f else 39f) * scale
                val dateFontSize = (if (isLandscape) 18f else 23f) * scale
                val dateY = (if (isLandscape) 58f else 70f) * scale

                val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = brandTypeface
                    textSize = brandFontSize
                    color = Color.BLACK
                }
                canvas.drawText(deviceText, brandX, barTop + brandY, brandPaint)

                // Draw dot after brand name
                val brandWidth = brandPaint.measureText(deviceText)
                drawTecnoYellowDot(context, canvas,
                    brandX + brandWidth + 5f * scale,
                    barTop + brandY - (if (isLandscape) 25f else 34f) * scale / 2f,
                    (if (isLandscape) 25f else 34f) * scale)

                // Date on right
                if (timeText.isNotEmpty()) {
                    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = dateTypeface
                        textSize = dateFontSize
                        color = Color.parseColor("#020202")
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(timeText, rightXLimit, barTop + dateY, datePaint)
                }
            }
            2 -> {
                // Mode 2: Brand name (smaller) + date on right + lens info below
                val brandFontSize = (if (isLandscape) 18f else 24f) * scale
                val brandY = (if (isLandscape) 40f else 52f) * scale
                val brandX = (if (isLandscape) 30f else 39f) * scale
                val dateFontSize = (if (isLandscape) 18f else 23f) * scale
                val dateY = (if (isLandscape) 58f else 70f) * scale
                val secondaryFontSize = (if (isLandscape) 14f else 19f) * scale
                val secondaryY = (if (isLandscape) 70f else 86f) * scale

                val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = brandTypeface
                    textSize = brandFontSize
                    color = Color.BLACK
                }
                canvas.drawText(deviceText, brandX, barTop + brandY, brandPaint)

                val brandWidth = brandPaint.measureText(deviceText)
                drawTecnoYellowDot(context, canvas,
                    brandX + brandWidth + 5f * scale,
                    barTop + brandY - (if (isLandscape) 20f else 28f) * scale / 2f,
                    (if (isLandscape) 20f else 28f) * scale)

                // Date on right
                if (timeText.isNotEmpty()) {
                    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = dateTypeface
                        textSize = dateFontSize
                        color = Color.BLACK
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(timeText, rightXLimit, barTop + dateY, datePaint)
                }

                // Lens info below brand
                if (lensText.isNotEmpty()) {
                    val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = dateTypeface
                        textSize = secondaryFontSize
                        color = Color.parseColor("#808080")
                    }
                    canvas.drawText(lensText, brandX, barTop + secondaryY, secondaryPaint)
                }
            }
            3 -> {
                // Mode 3: Brand name + time on right (2 lines - date top, time below) + lens below
                val brandFontSize = (if (isLandscape) 22f else 29f) * scale
                val brandY = (if (isLandscape) 59f else 72f) * scale
                val brandX = (if (isLandscape) 30f else 39f) * scale
                val dateFontSize = (if (isLandscape) 18f else 23f) * scale
                val dateY = (if (isLandscape) 40f else 52f) * scale
                val secondaryFontSize = (if (isLandscape) 14f else 19f) * scale
                val secondaryY = (if (isLandscape) 70f else 85f) * scale

                val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = brandTypeface
                    textSize = brandFontSize
                    color = Color.BLACK
                }
                canvas.drawText(deviceText, brandX, barTop + brandY, brandPaint)

                val brandWidth = brandPaint.measureText(deviceText)
                drawTecnoYellowDot(context, canvas,
                    brandX + brandWidth + 5f * scale,
                    barTop + brandY - 34f * scale / 2f,
                    34f * scale)

                // Date/time on right - split into two lines
                if (timeText.isNotEmpty()) {
                    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = dateTypeface
                        textSize = dateFontSize
                        color = Color.BLACK
                        textAlign = Paint.Align.RIGHT
                    }
                    // Split time: "2026/02/15 12:30" -> date on top, time below
                    val parts = timeText.split(" ", limit = 2)
                    canvas.drawText(parts[0], rightXLimit, barTop + dateY, datePaint)
                    if (parts.size > 1) {
                        val timePart = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            typeface = dateTypeface
                            textSize = secondaryFontSize
                            color = Color.parseColor("#808080")
                            textAlign = Paint.Align.RIGHT
                        }
                        canvas.drawText(parts[1], rightXLimit, barTop + secondaryY, timePart)
                    }
                }

                // Lens info below brand on second line
                if (lensText.isNotEmpty()) {
                    val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = dateTypeface
                        textSize = secondaryFontSize
                        color = Color.parseColor("#808080")
                    }
                    // Position relative to date/time right-aligned text
                    val dateWidth = if (timeText.isNotEmpty()) {
                        val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            typeface = dateTypeface
                            textSize = dateFontSize
                        }
                        dp.measureText(timeText.split(" ", limit = 2)[0])
                    } else 0f
                    canvas.drawText(lensText, rightXLimit - dateWidth, barTop + secondaryY, lensPaint)
                }
            }
            4 -> {
                // Mode 4: Brand name + date on right + lens info + location
                val brandFontSize = (if (isLandscape) 18f else 24f) * scale
                val brandY = (if (isLandscape) 40f else 52f) * scale
                val brandX = (if (isLandscape) 30f else 39f) * scale
                val dateFontSize = (if (isLandscape) 18f else 23f) * scale
                val dateY = (if (isLandscape) 40f else 52f) * scale
                val secondaryFontSize = (if (isLandscape) 14f else 19f) * scale
                val secondaryY = (if (isLandscape) 68f else 86f) * scale

                val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = brandTypeface
                    textSize = brandFontSize
                    color = Color.BLACK
                }
                canvas.drawText(deviceText, brandX, barTop + brandY, brandPaint)

                val brandWidth = brandPaint.measureText(deviceText)
                drawTecnoYellowDot(context, canvas,
                    brandX + brandWidth + 5f * scale,
                    barTop + brandY - 28f * scale / 2f,
                    28f * scale)

                // Date on right
                if (timeText.isNotEmpty()) {
                    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = dateTypeface
                        textSize = dateFontSize
                        color = Color.BLACK
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(timeText, rightXLimit, barTop + dateY, datePaint)
                }

                // Lens info below brand
                if (lensText.isNotEmpty()) {
                    val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = dateTypeface
                        textSize = secondaryFontSize
                        color = Color.parseColor("#808080")
                    }
                    canvas.drawText(lensText, brandX, barTop + secondaryY, secondaryPaint)
                }

                // Location text below date on right
                if (locText.isNotEmpty()) {
                    val locPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = dateTypeface
                        textSize = secondaryFontSize
                        color = Color.parseColor("#808080")
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(locText, rightXLimit, barTop + secondaryY, locPaint)
                }
            }
        }

        return result
    }

    /**
     * Draw the TECNO yellow/orange dot icon.
     */
    private fun drawTecnoYellowDot(
        context: Context,
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float
    ) {
        try {
            context.assets.open("watermark/TECNO/icons/YellowPoint.png").use { stream ->
                val dotBitmap = BitmapFactory.decodeStream(stream)
                if (dotBitmap != null) {
                    val rect = Rect(x.toInt(), y.toInt(), (x + size).toInt(), (y + size).toInt())
                    canvas.drawBitmap(dotBitmap, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
                    dotBitmap.recycle()
                }
            }
        } catch (_: Exception) {
            // Fallback: draw a simple orange circle
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF8C00")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(x + size / 2f, y + size / 2f, size / 3f, paint)
        }
    }
}
