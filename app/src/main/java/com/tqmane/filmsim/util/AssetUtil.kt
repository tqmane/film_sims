package com.tqmane.filmsim.util

import android.content.Context
import java.io.FileNotFoundException
import java.io.InputStream

object AssetUtil {
    /**
     * Helper to open an asset that might be encrypted.
     * Handles both dynamically listed paths (already ending in .enc) and hardcoded paths.
     */
    fun openAsset(context: Context, path: String): InputStream {
        // 1. If the path already has .enc, just open and decrypt
        if (path.endsWith(".enc", ignoreCase = true)) {
            val stream = context.assets.open(path)
            return AssetDecryptor.decryptStream(stream)
        }
        
        // 2. Try adding .enc for hardcoded paths (e.g., logos, jsons)
        try {
            val stream = context.assets.open("$path.enc")
            return AssetDecryptor.decryptStream(stream)
        } catch (e: FileNotFoundException) {
            // 3. Fallback to unencrypted path if .enc does not exist
            return context.assets.open(path)
        }
    }
}
