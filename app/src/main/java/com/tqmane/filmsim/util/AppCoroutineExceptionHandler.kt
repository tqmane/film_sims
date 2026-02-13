package com.tqmane.filmsim.util

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Application-wide [CoroutineExceptionHandler] that logs unhandled exceptions
 * and optionally forwards to [ErrorHandler].
 */
object AppCoroutineExceptionHandler {

    private const val TAG = "FilmSims-Coroutine"

    /**
     * Creates a [CoroutineExceptionHandler] that logs exceptions.
     * Attach to top-level scopes (e.g. viewModelScope supervisor).
     */
    fun create(): CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Unhandled coroutine exception", throwable)
            ErrorHandler.log(defaultErrorMapper(throwable))
        }
}
