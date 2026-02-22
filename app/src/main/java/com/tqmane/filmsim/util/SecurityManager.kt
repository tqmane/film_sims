package com.tqmane.filmsim.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

object SecurityManager {
    private const val TAG = "SecurityManager"

    // Valid signature hashes (SHA-256 Base64 encoded)
    private val EXPECTED_SIGNATURE_HASHES = listOf(
        "07D6Pj199ET0XVf2+Ui/ZB+veLHMPph1mLzMWSAeW/w=", // For Release
        "xgnLChOLC3w983F5Z95nUeMKO14IhATfWBy4tl69ZvM="  // For Debug
    )

    /**
     * Verifies if the application's signature matches the expected official developer signature.
     * In DEBUG builds, it logs the current hash to help the developer configure it, and always returns true.
     * In RELEASE builds, it enforces the check.
     *
     * @return true if the signature is valid or if running in debug mode. false if tampered.
     */
    @SuppressLint("PackageManagerGetSignatures")
    fun verifySignature(context: Context): Boolean {
        // Automatically allow debug builds to prevent development blocks,
        // but enforce on release builds.
        val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

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
                return isDebuggable
            }

            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                val currentSignatureHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                
                if (isDebuggable) {
                    Log.d(TAG, "Current App Signature Hash (SHA-256 Base64): $currentSignatureHash")
                }

                if (EXPECTED_SIGNATURE_HASHES.contains(currentSignatureHash)) {
                    return true
                }
            }
            
            Log.e(TAG, "Signature verification failed! The app might be modified.")
            return isDebuggable

        } catch (e: Exception) {
            Log.e(TAG, "Error generating signature hash", e)
            return isDebuggable
        }
    }
}
