package com.tqmane.filmsim.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Backward-compatible runtime permission helper.
 *
 * | API level      | Required permission(s)          |
 * |---------------|--------------------------------|
 * | 33+ (13)      | READ_MEDIA_IMAGES              |
 * | 29–32 (10–12) | READ_EXTERNAL_STORAGE          |
 * | 26–28 (8–9)   | READ_ & WRITE_EXTERNAL_STORAGE |
 *
 * Register in `onCreate()` via [register], then call [ensurePermissions].
 */
class PermissionHelper(
    private val activity: ComponentActivity,
    private val onGranted: () -> Unit,
    private val onDenied: () -> Unit
) {

    private lateinit var launcher: ActivityResultLauncher<Array<String>>

    /**
     * Call from [ComponentActivity.onCreate].
     */
    fun register() {
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (results.values.all { it }) onGranted() else onDenied()
        }
    }

    /**
     * Check current permissions; request if not yet granted.
     * If already granted, [onGranted] is invoked immediately.
     */
    fun ensurePermissions() {
        val needed = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            onGranted()
        } else {
            launcher.launch(needed.toTypedArray())
        }
    }

    /**
     * Returns `true` if all required permissions are currently granted.
     */
    fun hasPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requiredPermissions(): List<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            // Scoped storage – READ is enough; WRITE goes through MediaStore.
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        else -> {
            // API 26–28
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
}
