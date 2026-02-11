package com.tqmane.filmsim.util

import android.content.Context
import android.content.SharedPreferences
import com.tqmane.filmsim.BuildConfig
import com.tqmane.filmsim.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,
    val version: String,
    val releaseNotes: String,
    val htmlUrl: String
)

object UpdateChecker {

    private const val PREFS_NAME = "update_checker"
    private const val KEY_SKIP_VERSION = "skip_version"
    private const val KEY_LAST_CHECK = "last_check"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * Check for updates from GitHub Releases.
     * Returns ReleaseInfo if a newer version is available, null otherwise.
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
                val request = Request.Builder()
                    .url(githubApiUrl)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext null
                }
                
                val body = response.body?.string() ?: return@withContext null
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
                    ReleaseInfo(
                        tagName = tagName,
                        version = version,
                        releaseNotes = releaseNotes,
                        htmlUrl = htmlUrl
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
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
    private fun isNewerVersion(newVersion: String, currentVersion: String): Boolean {
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
}
