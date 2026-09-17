package com.janoski.retroframe

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.pow
import kotlin.math.sqrt

object FilterProcessor {
    fun apply(source: Bitmap, preset: FilmAdjustments): Bitmap {
        val working = source.copy(Bitmap.Config.ARGB_8888, true)
        val w = working.width
        val h = working.height
        val pixels = IntArray(w * h)
        working.getPixels(pixels, 0, w, 0, 0, w, h)

        val intensity = preset.intensity.coerceIn(0f, 1f)
        val contrast = 1f + (preset.contrast - 1f) * intensity
        val exposure = 2f.pow(preset.exposure * intensity)
        val saturation = preset.saturation + (1f - preset.saturation) * (1f - intensity)
        val temp = preset.temperature * intensity
        val fade = (preset.fade * intensity).coerceIn(0f, 1f)
        val grain = (preset.grain * intensity).coerceIn(0f, 1f)
        val vignette = (preset.vignette * intensity).coerceIn(0f, 1f)

        val seed = 0x9E3779B9.toInt()
        val centerX = (w - 1) * 0.5f
        val centerY = (h - 1) * 0.5f
        val maxDist = sqrt(centerX * centerX + centerY * centerY)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val c = pixels[idx]
                var r = Color.red(c).toFloat()
                var g = Color.green(c).toFloat()
                var b = Color.blue(c).toFloat()

                r *= exposure
                g *= exposure
                b *= exposure

                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                r = luma + (r - luma) * saturation
                g = luma + (g - luma) * saturation
                b = luma + (b - luma) * saturation

                r *= (1f + 0.12f * temp)
                g *= (1f + 0.02f * temp)
                b *= (1f - 0.12f * temp)

                r = (r - 128f) * contrast + 128f
                g = (g - 128f) * contrast + 128f
                b = (b - 128f) * contrast + 128f

                r = r * (1f - fade) + 128f * fade
                g = g * (1f - fade) + 128f * fade
                b = b * (1f - fade) + 128f * fade

                if (grain > 0f) {
                    val noise = deterministicNoise(x, y, seed) * 255f * 0.085f * grain
                    r += noise
                    g += noise
                    b += noise
                }

                if (vignette > 0f) {
                    val dx = (x - centerX) / maxDist
                    val dy = (y - centerY) / maxDist
                    val distance = (dx * dx + dy * dy).coerceIn(0f, 1f)
                    val edge = 1f - distance * 0.28f * vignette
                    r *= edge
                    g *= edge
                    b *= edge
                }

                pixels[idx] = Color.argb(255, r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), b.toInt().coerceIn(0, 255))
            }
        }

        working.setPixels(pixels, 0, w, 0, 0, w, h)
        return working
    }

    // Tiny deterministic hash: repeatable grain with no per-pixel Random allocations.
    private fun deterministicNoise(x: Int, y: Int, seed: Int): Float {
        var n = x * 374761393 + y * 668265263 + seed
        n = (n xor (n shr 13)) * 1274126177
        n = n xor (n shr 16)
        return (n and 0xFFFF) / 65535f - 0.5f
    }
}
