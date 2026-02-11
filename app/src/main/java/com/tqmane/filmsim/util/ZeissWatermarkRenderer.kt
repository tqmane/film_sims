package com.tqmane.filmsim.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ZEISS/VIVO Watermark Renderer — Accurate Template-Driven Implementation
 *
 * Renders watermarks from parsed VivoWatermarkTemplate configs.
 * Supports adaptive (bar below photo) and fixed (frame overlay) modes.
 *
 * Template coordinate system:
 *   - All coordinates in dp at base 360dp width (template 1080px = 360dp)
 *   - Adaptive mode: border area is below the photo content path
 *   - Fixed mode: photo is placed inside the path rect within the full template
 *
 * Subgroup selection logic:
 *   The template's second group (right side) contains multiple subgroups for different
 *   data availability scenarios. We pick the best match based on what data is available
 *   (3A info, time, location, blueImage).
 */
class ZeissWatermarkRenderer(private val context: Context) {

    companion object {
        private const val TAG = "ZeissWmRenderer"
        // Template coordinate base: 1080px = 360dp
        private const val TEMPLATE_DPI = 3f
    }

    private val fontManager = VivoFontManager(context)

    /**
     * Main render entry point.
     */
    fun render(
        source: Bitmap,
        template: VivoWatermarkTemplate,
        config: VivoRenderConfig
    ): Bitmap {
        return try {
            val frame = template.frame
            if (frame.isadaptive) {
                renderAdaptive(source, template, config)
            } else if (frame.isfixed) {
                renderFixed(source, template, config)
            } else {
                renderAdaptive(source, template, config)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Render failed", e)
            source
        }
    }

    // ========== ADAPTIVE MODE ==========
    // Photo on top, white bar below. Bar height derived from template path.

    private fun renderAdaptive(
        source: Bitmap,
        template: VivoWatermarkTemplate,
        config: VivoRenderConfig
    ): Bitmap {
        val frame = template.frame
        val imgW = source.width
        val imgH = source.height

        // Get content rect from path (photo area within template)
        val contentBottom = template.paths.firstOrNull()?.points?.getOrNull(2)?.y ?: 1395f
        val tmplW = frame.templatewidth.toFloat()
        val tmplH = frame.templateheight.toFloat()

        // Bar height in template dp (area below photo)
        val barDp = (tmplH - contentBottom) / TEMPLATE_DPI

        // Scale: 1 dp = (imgW / (tmplW/3)) px = imgW * 3 / tmplW
        val dp = imgW.toFloat() * TEMPLATE_DPI / tmplW
        val barHPx = (barDp * dp).roundToInt()
        val barHF = barDp * dp

        val totalH = imgH + barHPx
        val result = Bitmap.createBitmap(imgW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw photo
        canvas.drawBitmap(source, 0f, 0f, null)

        // Draw bar background (basecolor, typically white = -65794 → #FFFFFE)
        val barColor = frame.basecolor
        val barPaint = Paint().apply { color = barColor; style = Paint.Style.FILL }
        canvas.drawRect(0f, imgH.toFloat(), imgW.toFloat(), totalH.toFloat(), barPaint)

        val barTop = imgH.toFloat()
        val barCY = barTop + barHF / 2f

        // Render groups
        val groups = template.groups
        for (group in groups) {
            val subgroup = selectSubgroup(group, config)
            if (subgroup != null) {
                renderSubgroupAdaptive(canvas, subgroup, frame, dp, barTop, barCY, barHF, imgW.toFloat(), config)
            }
        }

        return result
    }

    private fun renderSubgroupAdaptive(
        canvas: Canvas,
        subgroup: VivoSubgroup,
        frame: VivoFrameConfig,
        dp: Float,
        barTop: Float,
        barCY: Float,
        barH: Float,
        imgW: Float,
        config: VivoRenderConfig
    ) {
        val marginStartDp = frame.marginstart
        val marginEndDp = frame.marginend
        val marginStartPx = marginStartDp * dp
        val marginEndPx = marginEndDp * dp

        // Calculate line heights for vertical centering
        val lineMetrics = mutableMapOf<Int, LineMetrics>()
        for (line in subgroup.lines) {
            val lineNum = getLineNum(line)
            // Compute line height from all elements
            var maxTop = Float.MAX_VALUE
            var maxBottom = Float.MIN_VALUE
            for (text in line.texts) {
                val rect = text.textpoint ?: continue
                maxTop = min(maxTop, rect.top)
                maxBottom = max(maxBottom, rect.bottom)
            }
            for (img in line.images) {
                val rect = img.picpoint ?: continue
                maxTop = min(maxTop, rect.top)
                maxBottom = max(maxBottom, rect.bottom)
            }
            if (maxTop != Float.MAX_VALUE) {
                lineMetrics[lineNum] = LineMetrics(maxTop, maxBottom, line.linemarginbottom)
            }
        }

        // Compute total height of all lines for vertical centering
        val sortedLineNums = lineMetrics.keys.sorted()
        var totalLineDpHeight = 0f
        for ((i, num) in sortedLineNums.withIndex()) {
            val metrics = lineMetrics[num]!!
            totalLineDpHeight += (metrics.bottom - metrics.top)
            if (i < sortedLineNums.size - 1) {
                totalLineDpHeight += metrics.marginBottom
            }
        }

        // Y offset to vertically center lines in bar
        val barCenterDp = barH / dp / 2f
        val totalHalfDp = totalLineDpHeight / 2f

        // Calculate each line's Y base offset
        val lineYBases = mutableMapOf<Int, Float>()
        var runningY = barCenterDp - totalHalfDp
        for (num in sortedLineNums) {
            val metrics = lineMetrics[num]!!
            lineYBases[num] = runningY - metrics.top  // offset so metrics.top aligns to runningY
            runningY += (metrics.bottom - metrics.top) + metrics.marginBottom
        }

        // Now render each line
        for (line in subgroup.lines) {
            val lineNum = getLineNum(line)
            val yBase = lineYBases[lineNum] ?: 0f

            // Render images in this line
            for (img in line.images) {
                renderImageAdaptive(canvas, img, dp, barTop, yBase, marginStartPx, marginEndPx, imgW, config)
            }

            // Render texts in this line
            for (text in line.texts) {
                renderTextAdaptive(canvas, text, dp, barTop, yBase, marginStartPx, marginEndPx, imgW, config)
            }
        }
    }

    private fun renderImageAdaptive(
        canvas: Canvas,
        img: VivoImageParam,
        dp: Float,
        barTop: Float,
        yBase: Float,
        marginStartPx: Float,
        marginEndPx: Float,
        imgW: Float,
        config: VivoRenderConfig
    ) {
        val rect = img.picpoint ?: return
        val imageName = img.pic
        if (imageName.isEmpty()) return

        val bmp = loadImage(imageName) ?: run {
            // For dividers, draw a rect
            if (imageName.contains("divider") || img.isforcedrawdivider) {
                drawDivider(canvas, rect, dp, barTop, yBase, marginStartPx, marginEndPx, imgW, img)
            }
            return
        }

        // Calculate position
        val x: Float
        val w = (rect.right - rect.left) * dp
        val h = (rect.bottom - rect.top) * dp

        when (img.picgravity) {
            "end" -> {
                x = imgW - marginEndPx - (360f - rect.right) * dp
            }
            "center" -> {
                x = imgW / 2f - w / 2f + (rect.left - (360f - rect.right - rect.left) / 2f) * dp * 0f
                // Center gravity: use template center
                val templateCenterX = (rect.left + rect.right) / 2f
                val cx = templateCenterX * dp
                val drawLeft = cx - w / 2f
                val drawTop = barTop + (yBase + rect.top) * dp
                val dstRect = RectF(drawLeft, drawTop, drawLeft + w, drawTop + h)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bmp, null, dstRect, paint)
                bmp.recycle()
                return
            }
            else -> { // "start"
                x = marginStartPx + rect.left * dp + img.picmarginstart * dp
            }
        }

        val y = barTop + (yBase + rect.top) * dp
        val dstRect = RectF(x, y, x + w, y + h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bmp, null, dstRect, paint)
        bmp.recycle()
    }

    private fun drawDivider(
        canvas: Canvas,
        rect: VivoRect,
        dp: Float,
        barTop: Float,
        yBase: Float,
        marginStartPx: Float,
        marginEndPx: Float,
        imgW: Float,
        img: VivoImageParam
    ) {
        val w = max(1f, (rect.right - rect.left) * dp)
        val h = (rect.bottom - rect.top) * dp

        val x = when (img.picgravity) {
            "end" -> imgW - marginEndPx - (360f - rect.right) * dp
            "center" -> imgW / 2f - w / 2f
            else -> marginStartPx + rect.left * dp
        }
        val y = barTop + (yBase + rect.top) * dp

        val color = if (img.pic.contains("black")) Color.BLACK else Color.parseColor("#757575")
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawRect(x, y, x + w, y + h, paint)
    }

    private fun renderTextAdaptive(
        canvas: Canvas,
        text: VivoTextParam,
        dp: Float,
        barTop: Float,
        yBase: Float,
        marginStartPx: Float,
        marginEndPx: Float,
        imgW: Float,
        config: VivoRenderConfig
    ) {
        val rect = text.textpoint ?: return
        val content = getTextContent(text, config) ?: return
        if (content.isEmpty()) return

        val paint = createTextPaint(text, dp)

        val x: Float
        when (text.textgravity) {
            "end" -> {
                paint.textAlign = Paint.Align.RIGHT
                x = imgW - marginEndPx
            }
            "center" -> {
                paint.textAlign = Paint.Align.CENTER
                x = imgW / 2f
            }
            else -> { // "start"
                paint.textAlign = Paint.Align.LEFT
                x = marginStartPx + rect.left * dp + text.textmarginstart * dp
            }
        }

        // Vertical position: center text within rect bounds
        val rectTop = barTop + (yBase + rect.top) * dp
        val rectBottom = barTop + (yBase + rect.bottom) * dp
        val rectCenterY = (rectTop + rectBottom) / 2f
        val textY = rectCenterY - (paint.ascent() + paint.descent()) / 2f

        canvas.drawText(content, x, textY, paint)
    }

    // ========== FIXED MODE ==========
    // Photo placed inside frame template at path coordinates.

    private fun renderFixed(
        source: Bitmap,
        template: VivoWatermarkTemplate,
        config: VivoRenderConfig
    ): Bitmap {
        val frame = template.frame
        val tmplW = frame.templatewidth
        val tmplH = frame.templateheight

        // Path defines where photo goes within template
        val path = template.paths.firstOrNull()?.points ?: return source
        val photoLeft = path[0].x.roundToInt()
        val photoTop = path[0].y.roundToInt()
        val photoRight = path[2].x.roundToInt()
        val photoBottom = path[2].y.roundToInt()

        val photoW = photoRight - photoLeft
        val photoH = photoBottom - photoTop

        // Scale factor: how much to scale source to fit photo area
        val scaleX = source.width.toFloat() / photoW
        val scaleY = source.height.toFloat() / photoH
        val scale = min(scaleX, scaleY) // maintain aspect ratio

        val realW = (tmplW * scale).roundToInt()
        val realH = (tmplH * scale).roundToInt()
        val dp = scale  // 1 template pixel = scale real pixels

        val result = Bitmap.createBitmap(realW, realH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Fill background
        val bgPaint = Paint().apply { color = frame.basecolor; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, realW.toFloat(), realH.toFloat(), bgPaint)

        // Draw baseboard frame if available
        loadImage(frame.baseboard)?.let { frameBmp ->
            canvas.drawBitmap(frameBmp, null, Rect(0, 0, realW, realH),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            frameBmp.recycle()
        }

        // Draw photo into the path area
        val destLeft = (photoLeft * scale).roundToInt()
        val destTop = (photoTop * scale).roundToInt()
        val destRight = (photoRight * scale).roundToInt()
        val destBottom = (photoBottom * scale).roundToInt()
        canvas.drawBitmap(source, null, Rect(destLeft, destTop, destRight, destBottom),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        // Render groups: coordinates are in template pixel space
        for (group in template.groups) {
            val subgroup = selectSubgroup(group, config)
            if (subgroup != null) {
                renderSubgroupFixed(canvas, subgroup, frame, scale, realW.toFloat(), realH.toFloat(), config)
            }
        }

        return result
    }

    private fun renderSubgroupFixed(
        canvas: Canvas,
        subgroup: VivoSubgroup,
        frame: VivoFrameConfig,
        scale: Float,
        canvasW: Float,
        canvasH: Float,
        config: VivoRenderConfig
    ) {
        // In fixed mode, coordinates are in template dp (360 base)
        // dp = scale (template px → real px), template dp = template px / 3
        val dp = scale * TEMPLATE_DPI  // template dp → real px

        for (line in subgroup.lines) {
            for (img in line.images) {
                renderImageFixed(canvas, img, dp, scale, canvasW, canvasH, config)
            }
            for (text in line.texts) {
                renderTextFixed(canvas, text, dp, scale, canvasW, canvasH, config)
            }
        }
    }

    private fun renderImageFixed(
        canvas: Canvas,
        img: VivoImageParam,
        dp: Float,
        scale: Float,
        canvasW: Float,
        canvasH: Float,
        config: VivoRenderConfig
    ) {
        val rect = img.picpoint ?: return
        val imageName = img.pic
        if (imageName.isEmpty()) return

        // Fixed mode: coordinates in dp (360 base), convert to real px
        val left = rect.left * dp
        val top = rect.top * dp
        val w = (rect.right - rect.left) * dp
        val h = (rect.bottom - rect.top) * dp

        val bmp = loadImage(imageName)
        if (bmp == null) {
            // Draw divider rect
            if (imageName.contains("divider") || img.isforcedrawdivider) {
                val color = if (imageName.contains("black")) Color.BLACK else Color.parseColor("#757575")
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color; style = Paint.Style.FILL
                }
                val x = when (img.picgravity) {
                    "center" -> canvasW / 2f - max(1f, w) / 2f + left - (canvasW / 2f - max(1f, w) / 2f)
                    else -> left
                }
                canvas.drawRect(left, top, left + max(1f, w), top + h, paint)
            }
            return
        }

        val x = when (img.picgravity) {
            "center" -> {
                // Center the image at the template's center position
                val templateCenterX = (rect.left + rect.right) / 2f * dp
                templateCenterX - w / 2f
            }
            else -> left
        }

        val dstRect = RectF(x, top, x + w, top + h)
        canvas.drawBitmap(bmp, null, dstRect,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        bmp.recycle()
    }

    private fun renderTextFixed(
        canvas: Canvas,
        text: VivoTextParam,
        dp: Float,
        scale: Float,
        canvasW: Float,
        canvasH: Float,
        config: VivoRenderConfig
    ) {
        val rect = text.textpoint ?: return
        val content = getTextContent(text, config) ?: return
        if (content.isEmpty()) return

        val paint = createTextPaint(text, dp)

        val x: Float
        when (text.textgravity) {
            "center" -> {
                paint.textAlign = Paint.Align.CENTER
                x = canvasW / 2f
            }
            "end" -> {
                paint.textAlign = Paint.Align.RIGHT
                x = canvasW - rect.left * dp  // mirror from right
            }
            else -> {
                paint.textAlign = Paint.Align.LEFT
                x = rect.left * dp + text.textmarginstart * dp
            }
        }

        // Vertical center within rect
        val rectTop = rect.top * dp
        val rectBottom = rect.bottom * dp
        val rectCY = (rectTop + rectBottom) / 2f
        val textY = rectCY - (paint.ascent() + paint.descent()) / 2f

        canvas.drawText(content, x, textY, paint)
    }

    // ========== SUBGROUP SELECTION ==========

    /**
     * Select the appropriate subgroup based on available data.
     *
     * zeiss7 has extensive subgroups (0-11) in group[1]:
     *   0: 3A + time + location (default)
     *   1: 3A only
     *   2: location only
     *   3: time only
     *   4: location + time (no 3A)
     *   5: 3A + time
     *   6: 3A + location
     *   7: no params
     *   8-11: blueImage variants
     *
     * For group[0] (left side), always use subgroup 0 (default).
     * For simpler templates (zeiss0-6), use subgroup 0.
     */
    private fun selectSubgroup(
        group: VivoParamGroup,
        config: VivoRenderConfig
    ): VivoSubgroup? {
        if (group.subgroups.isEmpty()) return null

        // If only one subgroup, use it
        if (group.subgroups.size == 1) return group.subgroups[0]

        val has3A = !config.lensInfo.isNullOrEmpty()
        val hasTime = !config.timeText.isNullOrEmpty()
        val hasLoc = !config.locationText.isNullOrEmpty()

        // For group with groupgravity=center_vertical containing left-side elements
        // (logo, device name, zeiss logo), use subgroup 0 if visible
        val defaultSub = group.subgroups.find { it.subgroupnum == 0 }

        // Check if this is a right-side group with multiple visibility states
        val hasVisibilityVariants = group.subgroups.count { !it.subgroupvisible } > 1

        if (!hasVisibilityVariants) {
            // Simple group: first visible subgroup
            return group.subgroups.find { it.subgroupvisible } ?: defaultSub
        }

        // Right-side group: select based on available data
        // Map of subgroupnum to what data it shows (derived from template analysis)
        val subMap = group.subgroups.associateBy { it.subgroupnum }

        // Determine the best subgroup based on available data
        return when {
            has3A && hasTime && hasLoc -> subMap[0]  // Full: 3A + time + location
            has3A && hasTime -> subMap[5] ?: subMap[0]  // 3A + time
            has3A && hasLoc -> subMap[6] ?: subMap[0]   // 3A + location
            has3A -> subMap[1] ?: subMap[0]              // 3A only
            hasTime && hasLoc -> subMap[4] ?: subMap[0]  // time + location
            hasTime -> subMap[3] ?: subMap[0]            // time only
            hasLoc -> subMap[2] ?: subMap[0]             // location only
            else -> subMap[7] ?: subMap[0]               // no params
        } ?: defaultSub
    }

    // ========== TEXT CONTENT RESOLUTION ==========

    /**
     * Resolve text content based on texttype.
     *
     * texttype values (from template analysis):
     *   0: static text (e.g. "ZEISS") — use text as-is
     *   1: device/model name (e.g. "vivo X90 Pro")
     *   2: focal length (e.g. "24mm")
     *   3: aperture (e.g. "f/1.9")
     *   4: shutter speed (e.g. "1/125")
     *   5: ISO (e.g. "ISO50")
     *   6: datetime (e.g. "2022.7.13 14:19")
     *   7: location (e.g. "江苏 南京")
     *   10: 3A combined info string
     *   13: "ZEISS" text (or device+ZEISS)
     *   14: device name (for humanity style)
     */
    private fun getTextContent(text: VivoTextParam, config: VivoRenderConfig): String? {
        return when (text.texttype) {
            0 -> text.text  // Static text
            1 -> config.deviceName ?: text.text  // Device name
            2 -> { // Focal length
                val parts = splitLensInfo(config.lensInfo)
                parts?.getOrNull(0) ?: text.text
            }
            3 -> { // Aperture
                val parts = splitLensInfo(config.lensInfo)
                parts?.getOrNull(1) ?: text.text
            }
            4 -> { // Shutter speed
                val parts = splitLensInfo(config.lensInfo)
                parts?.getOrNull(2) ?: text.text
            }
            5 -> { // ISO
                val parts = splitLensInfo(config.lensInfo)
                parts?.getOrNull(3) ?: text.text
            }
            6 -> config.timeText ?: text.text  // Datetime
            7 -> config.locationText ?: text.text  // Location
            10 -> config.lensInfo ?: text.text  // Combined 3A string
            13 -> text.text  // "ZEISS" static
            14 -> { // Device name for humanity (model | ZEISS)
                val dev = config.deviceName
                if (dev != null && dev.isNotEmpty()) "$dev | ZEISS" else "ZEISS"
            }
            else -> text.text
        }
    }

    /**
     * Split lens info into [focal, aperture, shutter, iso].
     * Expected input formats: "24mm f/1.9 1/125 ISO50" or "24mm  f/1.9  1/125  ISO50"
     */
    private fun splitLensInfo(lensInfo: String?): List<String>? {
        if (lensInfo.isNullOrEmpty()) return null
        val parts = lensInfo.split(Regex("\\s{2,}|\\s+")).filter { it.isNotEmpty() }
        return if (parts.size >= 2) parts else null
    }

    // ========== IMAGE LOADING ==========

    private fun loadImage(imageName: String): Bitmap? {
        if (imageName.isEmpty()) return null
        val searchPaths = listOf(
            "watermark/vivo/logos/$imageName",
            "watermark/vivo/frames/$imageName",
            "vivo_watermark_full2/assets/zeiss_editors/$imageName",
            "vivo_watermark_full2/assets/CameraWmElement/$imageName",
            "vivo_watermark_full2/assets/CameraWmElement copy/$imageName"
        )
        for (path in searchPaths) {
            try {
                context.assets.open(path).use { stream ->
                    return BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }
        return null
    }

    // ========== PAINT CREATION ==========

    private fun createTextPaint(text: VivoTextParam, dp: Float): Paint {
        val typeface = fontManager.getTypeface(text.typeface)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = text.textsize * dp
            color = parseColor(text.textcolor)
            letterSpacing = text.letterspacing
        }

        // Apply font weight via Paint if API level supports it
        try {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                paint.typeface = android.graphics.Typeface.create(typeface, text.textfontweight, false)
            }
        } catch (_: Exception) {
            // Fallback: use bold style for weight >= 700
            if (text.textfontweight >= 700) {
                paint.isFakeBoldText = true
            }
        }

        return paint
    }

    private fun parseColor(colorStr: String): Int {
        return try {
            when {
                colorStr.startsWith("#") -> {
                    when (colorStr.length) {
                        9 -> { // #AARRGGBB
                            val alpha = colorStr.substring(1, 3).toLong(16).toInt()
                            val rgb = colorStr.substring(3).toLong(16).toInt()
                            Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
                        }
                        7 -> Color.parseColor(colorStr)  // #RRGGBB
                        else -> Color.BLACK
                    }
                }
                else -> Color.BLACK
            }
        } catch (_: Exception) {
            Color.BLACK
        }
    }

    // ========== HELPER CLASSES ==========

    private data class LineMetrics(
        val top: Float,
        val bottom: Float,
        val marginBottom: Float
    )

    private fun getLineNum(line: VivoLine): Int {
        // Get line number from the first element
        val textLineNum = line.texts.firstOrNull()?.linenum
        val imgLineNum = line.images.firstOrNull()?.piclinenum
        return textLineNum ?: imgLineNum ?: 0
    }
}
