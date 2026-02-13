package com.tqmane.filmsim.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import com.tqmane.filmsim.util.CubeLUT
import com.tqmane.filmsim.util.ReleaseInfo
import com.tqmane.filmsim.util.WatermarkProcessor

// ─── LCE View State (sealed interface) ─────────────────

/** Main screen state following Loading-Content-Error pattern. */
sealed interface ViewState {
    /** No image loaded – show the placeholder. */
    data object Empty : ViewState
    /** Image is being decoded / processed. */
    data object Loading : ViewState
    /**
     * Image loaded and ready. Only preview-sized bitmaps are held in memory;
     * full-resolution is loaded on-demand during export.
     */
    data class Content(
        val originalUri: Uri,
        val previewBitmap: Bitmap,
        val thumbnailBitmap: Bitmap,
        val previewBitmapCopy: Bitmap?,
        val imageDimensions: String,
        val previewDimensions: String
    ) : ViewState
    /** An error occurred while loading or processing. */
    data class Error(val message: String) : ViewState
}

// ─── Edit State ─────────────────────────────────────────

/** LUT / effect editing state (independent of image lifecycle). */
data class EditState(
    val currentLutPath: String? = null,
    val currentLut: CubeLUT? = null,
    val intensity: Float = 1f,
    val grainEnabled: Boolean = false,
    val grainIntensity: Float = 0.5f,
    val grainStyle: String = "Xiaomi",
    val hasSelectedLut: Boolean = false
)

// ─── Watermark State ────────────────────────────────────

/** Watermark editing state. */
data class WatermarkState(
    val style: WatermarkProcessor.WatermarkStyle = WatermarkProcessor.WatermarkStyle.NONE,
    val brandName: String = "",
    val deviceName: String = "",
    val timeText: String = "",
    val locationText: String = "",
    val lensInfo: String = ""
)

// ─── One-shot UI Events ────────────────────────────────

/** One-shot events sent from ViewModel → UI (resource IDs to avoid Context in VM). */
sealed interface UiEvent {
    data class ShowToast(@StringRes val messageResId: Int, val formatArgs: Array<Any> = emptyArray()) : UiEvent
    data class ShowRawToast(val message: String) : UiEvent
    data class ShowUpdateDialog(val release: ReleaseInfo) : UiEvent
    data class ImageSaved(val width: Int, val height: Int, val path: String, val filename: String) : UiEvent
}
