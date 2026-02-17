package com.tqmane.filmsim.util

import android.content.Context
import android.util.Log
import android.widget.Toast

/**
 * Centralised error handler for logging and user notification.
 */
object ErrorHandler {

    private const val TAG = "FilmSims"

    /**
     * Handle an [AppError]: log it and optionally show a Toast.
     */
    fun handle(context: Context?, error: AppError, showToast: Boolean = true) {
        // Always log
        when (error) {
            is AppError.NetworkError -> Log.e(TAG, "[Network] ${error.logMessage}", error.cause)
            is AppError.StorageError -> Log.e(TAG, "[Storage] ${error.logMessage}", error.cause)
            is AppError.ProcessingError -> Log.e(TAG, "[Processing] ${error.logMessage}", error.cause)
            is AppError.MemoryError -> Log.e(TAG, "[Memory] ${error.logMessage}", error.cause)
        }

        // Show user-facing message when requested
        if (showToast && context != null && error.userMessage.isNotBlank()) {
            Toast.makeText(context, error.userMessage, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Log-only variant (no user notification).
     */
    fun log(error: AppError) {
        handle(null, error, showToast = false)
    }
}
