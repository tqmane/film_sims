package com.tqmane.filmsim.data.lut.parser

import android.util.Log
import com.tqmane.filmsim.util.CubeLUT
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Parses binary LUT formats: raw uint8, float32, MS-LUT header, LUT3 header, extensionless.
 */
class BinLutParser @Inject constructor() : LutParser {

    companion object {
        private const val TAG = "BinLutParser"
    }

    override fun parse(bytes: ByteArray, assetPath: String): CubeLUT? {
        try {
            if (bytes.isEmpty()) return null

            var lutSize = 32
            var channels = 3
            var dataOffset = 0
            var isFloatFormat = false

            val magic8 = String(bytes.copyOfRange(0, 8.coerceAtMost(bytes.size)))
            val magic4 = String(bytes.copyOfRange(0, 4.coerceAtMost(bytes.size)))
            val hasMsLutHeader = magic8 == ".MS-LUT "
            val hasLut3Header = magic4 == "LUT3"

            if (hasLut3Header && bytes.size >= 12) {
                lutSize = readInt32LE(bytes, 0x04)
                dataOffset = 12
                channels = 3
                Log.d(TAG, "LUT3 header: lutSize=$lutSize")
            } else if (hasMsLutHeader) {
                parseMsLutHeader(bytes).let { (s, c, o, f) ->
                    lutSize = s; channels = c; dataOffset = o; isFloatFormat = f
                }
            } else {
                parseRawBinary(bytes).let { (s, c, f) ->
                    lutSize = s; channels = c; isFloatFormat = f
                }
            }

            if (!isFloatFormat && hasMsLutHeader && bytes.size > 0x14) {
                val formatHint = bytes[0x10].toInt() and 0xFF
                val dataSize = bytes.size - dataOffset
                val expectedBytesPerPixel = dataSize / (lutSize * lutSize * lutSize)
                if (formatHint == 3 || expectedBytesPerPixel >= 12) {
                    isFloatFormat = true
                    channels = 3
                }
            }

            val isBgr = detectBgr(bytes, dataOffset, lutSize, channels, isFloatFormat, assetPath)

            Log.d(TAG, "BIN LUT: $assetPath, Size=$lutSize, Ch=$channels, Off=$dataOffset, BGR=$isBgr, Float=$isFloatFormat")

            val bytesPerPixel = if (isFloatFormat) 12 else channels
            val bufferSize = lutSize * lutSize * lutSize * bytesPerPixel
            if (dataOffset + bufferSize > bytes.size) {
                Log.e(TAG, "File too small (need ${dataOffset + bufferSize}, have ${bytes.size})")
                return null
            }

            return extractData(bytes, dataOffset, lutSize, channels, isFloatFormat, isBgr)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing BIN LUT: $assetPath", e)
            return null
        }
    }

    // ── Header parsers ──────────────────────────────────

    private data class MsLutResult(val size: Int, val channels: Int, val offset: Int, val isFloat: Boolean)

    private fun parseMsLutHeader(bytes: ByteArray): MsLutResult {
        val fileSize = bytes.size
        if (bytes.size <= 0x30) return MsLutResult(17, 3, 0, false)

        val headerLutSize = readInt32LE(bytes, 0x0C)
        val headerDataOffset = readInt32LE(bytes, 0x28)

        if (headerLutSize in 8..128 && headerDataOffset in 48..4096) {
            val dataSize = fileSize - headerDataOffset
            val expectedPixels = headerLutSize * headerLutSize * headerLutSize
            val channels = when {
                dataSize == expectedPixels * 4 -> 4
                dataSize == expectedPixels * 3 -> 3
                dataSize >= expectedPixels * 3 -> 3
                else -> 3
            }
            return MsLutResult(headerLutSize, channels, headerDataOffset, false)
        }

        // Brute force fallback
        for (size in listOf(17, 32, 33, 21, 16, 25, 20, 64)) {
            for (ch in listOf(3, 4)) {
                val payload = size * size * size * ch
                val hs = fileSize - payload
                if (hs in 0 until 4096) return MsLutResult(size, ch, hs, false)
            }
        }

        val fallbackSize = when {
            fileSize <= 16000 -> 17; fileSize <= 30000 -> 21
            fileSize <= 100000 -> 32; else -> 33
        }
        val off = (fileSize - fallbackSize * fallbackSize * fallbackSize * 3).coerceAtLeast(0)
        return MsLutResult(fallbackSize, 3, off, false)
    }

    private data class RawResult(val size: Int, val channels: Int, val isFloat: Boolean)

    private fun parseRawBinary(bytes: ByteArray): RawResult {
        val fileSize = bytes.size
        return when (fileSize) {
            16384 -> RawResult(16, 4, false)
            131072 -> RawResult(32, 4, false)
            98304 -> RawResult(32, 3, false)
            12288 -> RawResult(16, 3, false)
            else -> {
                val sizeF3 = kotlin.math.round(Math.pow((fileSize / 12).toDouble(), 1.0 / 3.0)).toInt()
                if (sizeF3 in 8..128 && sizeF3 * sizeF3 * sizeF3 * 12 == fileSize)
                    return RawResult(sizeF3, 3, true)

                val sizeC4 = kotlin.math.round(Math.pow((fileSize / 4).toDouble(), 1.0 / 3.0)).toInt()
                if (sizeC4 * sizeC4 * sizeC4 * 4 == fileSize)
                    return RawResult(sizeC4, 4, false)

                val sizeC3 = kotlin.math.round(Math.pow((fileSize / 3).toDouble(), 1.0 / 3.0)).toInt()
                RawResult(sizeC3, 3, false)
            }
        }
    }

    // ── BGR detection ───────────────────────────────────

    private fun detectBgr(
        bytes: ByteArray, dataOffset: Int, lutSize: Int,
        channels: Int, isFloat: Boolean, assetPath: String
    ): Boolean {
        return if (isFloat) {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val b0 = mutableListOf<Float>(); val b2 = mutableListOf<Float>()
            for (r in 0 until minOf(4, lutSize)) {
                val idx = dataOffset + r * 12
                if (idx + 12 <= bytes.size) {
                    buf.position(idx)
                    b0.add(buf.float); buf.float; b2.add(buf.float)
                }
            }
            if (b0.size >= 2 && b2.size >= 2) (b2.last() - b2.first()) > (b0.last() - b0.first()) else false
        } else {
            val b0 = mutableListOf<Int>(); val b2 = mutableListOf<Int>()
            for (r in 0 until minOf(4, lutSize)) {
                val idx = dataOffset + r * channels
                if (idx + 3 <= bytes.size) {
                    b0.add(bytes[idx].toInt() and 0xFF); b2.add(bytes[idx + 2].toInt() and 0xFF)
                }
            }
            if (b0.size >= 2 && b2.size >= 2) (b2.last() - b2.first()) > (b0.last() - b0.first())
            else assetPath.contains(".rgba.", ignoreCase = true)
        }
    }

    // ── Data extraction ─────────────────────────────────

    private fun extractData(
        bytes: ByteArray, dataOffset: Int, lutSize: Int,
        channels: Int, isFloat: Boolean, isBgr: Boolean
    ): CubeLUT {
        val totalPixels = lutSize * lutSize * lutSize
        val floatBuffer = ByteBuffer.allocateDirect(totalPixels * 3 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        var index = dataOffset
        val dataBuf = if (isFloat) ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN) else null

        for (i in 0 until totalPixels) {
            if (isFloat) {
                if (index + 12 > bytes.size) break
                dataBuf!!.position(index)
                val v1 = dataBuf.float.coerceIn(0f, 1f)
                val v2 = dataBuf.float.coerceIn(0f, 1f)
                val v3 = dataBuf.float.coerceIn(0f, 1f)
                if (isBgr) { floatBuffer.put(v3); floatBuffer.put(v2); floatBuffer.put(v1) }
                else { floatBuffer.put(v1); floatBuffer.put(v2); floatBuffer.put(v3) }
                index += 12
            } else {
                if (index + channels > bytes.size) break
                val v1 = (bytes[index].toInt() and 0xFF) / 255f
                val v2 = (bytes[index + 1].toInt() and 0xFF) / 255f
                val v3 = (bytes[index + 2].toInt() and 0xFF) / 255f
                if (isBgr) { floatBuffer.put(v3); floatBuffer.put(v2); floatBuffer.put(v1) }
                else { floatBuffer.put(v1); floatBuffer.put(v2); floatBuffer.put(v3) }
                index += channels
            }
        }

        floatBuffer.position(0)
        return CubeLUT(lutSize, floatBuffer)
    }

    private fun readInt32LE(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 3].toInt() and 0xFF) shl 24)
}
