package com.tqmane.filmsim.util

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object LutBitmapProcessor {

    // Use coroutines with Dispatchers.Default for optimal CPU utilization
    private val numCores = Runtime.getRuntime().availableProcessors()

    // Ultra-fast parallel LUT application using coroutines
    // intensity: 0.0 = original, 1.0 = full LUT effect
    suspend fun applyLutToBitmap(source: Bitmap, lut: CubeLUT, intensity: Float = 1f): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val size = lut.size
        val sizeMinus1 = size - 1
        val sizeF = sizeMinus1.toFloat()
        val sizeSquared = size * size
        
        // Cache LUT data - access once, use many times
        val buffer = lut.data.duplicate()
        buffer.position(0)
        val lutData = FloatArray(buffer.capacity())
        buffer.get(lutData)

        val newPixels = IntArray(totalPixels)
        val inv = 1f - intensity.coerceIn(0f, 1f)
        val fwd = intensity.coerceIn(0f, 1f)

        // Use maximum parallelism - one chunk per core
        val chunkSize = (totalPixels + numCores - 1) / numCores
        
        val jobs = (0 until numCores).map { chunkIdx ->
            async {
                val startIdx = chunkIdx * chunkSize
                val endIdx = minOf(startIdx + chunkSize, totalPixels)
                
                var i = startIdx
                while (i < endIdx) {
                    val pixel = pixels[i]
                    
                    val origR = pixel ushr 16 and 0xFF
                    val origG = pixel ushr 8 and 0xFF
                    val origB = pixel and 0xFF

                    val r = origR * 0.003921569f  // 1/255
                    val g = origG * 0.003921569f
                    val b = origB * 0.003921569f

                    val rIdx = (r * sizeF + 0.5f).toInt().coerceIn(0, sizeMinus1)
                    val gIdx = (g * sizeF + 0.5f).toInt().coerceIn(0, sizeMinus1)
                    val bIdx = (b * sizeF + 0.5f).toInt().coerceIn(0, sizeMinus1)

                    val index = (bIdx * sizeSquared + gIdx * size + rIdx) * 3

                    val lutR = (lutData[index] * 255f + 0.5f).toInt()
                    val lutG = (lutData[index + 1] * 255f + 0.5f).toInt()
                    val lutB = (lutData[index + 2] * 255f + 0.5f).toInt()

                    // Blend: original * (1-intensity) + lut * intensity
                    val outR = (origR * inv + lutR * fwd + 0.5f).toInt()
                    val outG = (origG * inv + lutG * fwd + 0.5f).toInt()
                    val outB = (origB * inv + lutB * fwd + 0.5f).toInt()

                    newPixels[i] = -0x1000000 or 
                        ((if (outR > 255) 255 else if (outR < 0) 0 else outR) shl 16) or 
                        ((if (outG > 255) 255 else if (outG < 0) 0 else outG) shl 8) or 
                        (if (outB > 255) 255 else if (outB < 0) 0 else outB)
                    
                    i++
                }
            }
        }
        
        jobs.awaitAll()

        Bitmap.createBitmap(newPixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
