package com.tqmane.filmsim.domain

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.tqmane.filmsim.util.WatermarkProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use-case encapsulating all image-loading logic:
 *   - Decode bounds
 *   - Decode sampled preview & export bitmaps
 *   - Create LUT thumbnails
 *   - Extract EXIF metadata
 */
class ImageLoadUseCase @Inject constructor() {

    data class LoadResult(
        val exportBitmap: Bitmap,
        val previewBitmap: Bitmap,
        val thumbnailBitmap: Bitmap,
        val previewCopy: Bitmap?,
        val imageDimensions: String,
        val previewDimensions: String,
        val exifData: ExifData?
    )

    data class ExifData(
        val deviceName: String,
        val timeText: String,
        val lensInfo: String,
        val locationText: String
    )

    suspend fun load(
        contentResolver: ContentResolver,
        uri: Uri
    ): LoadResult = withContext(Dispatchers.IO) {
        val (origW, origH) = decodeImageBounds(contentResolver, uri) ?: (0 to 0)

        val previewBitmap = decodeSampledBitmap(contentResolver, uri, 10_000_000)
            ?: throw IllegalStateException("Failed to decode preview bitmap")

        val exportBitmap = decodeFullOrSampledBitmap(contentResolver, uri, 30_000_000)
            ?: throw IllegalStateException("Failed to decode export bitmap")

        // Thumbnail for LUT previews (500 px max dimension)
        val maxDim = 500
        val ratio = previewBitmap.width.toFloat() / previewBitmap.height.toFloat()
        val (thumbW, thumbH) = if (ratio > 1) {
            maxDim to (maxDim / ratio).toInt()
        } else {
            (maxDim * ratio).toInt() to maxDim
        }
        val thumbnail = Bitmap.createScaledBitmap(previewBitmap, thumbW, thumbH, true)
        val previewCopy = previewBitmap.copy(Bitmap.Config.ARGB_8888, false)

        val dims = if (origW > 0 && origH > 0) "${origW}x${origH}" else "${exportBitmap.width}x${exportBitmap.height}"
        val previewDims = "${previewBitmap.width}x${previewBitmap.height}"

        val exif = extractExif(contentResolver, uri)

        LoadResult(exportBitmap, previewBitmap, thumbnail, previewCopy, dims, previewDims, exif)
    }

    // ─── EXIF extraction ────────────────────────────────

    private fun extractExif(contentResolver: ContentResolver, uri: Uri): ExifData? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)

                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim() ?: ""
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim() ?: ""
                val deviceName = when {
                    make.isNotEmpty() && model.isNotEmpty() ->
                        if (model.startsWith(make, ignoreCase = true)) model else "$make $model"
                    else -> make + model
                }

                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                val timeStr = WatermarkProcessor.formatExifDateTime(dateTime)
                    ?: WatermarkProcessor.getDefaultTimeString()

                val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)
                    ?: exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { raw ->
                        val parts = raw.split("/")
                        if (parts.size == 2) String.format("%.0f", parts[0].toDouble() / parts[1].toDouble()) else raw
                    }
                val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { raw ->
                    val value = raw.toDoubleOrNull()
                    if (value != null && value < 1) "1/${(1.0 / value).toInt()}" else raw
                }
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                val lensInfo = WatermarkProcessor.buildLensInfoFromExif(focalLength, fNumber, exposureTime, iso)

                val latLong = exif.latLong
                val locationText = if (latLong != null) String.format("%.4f, %.4f", latLong[0], latLong[1]) else ""

                ExifData(deviceName, timeStr, lensInfo, locationText)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ─── Bitmap decode helpers ──────────────────────────

    private fun decodeImageBounds(cr: ContentResolver, uri: Uri): Pair<Int, Int>? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        return if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth to opts.outHeight else null
    }

    private fun decodeSampledBitmap(cr: ContentResolver, uri: Uri, maxPixels: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxPixels)
        }
        return cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun decodeFullOrSampledBitmap(cr: ContentResolver, uri: Uri, fallback: Int): Bitmap? {
        try {
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }?.let { return it }
        } catch (_: OutOfMemoryError) { /* fall through */ }
        return decodeSampledBitmap(cr, uri, fallback)
    }

    private fun calculateInSampleSize(w: Int, h: Int, maxPixels: Int): Int {
        var s = 1
        while ((w / s) * (h / s) > maxPixels) s *= 2
        return s.coerceAtLeast(1)
    }
}
