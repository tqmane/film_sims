package com.tqmane.filmsim.util

import com.tqmane.filmsim.BuildConfig
import java.io.InputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AssetDecryptor {

    private val masterKey: String = BuildConfig.ASSET_KEY

    /**
     * Wrap an InputStream with an AES-256-CTR decryption layer on the fly.
     *
     * Format: [16-byte IV][AES-CTR encrypted payload]
     *
     * Key derivation: SHA-256(masterKey || IV)
     * This must match the scheme used when the .enc assets were produced.
     * Changing this requires re-encrypting every asset file.
     */
    fun decryptStream(inputStream: InputStream): InputStream {
        if (BuildConfig.ASSET_KEY == "placeholder_key" || masterKey.isEmpty()) {
            if (!BuildConfig.DEBUG) {
                throw SecurityException("Asset decryption key not configured")
            }
            return inputStream
        }

        // Read 16-byte IV
        val iv = ByteArray(16)
        var bytesRead = 0
        while (bytesRead < 16) {
            val read = inputStream.read(iv, bytesRead, 16 - bytesRead)
            if (read == -1) break
            bytesRead += read
        }

        if (bytesRead < 16) {
            if (!BuildConfig.DEBUG) {
                throw SecurityException("Invalid encrypted asset format")
            }
            return inputStream
        }

        // Derive 32-byte key: SHA-256(masterKeyBytes || IV)
        val fileKey = deriveKey(masterKey, iv)

        // Initialize AES-256-CTR cipher
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val keySpec = SecretKeySpec(fileKey, "AES")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        return object : InputStream() {
            // Internal buffer for single-byte reads
            private val singleByte = ByteArray(1)

            override fun read(): Int {
                val n = read(singleByte, 0, 1)
                return if (n == -1) -1 else (singleByte[0].toInt() and 0xFF)
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val readLen = inputStream.read(b, off, len)
                if (readLen <= 0) return readLen
                // Decrypt in-place
                val decrypted = cipher.update(b, off, readLen)
                if (decrypted != null) {
                    System.arraycopy(decrypted, 0, b, off, decrypted.size)
                }
                return readLen
            }

            override fun close() = inputStream.close()
        }
    }

    /**
     * Derive a 32-byte key from the master key and per-file IV.
     * Algorithm: SHA-256(masterKeyBytes || IV)
     *
     * WARNING: Do not change this algorithm without also re-encrypting all .enc assets.
     */
    private fun deriveKey(masterKey: String, iv: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(masterKey.toByteArray(Charsets.UTF_8))
        md.update(iv)
        return md.digest()
    }
}
