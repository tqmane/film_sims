package com.tqmane.filmsim.core.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Base64
import android.util.Log
import com.tqmane.filmsim.BuildConfig
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityCheckerImpl @Inject constructor() : SecurityChecker {

    companion object {
        private const val TAG = "SecurityChecker"
        private const val CACHE_TTL_MS = 60_000L

        private val RELEASE_SIGNATURE_HASH = "07D6Pj199ET0XVf2+Ui/ZB+veLHMPph1mLzMWSAeW/w="
        private val DEBUG_SIGNATURE_HASH = if (BuildConfig.DEBUG) {
            "xgnLChOLC3w983F5Z95nUeMKO14IhATfWBy4tl69ZvM="
        } else {
            null
        }
    }

    @Volatile
    private var cachedTrustResult: Boolean? = null
    @Volatile
    private var lastCheckTimestamp: Long = 0L

    override fun isEnvironmentTrusted(context: Context): Boolean {
        if (BuildConfig.DEBUG) return true

        val now = System.currentTimeMillis()
        val cached = cachedTrustResult
        if (cached != null && (now - lastCheckTimestamp) < CACHE_TTL_MS) {
            return cached
        }

        val result = verifySignature(context) &&
                !isRootedDevice() &&
                !isHookingFrameworkPresent() &&
                !isDebuggerAttached(context)

        cachedTrustResult = result
        lastCheckTimestamp = now

        if (!result) {
            Log.e(TAG, "Environment trust check FAILED")
        }
        return result
    }

    override fun invalidateCache() {
        cachedTrustResult = null
        lastCheckTimestamp = 0L
    }

    @SuppressLint("PackageManagerGetSignatures")
    private fun verifySignature(context: Context): Boolean {
        val isDebugBuild = BuildConfig.DEBUG

        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) {
                Log.e(TAG, "No signatures found!")
                return isDebugBuild
            }

            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                val currentHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)

                if (isDebugBuild) {
                    Log.d(TAG, "Current App Signature Hash (SHA-256 Base64): $currentHash")
                }

                if (currentHash == RELEASE_SIGNATURE_HASH) return true
                if (isDebugBuild && currentHash == DEBUG_SIGNATURE_HASH) return true
            }

            Log.e(TAG, "Signature verification failed! The app might be modified.")
            return isDebugBuild

        } catch (e: Exception) {
            Log.e(TAG, "Error generating signature hash", e)
            return isDebugBuild
        }
    }

    private fun isRootedDevice(): Boolean {
        val suPaths = arrayOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/su", "/su/bin/su",
            "/sbin/.magisk", "/data/adb/magisk"
        )
        for (path in suPaths) {
            if (File(path).exists()) return true
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val result = process.inputStream.bufferedReader().readText().trim()
            process.destroy()
            if (result.isNotEmpty()) return true
        } catch (_: Exception) { }

        return false
    }

    private fun isHookingFrameworkPresent(): Boolean {
        val fridaIndicators = arrayOf(
            "frida-server", "frida-agent", "frida-gadget",
            "gmain", "linjector"
        )

        try {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists()) {
                val content = mapsFile.readText()
                for (indicator in fridaIndicators) {
                    if (content.contains(indicator, ignoreCase = true)) return true
                }
                if (content.contains("XposedBridge", ignoreCase = true)) return true
                if (content.contains("libxposed", ignoreCase = true)) return true
            }
        } catch (_: Exception) { }

        try {
            val stackTrace = Thread.currentThread().stackTrace
            for (element in stackTrace) {
                val className = element.className
                if (className.contains("de.robv.android.xposed", ignoreCase = true)) return true
                if (className.contains("com.saurik.substrate", ignoreCase = true)) return true
            }
        } catch (_: Exception) { }

        return false
    }

    private fun isDebuggerAttached(context: Context): Boolean {
        if (Debug.isDebuggerConnected()) return true

        try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            if (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                return !BuildConfig.DEBUG
            }
        } catch (_: Exception) { }

        return false
    }
}
