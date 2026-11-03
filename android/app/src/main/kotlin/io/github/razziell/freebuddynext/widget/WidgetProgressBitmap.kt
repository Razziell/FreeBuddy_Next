package io.github.razziell.freebuddynext.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color
import android.graphics.RectF
import androidx.annotation.ColorInt
import io.github.razziell.freebuddynext.protocol.BatteryLevel

/**
 * Produces a single pixel-accurate progress image for RemoteViews/Glance.
 *
 * Some launchers flatten weighted/nested RemoteViews and expand every child to the full parent.
 * Encoding progress into one bitmap avoids any dependency on launcher layout behaviour.
 */
object WidgetProgressBitmap {
    /** Multiplies the alpha channel of [color] by [opacity] percent (0..100). */
    @ColorInt
    fun withOpacity(@ColorInt color: Int, opacity: Int): Int {
        val factor = opacity.coerceIn(0, 100)
        val alpha = Color.alpha(color) * factor / 100
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    private const val MAX_CACHE_ENTRIES = 64

    // One widget draws 3 bars and often 2+ of them share the same level
    // (e.g. both buds at 100%); reuse identical bitmaps with LRU eviction.
    private val cache = object : LinkedHashMap<CacheKey, Bitmap>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, Bitmap>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    private data class CacheKey(
        val level: Int?,
        val filled: Int,
        val empty: Int,
        val vertical: Boolean,
    )

    fun horizontal(level: Int?, @ColorInt filled: Int, @ColorInt empty: Int): Bitmap =
        cached(level, filled, empty, vertical = false) {
            create(width = 200, height = 40, level = level, filled = filled, empty = empty, vertical = false)
        }

    fun vertical(level: Int?, @ColorInt filled: Int, @ColorInt empty: Int): Bitmap =
        cached(level, filled, empty, vertical = true) {
            create(width = 60, height = 200, level = level, filled = filled, empty = empty, vertical = true)
        }

    @Synchronized
    private fun cached(
        level: Int?,
        filled: Int,
        empty: Int,
        vertical: Boolean,
        producer: () -> Bitmap,
    ): Bitmap {
        return cache.getOrPut(CacheKey(level, filled, empty, vertical), producer)
    }

    private fun create(
        width: Int,
        height: Int,
        level: Int?,
        @ColorInt filled: Int,
        @ColorInt empty: Int,
        vertical: Boolean,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val bounds = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val radius = minOf(width, height) * 0.22f
        val clip = Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) }

        paint.color = empty
        canvas.drawRoundRect(bounds, radius, radius, paint)

        val fraction = BatteryLevel.fraction(level)
        if (fraction > 0f) {
            canvas.save()
            canvas.clipPath(clip)
            paint.color = filled
            if (vertical) {
                val top = height * (1f - fraction)
                canvas.drawRect(0f, top, width.toFloat(), height.toFloat(), paint)
            } else {
                canvas.drawRect(0f, 0f, width * fraction, height.toFloat(), paint)
            }
            canvas.restore()
        }
        return bitmap
    }
}
