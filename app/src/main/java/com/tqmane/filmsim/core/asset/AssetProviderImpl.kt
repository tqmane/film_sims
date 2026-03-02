package com.tqmane.filmsim.core.asset

import android.content.Context
import android.graphics.Typeface
import com.tqmane.filmsim.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AssetProvider {

    private val masterKey: String = BuildConfig.ASSET_KEY

    override fun openAsset(path: String): InputStream {
        // If the path already has .enc, just open and decrypt
        if (path.endsWith(".enc", ignoreCase = true)) {
            val stream = context.assets.open(path)
            return decryptStream(stream)
        }

        // Try adding .enc for hardcoded paths
        try {
            val stream = context.assets.open("$path.enc")
            return decryptStream(stream)
        } catch (e: FileNotFoundException) {
            // Fallback to unencrypted path
            return context.assets.open(path)
        }
    }

    override fun loadTypeface(path: String): Typeface {
        var tempFile: File? = null
        try {
            val inputStream = openAsset(path)
            tempFile = File.createTempFile("font_", ".tmp", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            return Typeface.createFromFile(tempFile)
        } finally {
            tempFile?.delete()
        }
    }

    // ─── Decryption ─────────────────────────────────────

    private fun decryptStream(inputStream: InputStream): InputStream {
        if (BuildConfig.ASSET_KEY == "placeholder_key" || masterKey.isEmpty()) {
            return inputStream
        }

        val iv = ByteArray(16)
        var bytesRead = 0
        while (bytesRead < 16) {
            val read = inputStream.read(iv, bytesRead, 16 - bytesRead)
            if (read == -1) break
            bytesRead += read
        }

        if (bytesRead < 16) return inputStream

        val fileKey = deriveKey(masterKey, iv)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val keySpec = SecretKeySpec(fileKey, "AES")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        return object : InputStream() {
            private val singleByte = ByteArray(1)

            override fun read(): Int {
                val n = read(singleByte, 0, 1)
                return if (n == -1) -1 else (singleByte[0].toInt() and 0xFF)
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val readLen = inputStream.read(b, off, len)
                if (readLen <= 0) return readLen
                val decrypted = cipher.update(b, off, readLen)
                if (decrypted != null) {
                    System.arraycopy(decrypted, 0, b, off, decrypted.size)
                }
                return readLen
            }

            override fun close() = inputStream.close()
        }
    }

    private fun deriveKey(masterKey: String, iv: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(masterKey.toByteArray(Charsets.UTF_8))
        md.update(iv)
        return md.digest()
    }
}
