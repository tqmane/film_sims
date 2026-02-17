package com.tqmane.filmsim.gl

import android.opengl.GLSurfaceView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Abstraction over OpenGL thread execution.
 * Converts [GLSurfaceView.queueEvent] callbacks into suspend functions,
 * eliminating the need for [java.util.concurrent.CountDownLatch].
 */
interface GlCommandExecutor {
    /** Execute [block] on the GL thread and suspend until it completes. */
    suspend fun <T> execute(block: () -> T): T
}

/**
 * Production implementation backed by a [GLSurfaceView].
 */
class GlSurfaceViewExecutor(
    private val glSurfaceView: GLSurfaceView
) : GlCommandExecutor {

    override suspend fun <T> execute(block: () -> T): T =
        suspendCancellableCoroutine { cont ->
            glSurfaceView.queueEvent {
                runCatching { block() }
                    .onSuccess { result -> cont.resume(result) }
                    .onFailure { error -> cont.resumeWithException(error) }
            }
        }
}
