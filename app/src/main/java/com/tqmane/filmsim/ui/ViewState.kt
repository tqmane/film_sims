package com.tqmane.filmsim.ui

import android.graphics.Bitmap
import android.net.Uri
import com.tqmane.filmsim.util.CubeLUT
import com.tqmane.filmsim.util.ReleaseInfo
import com.tqmane.filmsim.util.WatermarkProcessor

/**
 * Sealed-class hierarchy representing the UI state of the main screen.
 */
sealed class ViewState {
    /** No image loaded – show the placeholder. */
    data object Empty : ViewState()

    /** Image is being decoded / processed. */
    data object Loading : ViewState()

    /** Image loaded and ready.  Holds all mutable editing state. */
    data class Ready(
        val originalUri: Uri,
        val originalBitmap: Bitmap,
        val previewBitmap: Bitmap,
        val thumbnailBitmap: Bitmap,
        val previewBitmapCopy: Bitmap?,
        val imageDimensions: String, // e.g. "4000x3000"
        val previewDimensions: String
    ) : ViewState()

    /** An error occurred while loading or processing. */
    data class Error(val message: String) : ViewState()
}

/**
 * Holds all LUT / effect editing state (independent of image lifecycle).
 */
data class EditState(
    val currentLutPath: String? = null,
    val currentLut: CubeLUT? = null,
    val intensity: Float = 1f,
    val grainEnabled: Boolean = false,
    val grainIntensity: Float = 0.5f,
    val grainStyle: String = "Xiaomi",
    val hasSelectedLut: Boolean = false
)

/**
 * Holds watermark editing state.
 */
data class WatermarkState(
    val style: WatermarkProcessor.WatermarkStyle = WatermarkProcessor.WatermarkStyle.NONE,
    val brandName: String = "",
    val deviceName: String = "",
    val timeText: String = "",
    val locationText: String = "",
    val lensInfo: String = ""
)

/**
 * One-shot events sent from ViewModel to UI.
 */
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowUpdateDialog(val release: ReleaseInfo) : UiEvent()
    data class ImageSaved(val width: Int, val height: Int, val path: String, val filename: String) : UiEvent()
}
