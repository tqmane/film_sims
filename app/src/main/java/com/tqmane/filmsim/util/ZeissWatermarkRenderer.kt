package com.tqmane.filmsim.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ZEISS Watermark Renderer - Accurate Implementation
 * Renders ZEISS/VIVO watermarks using parsed configuration templates with exact positioning
 */
class ZeissWatermarkRenderer(private val context: Context) {

    private val tag = "ZeissRenderer"
    private val fontManager = VivoFontManager(context)
    private val imageCache = mutableMapOf<String, Bitmap>()

    /**
     * Main render function
     */
    fun render(
        source: Bitmap,
        template: VivoWatermarkTemplate?,
        config: VivoRenderConfig
    ): Bitmap {
        if (template == null) {
            return source
        }

        val frame = template.frame

        // Determine rendering mode
        val isFixed = frame.isfixed
        val isAdaptive = frame.isadaptive

        // Calculate scale based on template dimensions
        val scale = calculateScale(source, frame)

        return if (isFixed) {
            renderFixedWatermark(source, template, config, scale)
        } else {
            renderAdaptiveWatermark(source, template, config, scale)
        }
    }

    /**
     * Calculate scale factor based on source image and template dimensions
     */
    private fun calculateScale(source: Bitmap, frame: VivoFrameConfig): Float {
        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()
        val tmplW = frame.templatewidth.toFloat()
        val tmplH = frame.templateheight.toFloat()

        // For landscape images (like zeiss5 - 1920x1080), scale based on width
        // For portrait images (like zeiss1 - 1080x1719), scale based on width
        return when {
            frame.templatewidth > frame.templateheight -> {
                // Landscape template - scale to match source width
                srcW / tmplW
            }
            else -> {
                // Portrait template - scale to match source width
                srcW / tmplW
            }
        }
    }

    /**
     * Render fixed watermark (full image overlay like zeiss4, zeiss5)
     * Template covers the entire image with background and positioned elements
     */
    private fun renderFixedWatermark(
        source: Bitmap,
        template: VivoWatermarkTemplate,
        config: VivoRenderConfig,
        scale: Float
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val frame = template.frame

        // Load and draw baseboard if specified
        if (frame.baseboard.isNotEmpty()) {
            drawBaseboard(canvas, frame.baseboard, source.width, source.height, scale)
        }

        // Render all groups with exact coordinates
        for (group in template.groups) {
            renderGroupFixed(canvas, group, config, scale, source)
        }

        return result
    }

    /**
     * Render adaptive watermark (bottom bar style like zeiss0, zeiss1, zeiss3)
     * Adds white border at bottom with positioned elements
     */
    private fun renderAdaptiveWatermark(
        source: Bitmap,
        template: VivoWatermarkTemplate,
        config: VivoRenderConfig,
        scale: Float
    ): Bitmap {
        val frame = template.frame
        val imgW = source.width
        val imgH = source.height

        // Calculate border height from template dimensions
        // Content area is from (27,27) to (1053-1054, 1395-1396) for most templates
        // So the content height is about 1369px, border is templateheight - content height
        val contentTop = 27f * scale
        val contentHeight = (frame.templateheight - 27f - 324f) * scale  // Approximate
        val borderHeight = maxOf((frame.templateheight - 1395f) * scale, 80f)

        val totalHeight = imgH + borderHeight.toInt()
        val result = Bitmap.createBitmap(imgW, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw original image
        canvas.drawBitmap(source, 0f, 0f, null)

        // Draw white border/background
        val barPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, imgH.toFloat(), imgW.toFloat(), totalHeight.toFloat(), barPaint)

        // Load and draw baseboard if specified
        if (frame.baseboard.isNotEmpty()) {
            drawBaseboardInBorder(canvas, frame.baseboard, imgW, borderHeight, scale, imgH)
        }

        // Apply content area clipping if paths exist
        val clipPath = template.paths.firstOrNull()?.let { path ->
            createPathFromPoints(path.points, scale)
        }
        clipPath?.let {
            canvas.clipPath(it)
        }

        // Render all groups with exact coordinates
        val barTop = imgH.toFloat()
        for (group in template.groups) {
            renderGroupAdaptive(canvas, group, config, scale, barTop, imgW)
        }

        return result
    }

    /**
     * Draw baseboard background image
     */
    private fun drawBaseboard(canvas: Canvas, baseboard: String, width: Int, height: Int, scale: Float) {
        val bitmap = loadImage(baseboard)
        bitmap?.let { bmp ->
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(bmp, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        }
    }

    /**
     * Draw baseboard in border area
     */
    private fun drawBaseboardInBorder(
        canvas: Canvas,
        baseboard: String,
        width: Int,
        borderHeight: Float,
        scale: Float,
        imgH: Int
    ) {
        val bitmap = loadImage(baseboard)
        bitmap?.let { bmp ->
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            val srcRect = Rect(0, 0, bmp.width, bmp.height)
            val dstRect = RectF(0f, imgH.toFloat(), width.toFloat(), imgH + borderHeight)
            canvas.drawBitmap(bmp, srcRect, dstRect, paint)
        }
    }

    /**
     * Create Path from points for content area clipping
     */
    private fun createPathFromPoints(points: List<VivoPoint>, scale: Float): Path {
        val path = Path()
        if (points.isNotEmpty()) {
            val first = points[0]
            path.moveTo(first.x * scale, first.y * scale)

            for (i in 1 until points.size) {
                val point = points[i]
                path.lineTo(point.x * scale, point.y * scale)
            }
            path.close()
        }
        return path
    }

    /**
     * Render group for fixed watermark (uses exact coordinates)
     */
    private fun renderGroupFixed(
        canvas: Canvas,
        group: VivoParamGroup,
        config: VivoRenderConfig,
        scale: Float,
        source: Bitmap
    ) {
        val groupMarginEnd = group.groupmarginend * scale
        val rightLimit = source.width - groupMarginEnd

        // Find visible subgroup or use first one
        val visibleSubgroups = group.subgroups.filter { it.subgroupvisible }
        val subgroupsToRender = if (visibleSubgroups.isNotEmpty()) visibleSubgroups else group.subgroups

        for (subgroup in subgroupsToRender) {
            for (line in subgroup.lines) {
                renderLineFixed(canvas, line, group.groupgravity, config, scale, rightLimit)
            }
        }
    }

    /**
     * Render group for adaptive watermark (uses exact coordinates with bar offset)
     */
    private fun renderGroupAdaptive(
        canvas: Canvas,
        group: VivoParamGroup,
        config: VivoRenderConfig,
        scale: Float,
        barTop: Float,
        imgW: Int
    ) {
        val groupMarginEnd = group.groupmarginend * scale
        val rightLimit = imgW - groupMarginEnd

        // Find visible subgroup or use first one
        val visibleSubgroups = group.subgroups.filter { it.subgroupvisible }
        val subgroupsToRender = if (visibleSubgroups.isNotEmpty()) visibleSubgroups else group.subgroups

        for (subgroup in subgroupsToRender) {
            for (line in subgroup.lines) {
                renderLineAdaptive(canvas, line, group.groupgravity, config, scale, barTop, rightLimit)
            }
        }
    }

    /**
     * Render line for fixed watermark (uses exact coordinates from config)
     */
    private fun renderLineFixed(
        canvas: Canvas,
        line: VivoLine,
        gravity: String,
        config: VivoRenderConfig,
        scale: Float,
        rightLimit: Float
    ) {
        // Render images first
        for (imageParam in line.images) {
            renderImageFixed(canvas, imageParam, scale)
        }

        // Render texts
        for (textParam in line.texts) {
            renderTextFixed(canvas, textParam, config, scale, rightLimit)
        }
    }

    /**
     * Render line for adaptive watermark (uses exact coordinates from config with bar offset)
     */
    private fun renderLineAdaptive(
        canvas: Canvas,
        line: VivoLine,
        gravity: String,
        config: VivoRenderConfig,
        scale: Float,
        barTop: Float,
        rightLimit: Float
    ) {
        val lineMarginBottom = line.linemarginbottom * scale

        // Render images first
        for (imageParam in line.images) {
            renderImageAdaptive(canvas, imageParam, scale, barTop)
        }

        // Render texts
        for (textParam in line.texts) {
            renderTextAdaptive(canvas, textParam, config, scale, barTop, rightLimit)
        }
    }

    /**
     * Render image for fixed watermark at exact coordinates
     */
    private fun renderImageFixed(
        canvas: Canvas,
        imageParam: VivoImageParam,
        scale: Float
    ) {
        val bitmap = loadImage(imageParam.pic)
        val rect = imageParam.picpoint ?: return

        bitmap?.let { bmp ->
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            if (!imageParam.isneedantialias) {
                paint.flags = paint.flags and Paint.ANTI_ALIAS_FLAG.inv()
            }

            // Apply margin if specified
            val marginStart = imageParam.picmarginstart * scale
            val adjustedRect = VivoRect(
                left = rect.left * scale + marginStart,
                top = rect.top * scale,
                right = rect.right * scale + marginStart,
                bottom = rect.bottom * scale
            )

            val dstRect = RectF(
                adjustedRect.left,
                adjustedRect.top,
                adjustedRect.right,
                adjustedRect.bottom
            )

            canvas.drawBitmap(bmp, null, dstRect, paint)
        }
    }

    /**
     * Render image for adaptive watermark at exact coordinates with bar offset
     */
    private fun renderImageAdaptive(
        canvas: Canvas,
        imageParam: VivoImageParam,
        scale: Float,
        barTop: Float
    ) {
        val bitmap = loadImage(imageParam.pic)
        val rect = imageParam.picpoint ?: return

        bitmap?.let { bmp ->
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            if (!imageParam.isneedantialias) {
                paint.flags = paint.flags and Paint.ANTI_ALIAS_FLAG.inv()
            }

            // Apply margin if specified
            val marginStart = imageParam.picmarginstart * scale
            val adjustedRect = VivoRect(
                left = rect.left * scale + marginStart,
                top = rect.top * scale + barTop,
                right = rect.right * scale + marginStart,
                bottom = rect.bottom * scale + barTop
            )

            val dstRect = RectF(
                adjustedRect.left,
                adjustedRect.top,
                adjustedRect.right,
                adjustedRect.bottom
            )

            canvas.drawBitmap(bmp, null, dstRect, paint)
        }
    }

    /**
     * Render text for fixed watermark at exact coordinates
     */
    private fun renderTextFixed(
        canvas: Canvas,
        textParam: VivoTextParam,
        config: VivoRenderConfig,
        scale: Float,
        rightLimit: Float
    ) {
        val rect = textParam.textpoint ?: return
        val text = getTextContent(textParam, config)
        if (text.isEmpty()) return

        val paint = fontManager.getTextPaint(textParam, scale)
        val gravity = textParam.textgravity

        // Calculate position based on gravity
        val x = when {
            gravity == "start" || gravity == "left" -> rect.left * scale
            gravity == "end" || gravity == "right" -> rect.right * scale
            gravity == "center" -> (rect.left + rect.right) / 2f * scale
            else -> rect.left * scale
        }

        // Use textsize from config for height calculation
        val textSize = textParam.textsize * scale
        val y = when {
            VivoGravity.hasCenterVertical(gravity) -> {
                val rectHeight = (rect.bottom - rect.top) * scale
                val centerY = (rect.top + rect.bottom) / 2f * scale
                centerY + (paint.ascent() + paint.descent()) / 2f
            }
            else -> rect.bottom * scale - paint.descent()
        }

        // Apply text alignment
        paint.textAlign = when (gravity) {
            "start", "left" -> Paint.Align.LEFT
            "end", "right" -> Paint.Align.RIGHT
            "center" -> Paint.Align.CENTER
            else -> Paint.Align.LEFT
        }

        canvas.drawText(text, x, y, paint)
    }

    /**
     * Render text for adaptive watermark at exact coordinates with bar offset
     */
    private fun renderTextAdaptive(
        canvas: Canvas,
        textParam: VivoTextParam,
        config: VivoRenderConfig,
        scale: Float,
        barTop: Float,
        rightLimit: Float
    ) {
        val rect = textParam.textpoint ?: return
        val text = getTextContent(textParam, config)
        if (text.isEmpty()) return

        val paint = fontManager.getTextPaint(textParam, scale)
        val gravity = textParam.textgravity

        // Calculate position based on gravity
        val x = when {
            gravity == "start" || gravity == "left" -> rect.left * scale
            gravity == "end" || gravity == "right" -> rect.right * scale
            gravity == "center" -> (rect.left + rect.right) / 2f * scale
            else -> rect.left * scale
        }

        // Calculate Y position with bar offset
        val y = when {
            VivoGravity.hasCenterVertical(gravity) -> {
                val rectHeight = (rect.bottom - rect.top) * scale
                val centerY = ((rect.top + rect.bottom) / 2f * scale) + barTop
                centerY + (paint.ascent() + paint.descent()) / 2f
            }
            else -> (rect.bottom * scale) + barTop - paint.descent()
        }

        // Apply text alignment
        paint.textAlign = when (gravity) {
            "start", "left" -> Paint.Align.LEFT
            "end", "right" -> Paint.Align.RIGHT
            "center" -> Paint.Align.CENTER
            else -> Paint.Align.LEFT
        }

        canvas.drawText(text, x, y, paint)
    }

    /**
     * Get text content based on text type
     */
    private fun getTextContent(textParam: VivoTextParam, config: VivoRenderConfig): String {
        // Return custom text if specified
        if (textParam.iscustomtext == 1 && textParam.text.isNotEmpty()) {
            return textParam.text
        }

        // Generate text based on type
        return when (textParam.texttype) {
            VivoTextType.DEVICE_NAME -> config.deviceName ?: textParam.text
            VivoTextType.TIME, VivoTextType.DATE, VivoTextType.TIME_ALT -> {
                config.timeText ?: textParam.text
            }
            VivoTextType.LOCATION -> config.locationText ?: textParam.text
            VivoTextType.FOCAL_LENGTH -> {
                parseLensInfo(config.lensInfo, textParam.texttype) ?: textParam.text
            }
            VivoTextType.APERTURE -> {
                parseLensInfo(config.lensInfo, textParam.texttype) ?: textParam.text
            }
            VivoTextType.SHUTTER -> {
                parseLensInfo(config.lensInfo, textParam.texttype) ?: textParam.text
            }
            VivoTextType.ISO -> {
                parseLensInfo(config.lensInfo, textParam.texttype) ?: textParam.text
            }
            VivoTextType.THREE_A_SINGLE -> {
                // Single 3A parameter display
                config.lensInfo?.split(" ")?.firstOrNull() ?: textParam.text
            }
            else -> textParam.text
        }
    }

    /**
     * Parse lens info to extract specific 3A parameter
     */
    private fun parseLensInfo(lensInfo: String?, type: Int): String? {
        if (lensInfo == null) return null

        val parts = lensInfo.split(" ").filter { it.isNotEmpty() }
        return when (type) {
            VivoTextType.FOCAL_LENGTH -> parts.firstOrNull { it.contains("mm") }
            VivoTextType.APERTURE -> parts.firstOrNull { it.startsWith("f/") }
            VivoTextType.SHUTTER -> parts.firstOrNull { it.contains("1/") }
            VivoTextType.ISO -> parts.firstOrNull { it.startsWith("ISO", ignoreCase = true) }
            else -> null
        }
    }

    /**
     * Load image from cache or assets
     */
    private fun loadImage(imageName: String): Bitmap? {
        if (imageName.isEmpty()) return null

        // Check cache first
        val cached = imageCache[imageName]
        if (cached != null) return cached

        // Try load
        var bitmap: Bitmap? = null

        // Try multiple paths for the image
        val paths = listOf(
            "vivo_watermark_full2/assets/zeiss_editors/$imageName",
            "watermark/vivo/logos/$imageName",
            "watermark/vivo/frames/$imageName",
            "watermark/vivo/CameraWmElement/$imageName"
        )

        for (path in paths) {
            bitmap = tryLoadImage(path)
            if (bitmap != null) break
        }

        if (bitmap == null) {
            Log.w(tag, "Failed to load image: $imageName")
            return null
        }

        // Cache and return
        imageCache[imageName] = bitmap
        return bitmap
    }

    /**
     * Try to load image from assets
     */
    private fun tryLoadImage(path: String): Bitmap? {
        return try {
            val inputStream = context.assets.open(path)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clear cache
     */
    fun clearCache() {
        imageCache.values.forEach { it.recycle() }
        imageCache.clear()
        fontManager.clearCache()
    }
}

/**
 * Render configuration for VIVO watermarks
 */
data class VivoRenderConfig(
    val deviceName: String? = null,
    val timeText: String? = null,
    val locationText: String? = null,
    val lensInfo: String? = null
)
