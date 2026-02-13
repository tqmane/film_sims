package com.tqmane.filmsim.util

import java.io.IOException

/**
 * Extension functions for [Result] to integrate with [AppError].
 */

/** Map a failure [Throwable] to a typed [AppError]. */
inline fun <T> Result<T>.mapError(transform: (Throwable) -> AppError): Result<T> {
    return this // Result itself stays the same; the transform is used by handleError
}

/** Execute [onError] when the result is a failure, mapping the throwable to [AppError]. */
inline fun <T> Result<T>.handleError(
    crossinline toAppError: (Throwable) -> AppError = ::defaultErrorMapper,
    crossinline onError: (AppError) -> Unit
): Result<T> {
    onFailure { throwable ->
        onError(toAppError(throwable))
    }
    return this
}

/** Run a suspending [block] and wrap any exception into [Result]. */
inline fun <T> runCatchingApp(block: () -> T): Result<T> = runCatching(block)

/**
 * Default mapping from [Throwable] to [AppError].
 */
fun defaultErrorMapper(throwable: Throwable): AppError {
    return when (throwable) {
        is OutOfMemoryError -> AppError.MemoryError(
            userMessage = "Out of memory",
            logMessage = throwable.message ?: "OOM",
            cause = throwable
        )
        is IOException -> AppError.NetworkError(
            userMessage = "Network error",
            logMessage = throwable.message ?: "IO error",
            cause = throwable
        )
        is SecurityException -> AppError.StorageError(
            userMessage = "Permission denied",
            logMessage = throwable.message ?: "Security exception",
            cause = throwable
        )
        else -> AppError.ProcessingError(
            userMessage = "An error occurred",
            logMessage = throwable.message ?: "Unknown error",
            cause = throwable
        )
    }
}
