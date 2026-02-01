package com.tqmane.filmsim.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.pow

data class CubeLUT(val size: Int, val data: FloatBuffer)

object CubeLUTParser {
    fun parse(context: Context, assetPath: String): CubeLUT? {
        return when {
            assetPath.endsWith(".png", ignoreCase = true) -> parsePngLut(context, assetPath)
            assetPath.endsWith(".cube", ignoreCase = true) -> parseCubeLut(context, assetPath)
            assetPath.endsWith(".bin", ignoreCase = true) -> parseBinLut(context, assetPath)
            else -> null
        }
    }
    
    private fun parseBinLut(context: Context, assetPath: String): CubeLUT? {
        try {
            val inputStream = context.assets.open(assetPath)
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            if (bytes.isEmpty()) return null
            
            // Header parsing / Heuristic detection
            var lutSize = 32 // Default, will be overwritten
            var channels = 3
            var dataOffset = 0
            var isBgr: Boolean
            
            // Check for .MS-LUT header
            val magic = String(bytes.copyOfRange(0, 8.coerceAtMost(bytes.size)))
            val hasMsLutHeader = magic == ".MS-LUT "
            
            // BGR detection will be determined by analyzing actual data pattern
            // after parsing header and getting data offset
            // (moved to after header parsing)
            
            if (hasMsLutHeader) {
                // Parse MS-LUT header
                // Header structure:
                // 0x00-0x07: ".MS-LUT " magic
                // 0x08-0x0B: version
                // 0x0C-0x0F: LUT size (little endian int)
                // 0x28-0x2F: data offset (little endian long)
                
                val fileSize = bytes.size
                
                // Read LUT size from header at offset 0x0C (4 bytes, little endian)
                if (bytes.size > 0x30) {
                    val headerLutSize = (bytes[0x0C].toInt() and 0xFF) or
                            ((bytes[0x0D].toInt() and 0xFF) shl 8) or
                            ((bytes[0x0E].toInt() and 0xFF) shl 16) or
                            ((bytes[0x0F].toInt() and 0xFF) shl 24)
                    
                    // Read data offset from header at offset 0x28 (8 bytes, little endian, but we only need low 4)
                    val headerDataOffset = (bytes[0x28].toInt() and 0xFF) or
                            ((bytes[0x29].toInt() and 0xFF) shl 8) or
                            ((bytes[0x2A].toInt() and 0xFF) shl 16) or
                            ((bytes[0x2B].toInt() and 0xFF) shl 24)
                    
                    if (headerLutSize in 8..128 && headerDataOffset in 48..4096) {
                        lutSize = headerLutSize
                        dataOffset = headerDataOffset
                        
                        // Determine channels from remaining data size
                        val dataSize = fileSize - dataOffset
                        val expectedPixels = lutSize * lutSize * lutSize
                        channels = when {
                            dataSize == expectedPixels * 4 -> 4
                            dataSize == expectedPixels * 3 -> 3
                            dataSize >= expectedPixels * 3 -> 3 // Allow some padding
                            else -> 3
                        }
                    } else {
                        // Header values look invalid, fall back to brute force
                        var found = false
                        for (size in listOf(17, 32, 33, 21, 16, 25, 20, 64)) {
                            for (ch in listOf(3, 4)) {
                                val payload = size * size * size * ch
                                val headerSize = fileSize - payload
                                if (headerSize in 0 until 4096) {
                                    lutSize = size
                                    channels = ch
                                    dataOffset = headerSize
                                    found = true
                                    break
                                }
                            }
                            if (found) break
                        }
                        
                        if (!found) {
                            lutSize = when {
                                fileSize <= 16000 -> 17
                                fileSize <= 30000 -> 21
                                fileSize <= 100000 -> 32
                                else -> 33
                            }
                            dataOffset = fileSize - (lutSize * lutSize * lutSize * 3)
                            if (dataOffset < 0) dataOffset = 0
                        }
                    }
                } else {
                    // File too small for header, shouldn't happen with MS-LUT
                    lutSize = 17
                    dataOffset = 0
                }
            } else {
                // Raw binary
                val fileSize = bytes.size
                
                when (fileSize) {
                    16384 -> { lutSize = 16; channels = 4 }
                    131072 -> { lutSize = 32; channels = 4 }
                    98304 -> { lutSize = 32; channels = 3 }
                    12288 -> { lutSize = 16; channels = 3 }
                    else -> {
                        // Try 4 channels
                        val sizeC4 = kotlin.math.round(Math.pow((fileSize / 4).toDouble(), 1.0/3.0)).toInt()
                        if (sizeC4 * sizeC4 * sizeC4 * 4 == fileSize) {
                            lutSize = sizeC4
                            channels = 4
                        } else {
                            // Try 3 channels
                            val sizeC3 = kotlin.math.round(Math.pow((fileSize / 3).toDouble(), 1.0/3.0)).toInt()
                            lutSize = sizeC3
                            channels = 3
                        }
                    }
                }
            }

            // Detect if data is stored as floats (12 bytes per pixel) vs bytes (3/4 bytes per pixel)
            // Check header byte at 0x10 - value 3 indicates float format for some files
            // Also check by calculating data size
            var isFloatFormat = false
            if (hasMsLutHeader && bytes.size > 0x14) {
                val formatHint = bytes[0x10].toInt() and 0xFF
                val dataSize = bytes.size - dataOffset
                val expectedBytesPerPixel = dataSize / (lutSize * lutSize * lutSize)
                
                // Float format: 12 bytes per pixel (3 floats × 4 bytes)
                // Byte format: 3 or 4 bytes per pixel
                if (formatHint == 3 || expectedBytesPerPixel >= 12) {
                    isFloatFormat = true
                    channels = 3 // Always 3 floats (RGB) for float format
                }
            }
            
            // Auto-detect RGB/BGR from actual data pattern
            // In a 3D LUT, the first few entries are at R=0,1,2,3... G=0, B=0
            // If data is RGB: first byte (R) should increase
            // If data is BGR: third byte (B->R after swap) should increase
            isBgr = if (isFloatFormat) {
                // Float format: check if values at position 0 and 8 (third float) increase
                // For float LUTs, check if RGB order or BGR order gives increasing R values
                val dataBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                val b0_vals = mutableListOf<Float>()
                val b2_vals = mutableListOf<Float>()
                
                for (r in 0 until minOf(4, lutSize)) {
                    val idx = dataOffset + r * 12
                    if (idx + 12 <= bytes.size) {
                        dataBuffer.position(idx)
                        b0_vals.add(dataBuffer.float)
                        dataBuffer.float // skip middle
                        b2_vals.add(dataBuffer.float)
                    }
                }
                
                if (b0_vals.size >= 2 && b2_vals.size >= 2) {
                    val b0_diff = b0_vals.last() - b0_vals.first()
                    val b2_diff = b2_vals.last() - b2_vals.first()
                    b2_diff > b0_diff // If 3rd float increases more, it's BGR
                } else {
                    false // Default to RGB for float format
                }
            } else {
                // Byte format: check which byte increases along R axis
                val b0_vals = mutableListOf<Int>()
                val b2_vals = mutableListOf<Int>()
                
                for (r in 0 until minOf(4, lutSize)) {
                    val idx = dataOffset + r * channels
                    if (idx + 3 <= bytes.size) {
                        b0_vals.add(bytes[idx].toInt() and 0xFF)
                        b2_vals.add(bytes[idx + 2].toInt() and 0xFF)
                    }
                }
                
                if (b0_vals.size >= 2 && b2_vals.size >= 2) {
                    val b0_diff = b0_vals.last() - b0_vals.first()
                    val b2_diff = b2_vals.last() - b2_vals.first()
                    b2_diff > b0_diff // If 3rd byte increases more, it's BGR
                } else {
                    // Fallback: use filename hints
                    val hasRgbaMarker = assetPath.contains(".rgba.", ignoreCase = true)
                    hasRgbaMarker
                }
            }
            
            android.util.Log.d("CubeLUTParser", "Parsing BIN LUT: $assetPath, Size: $lutSize, Channels: $channels, Offset: $dataOffset, BGR: $isBgr, Float: $isFloatFormat")
            
            // Extract data
            val bytesPerPixel = if (isFloatFormat) 12 else channels
            val bufferSize = lutSize * lutSize * lutSize * bytesPerPixel
            if (dataOffset + bufferSize > bytes.size) {
                 android.util.Log.e("CubeLUTParser", "File too small for expected data size (need ${dataOffset + bufferSize}, have ${bytes.size})")
                 return null
            }

            val floatCount = lutSize * lutSize * lutSize * 3 // Always output RGB (3 channels)
            val floatBuffer = ByteBuffer.allocateDirect(floatCount * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            var index = dataOffset
            val totalPixels = lutSize * lutSize * lutSize
            
            // Create a ByteBuffer for reading floats if needed
            val dataBuffer = if (isFloatFormat) {
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            } else null
            
            for (i in 0 until totalPixels) {
                if (isFloatFormat) {
                    if (index + 12 > bytes.size) break
                    
                    // Read 3 IEEE 754 floats (little endian)
                    dataBuffer!!.position(index)
                    val v1 = dataBuffer.float.coerceIn(0f, 1f)
                    val v2 = dataBuffer.float.coerceIn(0f, 1f)
                    val v3 = dataBuffer.float.coerceIn(0f, 1f)
                    
                    if (isBgr) {
                        floatBuffer.put(v3)
                        floatBuffer.put(v2)
                        floatBuffer.put(v1)
                    } else {
                        floatBuffer.put(v1)
                        floatBuffer.put(v2)
                        floatBuffer.put(v3)
                    }
                    index += 12
                } else {
                    if (index + channels > bytes.size) break
                    
                    // Read bytes as unsigned (0..255)
                    val v1 = (bytes[index].toInt() and 0xFF) / 255f
                    val v2 = (bytes[index + 1].toInt() and 0xFF) / 255f
                    val v3 = (bytes[index + 2].toInt() and 0xFF) / 255f
                    
                    if (isBgr) {
                        // BGR -> RGB
                        floatBuffer.put(v3) // R
                        floatBuffer.put(v2) // G
                        floatBuffer.put(v1) // B
                    } else {
                        // RGB -> RGB
                        floatBuffer.put(v1) // R
                        floatBuffer.put(v2) // G
                        floatBuffer.put(v3) // B
                    }
                    
                    // Ignore alpha if 4 channels (just skip it)
                    index += channels
                }
            }
            
            floatBuffer.position(0)
            return CubeLUT(lutSize, floatBuffer)
            
        } catch (e: Exception) {
            android.util.Log.e("CubeLUTParser", "Error parsing BIN LUT", e)
            return null
        }
    }

    private fun parsePngLut(context: Context, assetPath: String): CubeLUT? {
        try {
            val inputStream = context.assets.open(assetPath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                android.util.Log.e("CubeLUTParser", "Failed to decode PNG: $assetPath")
                return null
            }
            
            val width = bitmap.width
            val height = bitmap.height
            
            android.util.Log.d("CubeLUTParser", "PNG LUT: $assetPath, size: ${width}x${height}")
            
            // 512x512 = 64³ in 8x8 grid of 64x64 slices (HALD format)
            if (width == 512 && height == 512) {
                return parseHaldLut(bitmap, 64)
            }
            
            // Try other common formats
            val lutSize = detectLutSize(width, height)
            if (lutSize == null) {
                android.util.Log.e("CubeLUTParser", "Could not detect LUT size for ${width}x${height}")
                bitmap.recycle()
                return null
            }
            
            return parseHaldLut(bitmap, lutSize)
            
        } catch (e: Exception) {
            android.util.Log.e("CubeLUTParser", "Error parsing PNG LUT: $assetPath", e)
            return null
        }
    }
    
    private fun parseHaldLut(bitmap: Bitmap, lutSize: Int): CubeLUT? {
        try {
            val width = bitmap.width
            val height = bitmap.height
            
            // Calculate grid dimensions
            // For 64³ LUT in 512x512: 8x8 grid of 64x64 tiles
            val tilesPerRow = kotlin.math.sqrt(lutSize.toDouble()).toInt()
            val tileWidth = width / tilesPerRow
            val tileHeight = height / tilesPerRow
            
            android.util.Log.d("CubeLUTParser", "HALD: lutSize=$lutSize, tilesPerRow=$tilesPerRow, tileSize=${tileWidth}x${tileHeight}")
            
            val dataList = mutableListOf<Float>()
            
            // HALD LUT format: iterate B, then G, then R
            // Each "tile" represents a slice of the blue channel
            for (b in 0 until lutSize) {
                // Calculate which tile contains this blue value
                val tileX = b % tilesPerRow
                val tileY = b / tilesPerRow
                
                for (g in 0 until lutSize) {
                    for (r in 0 until lutSize) {
                        // Calculate pixel position within the tile
                        val pixelX = tileX * tileWidth + r
                        val pixelY = tileY * tileHeight + g
                        
                        if (pixelX >= width || pixelY >= height) {
                            android.util.Log.e("CubeLUTParser", "Pixel out of bounds: ($pixelX, $pixelY)")
                            continue
                        }
                        
                        val pixel = bitmap.getPixel(pixelX, pixelY)
                        dataList.add(android.graphics.Color.red(pixel) / 255f)
                        dataList.add(android.graphics.Color.green(pixel) / 255f)
                        dataList.add(android.graphics.Color.blue(pixel) / 255f)
                    }
                }
            }
            
            bitmap.recycle()
            
            if (dataList.isEmpty()) {
                android.util.Log.e("CubeLUTParser", "No data parsed from HALD LUT")
                return null
            }
            
            android.util.Log.d("CubeLUTParser", "Parsed ${dataList.size / 3} LUT entries (expected ${lutSize * lutSize * lutSize})")
            
            val floatBuffer = ByteBuffer.allocateDirect(dataList.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            
            for (f in dataList) {
                floatBuffer.put(f)
            }
            floatBuffer.position(0)
            
            return CubeLUT(lutSize, floatBuffer)
            
        } catch (e: Exception) {
            android.util.Log.e("CubeLUTParser", "Error parsing HALD LUT", e)
            return null
        }
    }

    private fun detectLutSize(width: Int, height: Int): Int? {
        // Common HALD LUT formats
        return when {
            width == 512 && height == 512 -> 64   // 8x8 grid of 64x64
            width == 256 && height == 256 -> 32   // 5.66x5.66 -> likely 32 with some padding
            width == 1024 && height == 1024 -> 64 // Alternative format
            width == 4096 && height == 64 -> 64   // Strip format
            width == 1024 && height == 32 -> 32   // Strip format
            // Try to infer from dimensions
            width == height -> {
                // Square image - try common LUT sizes
                val sqrtSize = kotlin.math.sqrt(width.toDouble()).toInt()
                when {
                    sqrtSize * sqrtSize == width && sqrtSize in listOf(8, 16, 32, 64) -> sqrtSize
                    width == 512 -> 64  // 512x512 for 64³
                    else -> null
                }
            }
            else -> null
        }
    }
    
    private fun parseCubeLut(context: Context, assetPath: String): CubeLUT? {
        try {
            val inputStream = context.assets.open(assetPath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            var size = -1
            val dataList = mutableListOf<Float>()
            
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("TITLE") || trimmed.startsWith("DOMAIN_")) {
                    line = reader.readLine()
                    continue
                }
                
                if (trimmed.startsWith("LUT_3D_SIZE")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        size = parts[1].toInt()
                    }
                } else {
                    // Assuming data line: R G B
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 3) {
                        try {
                            dataList.add(parts[0].toFloat())
                            dataList.add(parts[1].toFloat())
                            dataList.add(parts[2].toFloat())
                        } catch (e: NumberFormatException) {
                            // Ignore malformed lines
                        }
                    }
                }
                line = reader.readLine()
            }
            reader.close()

            if (size == -1 || dataList.isEmpty()) return null

            val floatBuffer = ByteBuffer.allocateDirect(dataList.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            
            for (f in dataList) {
                floatBuffer.put(f)
            }
            floatBuffer.position(0)
            
            return CubeLUT(size, floatBuffer)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}