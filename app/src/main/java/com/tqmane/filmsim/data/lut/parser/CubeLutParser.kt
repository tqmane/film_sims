package com.tqmane.filmsim.data.lut.parser

import android.util.Log
import com.tqmane.filmsim.util.CubeLUT
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Parses .cube text-format LUT files (Adobe/Resolve standard).
 */
class CubeLutParser @Inject constructor() : LutParser {

    companion object {
        private const val TAG = "CubeLutParser"
    }

    override fun parse(bytes: ByteArray, assetPath: String): CubeLUT? {
        try {
            val reader = BufferedReader(InputStreamReader(bytes.inputStream()))
            var size = -1
            val dataList = mutableListOf<Float>()

            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") ||
                    trimmed.startsWith("TITLE") || trimmed.startsWith("DOMAIN_")
                ) {
                    line = reader.readLine()
                    continue
                }

                if (trimmed.startsWith("LUT_3D_SIZE")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 2) size = parts[1].toInt()
                } else {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 3) {
                        try {
                            dataList.add(parts[0].toFloat())
                            dataList.add(parts[1].toFloat())
                            dataList.add(parts[2].toFloat())
                        } catch (_: NumberFormatException) { }
                    }
                }
                line = reader.readLine()
            }
            reader.close()

            if (size == -1 || dataList.isEmpty()) return null

            val floatBuffer = ByteBuffer.allocateDirect(dataList.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            for (f in dataList) floatBuffer.put(f)
            floatBuffer.position(0)

            return CubeLUT(size, floatBuffer)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing .cube LUT: $assetPath", e)
            return null
        }
    }
}
