package com.tqmane.filmsim.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tqmane.filmsim.BuildConfig
import com.tqmane.filmsim.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,
    val version: String,
    val releaseNotes: String,
    val htmlUrl: String
)

/**
 * Checks for application updates from GitHub Releases.
 *
 * Security:
 * - Certificate pinning for api.github.com
 * - Retry with exponential back-off (max 3 attempts)
 *
 * HOW TO UPDATE CERTIFICATE PINS:
 * 1. openssl s_client -connect api.github.com:443 -servername api.github.com < /dev/null 2>/dev/null \
 *      | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der \
 *      | openssl dgst -sha256 -binary | openssl enc -base64
 * 2. Replace the sha256 value in [certificatePinner].
 * 3. Keep the previous pin as a backup during rotation.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val PREFS_NAME = "update_checker"
    private const val KEY_SKIP_VERSION = "skip_version"
    private const val KEY_LAST_CHECK = "last_check"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours

    private const val MAX_RETRIES = 3
    private const val INITIAL_BACKOFF_MS = 1_000L

    private val certificatePinner = CertificatePinner.Builder()
        // DigiCert Global Root G2 (current GitHub CA)
        .add("api.github.com", "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=")
        // DigiCert Global Root CA (backup / rotation)
        .add("api.github.com", "sha256/r/mIkG3eEpVdm+u/ko/cwxzOMo1bk4TyHIlByibiA5E=")
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .certificatePinner(certificatePinner)
        .build()

    /**
     * Check for updates from GitHub Releases.
     * Returns [ReleaseInfo] if a newer version is available, null otherwise.
     */
    suspend fun checkForUpdate(context: Context, force: Boolean = false): ReleaseInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val githubApiUrl = context.getString(R.string.github_api_url)

        // Skip check if recently checked (unless forced)
        if (!force) {
            val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
            if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) {
                return null
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val body = executeWithRetry(githubApiUrl) ?: return@withContext null
                val json = JSONObject(body)

                val tagName = json.getString("tag_name")
                val version = tagName.removePrefix("v")
                val releaseNotes = json.optString("body", "")
                val htmlUrl = json.getString("html_url")

                // Update last check time
                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

                // Check if this version is newer
                val currentVersion = BuildConfig.VERSION_NAME
                val skippedVersion = prefs.getString(KEY_SKIP_VERSION, null)

                if (isNewerVersion(version, currentVersion) && version != skippedVersion) {
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
    private suspend fun executeWithRetry(url: String): String? {
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
     * Mark a version as skipped so the user won't be prompted again.
     */
    fun skipVersion(context: Context, version: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SKIP_VERSION, version)
            .apply()
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
