package com.tqmane.filmsim.data.update

import android.content.Context
import android.util.Log
import com.tqmane.filmsim.BuildConfig
import com.tqmane.filmsim.R
import com.tqmane.filmsim.util.ReleaseInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) : UpdateRepository {

    companion object {
        private const val TAG = "UpdateRepository"
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1_000L
    }

    override suspend fun checkForUpdate(force: Boolean): ReleaseInfo? {
        val githubApiUrl = context.getString(R.string.github_api_url)

        return withContext(Dispatchers.IO) {
            try {
                val body = executeWithRetry(githubApiUrl) ?: return@withContext null
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

    private suspend fun executeWithRetry(url: String): String? {
        var lastException: Exception? = null
        var backoff = INITIAL_BACKOFF_MS

        for (attempt in 1..MAX_RETRIES) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    return response.body?.string()
                }
                if (response.code in 400..499) {
                    Log.w(TAG, "HTTP ${response.code} – not retrying")
                    return null
                }
                Log.w(TAG, "HTTP ${response.code} – attempt $attempt/$MAX_RETRIES")
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "Attempt $attempt/$MAX_RETRIES failed: ${classifyNetworkError(e)}")
            }

            if (attempt < MAX_RETRIES) {
                delay(backoff)
                backoff *= 2
            }
        }

        lastException?.let { throw it }
        return null
    }

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
            return false
        } catch (e: Exception) {
            return false
        }
    }

    private fun classifyNetworkError(e: Exception): String = when (e) {
        is UnknownHostException -> "DNS resolution failed: ${e.message}"
        is SocketTimeoutException -> "Connection timed out: ${e.message}"
        is javax.net.ssl.SSLPeerUnverifiedException -> "Certificate pinning verification failed: ${e.message}"
        is javax.net.ssl.SSLHandshakeException -> "SSL handshake failed: ${e.message}"
        is IOException -> "Network IO error: ${e.message}"
        else -> "Unexpected error: ${e.message}"
    }
}
