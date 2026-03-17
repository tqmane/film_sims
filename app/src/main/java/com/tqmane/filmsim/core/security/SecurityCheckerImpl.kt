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
        private const val CACHE_TTL_MS = 10_000L

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

        val signatureValid = verifySignature(context)
        val rooted = isRootedDevice()
        val hookingPresent = isHookingFrameworkPresent()
        val debuggerAttached = isDebuggerAttached(context)
        val result = signatureValid && !rooted && !hookingPresent && !debuggerAttached

        // Only cache positive results; failures are always re-evaluated
        if (result) {
            cachedTrustResult = result
            lastCheckTimestamp = now
        } else {
            cachedTrustResult = null
        }

        if (!result && BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "Environment trust check FAILED: " +
                    "signatureValid=$signatureValid, rooted=$rooted, " +
                    "hookingPresent=$hookingPresent, debuggerAttached=$debuggerAttached"
            )
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
                if (BuildConfig.DEBUG) Log.d(TAG, "No signatures found!")
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

            if (BuildConfig.DEBUG) Log.d(TAG, "Signature verification failed!")
            return isDebugBuild

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Error generating signature hash", e)
            return isDebugBuild
        }
    }

    private fun isRootedDevice(): Boolean {
        if (Build.TAGS?.contains("test-keys", ignoreCase = true) == true) return true
        if (hasInsecureSystemProperties()) return true

        val suPaths = arrayOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/su", "/su/bin/su",
            "/sbin/.magisk", "/data/adb/magisk",
            "/system/app/Superuser.apk", "/cache/su"
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

    private fun hasInsecureSystemProperties(): Boolean {
        val suspiciousProperties = mapOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0"
        )
        return suspiciousProperties.any { (key, expectedValue) ->
            readSystemProperty(key) == expectedValue
        }
    }

    private fun readSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
            process.inputStream.bufferedReader().use { reader ->
                reader.readText().trim().ifEmpty { null }
            }.also {
                process.destroy()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isHookingFrameworkPresent(): Boolean {
        val fridaIndicators = arrayOf(
            "frida-server", "frida-agent", "frida-gadget",
            "gmain", "linjector", "zygisk", "riru", "lsposed", "substrate"
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

        if (isKnownHookClassLoaded()) return true

        try {
            val stackTrace = Thread.currentThread().stackTrace
            for (element in stackTrace) {
                val className = element.className
                if (className.contains("de.robv.android.xposed", ignoreCase = true)) return true
                if (className.contains("com.saurik.substrate", ignoreCase = true)) return true
                if (className.contains("org.lsposed", ignoreCase = true)) return true
            }
        } catch (_: Exception) { }

        return false
    }

    private fun isKnownHookClassLoaded(): Boolean {
        val classLoader = Thread.currentThread().contextClassLoader ?: return false
        val hookClasses = arrayOf(
            "de.robv.android.xposed.XposedBridge",
            "org.lsposed.lspd.core.Main",
            "com.saurik.substrate.MS\$2"
        )
        return hookClasses.any { className ->
            try {
                Class.forName(className, false, classLoader)
                true
            } catch (_: ClassNotFoundException) {
                false
            } catch (_: LinkageError) {
                false
            }
        }
    }

    private fun isDebuggerAttached(context: Context): Boolean {
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger() || hasActiveTracer()) return true

        try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            if (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                return !BuildConfig.DEBUG
            }
        } catch (_: Exception) { }

        return false
    }

    private fun hasActiveTracer(): Boolean {
        return try {
            val statusFile = File("/proc/self/status")
            if (!statusFile.exists()) {
                false
            } else {
                statusFile.useLines { lines ->
                    lines.firstOrNull { it.startsWith("TracerPid:") }
                        ?.substringAfter(':')
                        ?.trim()
                        ?.toIntOrNull()
                        ?.let { it > 0 }
                        ?: false
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}
