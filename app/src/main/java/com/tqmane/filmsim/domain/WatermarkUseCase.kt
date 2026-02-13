package com.tqmane.filmsim.domain

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.tqmane.filmsim.util.CubeLUT
import com.tqmane.filmsim.util.HighResLutProcessor
import com.tqmane.filmsim.util.LutBitmapProcessor
import com.tqmane.filmsim.util.WatermarkProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use-case for watermark rendering and high-res export/save.
 */
class WatermarkUseCase @Inject constructor() {

    /**
     * Generate a watermark preview on top of a preview bitmap.
     * Applies the current LUT at [intensity] before adding the watermark
     * so the preview matches the GL view.
     */
    suspend fun renderPreview(
        context: Context,
        previewBitmap: Bitmap,
        lut: CubeLUT?,
        intensity: Float,
        config: WatermarkProcessor.WatermarkConfig
    ): Bitmap = withContext(Dispatchers.Default) {
        val base = if (lut != null && intensity > 0f) {
            LutBitmapProcessor.applyLutToBitmap(previewBitmap, lut, intensity)
        } else {
            previewBitmap
        }
        WatermarkProcessor.applyWatermark(context, base, config)
    }

    /**
     * Apply watermark to an already-processed export bitmap.
     */
    suspend fun applyWatermark(
        context: Context,
        bitmap: Bitmap,
        config: WatermarkProcessor.WatermarkConfig
    ): Bitmap = withContext(Dispatchers.Default) {
        WatermarkProcessor.applyWatermark(context, bitmap, config)
    }

    /**
     * CPU-based LUT application (fallback when GPU export fails).
     */
    suspend fun applyCpuLut(
        source: Bitmap,
        lut: CubeLUT,
        intensity: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        HighResLutProcessor.applyLut(source, lut, intensity)
    }

    /**
     * Save a bitmap to MediaStore with EXIF preservation.
     */
    suspend fun saveBitmapWithExif(
        context: Context,
        bitmap: Bitmap,
        originalUri: Uri?,
        savePath: String,
        quality: Int
    ): SaveResult = withContext(Dispatchers.IO) {
        val filename = "FilmSim_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, savePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream))
                throw IllegalStateException("Bitmap compress failed")
        } ?: throw IllegalStateException("Failed to open output stream")

        // Copy EXIF data
        copyExif(context, originalUri, resolver, uri)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        SaveResult(bitmap.width, bitmap.height, savePath, filename)
    }

    data class SaveResult(val width: Int, val height: Int, val path: String, val filename: String)

    // ─── EXIF copy ──────────────────────────────────────

    private fun copyExif(
        context: Context,
        sourceUri: Uri?,
        resolver: android.content.ContentResolver,
        destUri: Uri
    ) {
        sourceUri ?: return
        try {
            val sourceExif = context.contentResolver.openInputStream(sourceUri)?.use { ExifInterface(it) }
                ?: return
            resolver.openFileDescriptor(destUri, "rw")?.use { pfd ->
                val dest = ExifInterface(pfd.fileDescriptor)
                EXIF_TAGS.forEach { tag ->
                    sourceExif.getAttribute(tag)?.let { dest.setAttribute(tag, it) }
                }
                dest.setAttribute(ExifInterface.TAG_SOFTWARE, "FilmSims LUT Editor")
                dest.saveAttributes()
            }
        } catch (_: Exception) { /* image saved; EXIF copy is best-effort */ }
    }

    companion object {
        private val EXIF_TAGS = arrayOf(
            ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED, ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL, ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_X_RESOLUTION, ExifInterface.TAG_Y_RESOLUTION,
            ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
            ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_FLASH,
            ExifInterface.TAG_METERING_MODE, ExifInterface.TAG_EXPOSURE_PROGRAM,
            ExifInterface.TAG_EXPOSURE_MODE, ExifInterface.TAG_SCENE_TYPE,
            ExifInterface.TAG_LENS_MAKE, ExifInterface.TAG_LENS_MODEL,
            ExifInterface.TAG_ARTIST, ExifInterface.TAG_COPYRIGHT,
            ExifInterface.TAG_SOFTWARE, ExifInterface.TAG_IMAGE_DESCRIPTION
        )
    }
}
