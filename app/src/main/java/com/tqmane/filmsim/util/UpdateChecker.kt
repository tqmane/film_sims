package com.tqmane.filmsim.util

import android.content.Context
import android.util.Log
import com.tqmane.filmsim.BuildConfig
import com.tqmane.filmsim.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class ReleaseInfo(
    val tagName: String,
    val version: String,
    val releaseNotes: String,
    val htmlUrl: String
)

/**
 * Checks for application updates from GitHub Releases.
 *
 * - Uses a single shared [OkHttpClient] to avoid duplicate HTTP client instances.
 * - Retry with exponential back-off (max 3 attempts)
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val MAX_RETRIES = 3
    private const val INITIAL_BACKOFF_MS = 1_000L

    /**
     * Check for updates from GitHub Releases.
     * Returns [ReleaseInfo] if a newer version is available, null otherwise.
     *
     * @param client A pre-configured [OkHttpClient] with certificate pinning (from DI).
     */
    suspend fun checkForUpdate(context: Context, client: OkHttpClient, force: Boolean = false): ReleaseInfo? {
        val githubApiUrl = context.getString(R.string.github_api_url)

        // Show update dialog on every launch if an update is available (no interval gate).

        return withContext(Dispatchers.IO) {
            try {
                val body = executeWithRetry(client, githubApiUrl) ?: return@withContext null
                val json = JSONObject(body)

                val tagName = json.getString("tag_name")
                val version = tagName.removePrefix("v")
                val releaseNotes = json.optString("body", "")
                val htmlUrl = json.getString("html_url")

                val currentVersion = BuildConfig.VERSION_NAME
                if (isNewerVersion(version, currentVersion)) {
                    ReleaseInfo(tagName, version, releaseNotes, htmlUrl)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, classifyNetworkError(e), e)
                null
            }
        }
    }

    /**
     * Execute an HTTP GET with exponential back-off retry.
     * Returns the response body string, or null on failure.
     */
    private suspend fun executeWithRetry(client: OkHttpClient, url: String): String? {
        var lastException: Exception? = null
        var backoff = INITIAL_BACKOFF_MS

        for (attempt in 1..MAX_RETRIES) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    return response.body?.string()
                }
                // Non-retriable HTTP error (4xx)
                if (response.code in 400..499) {
                    Log.w(TAG, "HTTP ${response.code} – not retrying")
                    return null
                }
                // Server error (5xx) – retry
                Log.w(TAG, "HTTP ${response.code} – attempt $attempt/$MAX_RETRIES")
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "Attempt $attempt/$MAX_RETRIES failed: ${classifyNetworkError(e)}")
            }

            if (attempt < MAX_RETRIES) {
                delay(backoff)
                backoff *= 2  // exponential back-off
            }
        }

        lastException?.let { throw it }
        return null
    }



    /**
     * Compare two semantic versions (e.g., "1.0.1" vs "1.0.0").
     * Returns true if newVersion is greater than currentVersion.
     */
    internal fun isNewerVersion(newVersion: String, currentVersion: String): Boolean {
        try {
            val newParts = newVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(newParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val newPart = newParts.getOrElse(i) { 0 }
                val currentPart = currentParts.getOrElse(i) { 0 }
                if (newPart > currentPart) return true
                if (newPart < currentPart) return false
            }
            return false // Versions are equal
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Classify a network exception for structured logging.
     */
    private fun classifyNetworkError(e: Exception): String = when (e) {
        is UnknownHostException -> "DNS resolution failed: ${e.message}"
        is SocketTimeoutException -> "Connection timed out: ${e.message}"
        is javax.net.ssl.SSLPeerUnverifiedException -> "Certificate pinning verification failed: ${e.message}"
        is javax.net.ssl.SSLHandshakeException -> "SSL handshake failed: ${e.message}"
        is IOException -> "Network IO error: ${e.message}"
        else -> "Unexpected error: ${e.message}"
    }
}
