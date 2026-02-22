package com.tqmane.filmsim.util

import com.tqmane.filmsim.BuildConfig
import java.io.InputStream

object AssetDecryptor {
    
    // The key is injected from BuildConfig (which reads from secrets.properties)
    private val keyBytes: ByteArray = BuildConfig.ASSET_KEY.toByteArray(Charsets.UTF_8)
    private val keyLen: Int = keyBytes.size

    /**
     * Wrap an InputStream with an XOR decryption layer on the fly.
     * This ensures the decrypted content never touches the disk.
     */
    fun decryptStream(inputStream: InputStream): InputStream {
        // If the key is the placeholder (meaning the contributor didn't set secrets.properties)
        // or empty, we just return the raw stream as a fallback. 
        // In a real scenario, returning the raw stream means it will fail to decode if it is encrypted,
        // but if the user provides raw files, it will work.
        if (BuildConfig.ASSET_KEY == "placeholder_key" || keyLen == 0) {
            return inputStream
        }

        return object : InputStream() {
            private var position = 0

            override fun read(): Int {
                val b = inputStream.read()
                if (b == -1) return -1
                
                // XOR decrypt the byte
                val decryptedByte = b xor keyBytes[position % keyLen].toInt()
                position++
                return decryptedByte
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val bytesRead = inputStream.read(b, off, len)
                if (bytesRead <= 0) return bytesRead
                
                for (i in 0 until bytesRead) {
                    b[off + i] = (b[off + i].toInt() xor keyBytes[(position + i) % keyLen].toInt()).toByte()
                }
                position += bytesRead
                return bytesRead
            }

            override fun close() {
                inputStream.close()
            }
        }
    }
}
