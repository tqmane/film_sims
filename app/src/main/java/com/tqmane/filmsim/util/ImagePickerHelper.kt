package com.tqmane.filmsim.util

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Backward-compatible image picker.
 *
 * | API level    | Strategy                         |
 * |-------------|----------------------------------|
 * | 33+ (13)    | PickVisualMedia (Photo Picker)   |
 * | 30–32 (11–12) | SAF ACTION_OPEN_DOCUMENT       |
 * | 26–29 (8–10)  | Legacy ACTION_PICK             |
 *
 * Register in `onCreate()` via [register], then call [launch] to open the picker.
 */
class ImagePickerHelper(
    private val activity: ComponentActivity,
    private val onImagePicked: (Uri) -> Unit
) {

    private var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var safLauncher: ActivityResultLauncher<Intent>? = null
    private var legacyLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * Must be called in [ComponentActivity.onCreate] BEFORE the activity is STARTED.
     */
    fun register() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: AndroidX Photo Picker
            photoPickerLauncher = activity.registerForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri -> uri?.let(onImagePicked) }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30–32: Storage Access Framework
            safLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                result.data?.data?.let(onImagePicked)
            }
        } else {
            // API 26–29: Legacy ACTION_PICK
            legacyLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                result.data?.data?.let(onImagePicked)
            }
        }
    }

    /**
     * Open the image picker.
     */
    fun launch() {
        when {
            photoPickerLauncher != null -> {
                photoPickerLauncher!!.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            safLauncher != null -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                safLauncher!!.launch(intent)
            }
            legacyLauncher != null -> {
                @Suppress("DEPRECATION")
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                legacyLauncher!!.launch(intent)
            }
        }
    }
}
