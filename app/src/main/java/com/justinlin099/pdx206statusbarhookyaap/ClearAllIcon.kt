package com.justinlin099.pdx206statusbarhookyaap

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Small, theme-aware clear-all glyph; does not load resources from the module APK. */
internal class ClearAllIcon(private val colors: ColorStateList) : Drawable() {
    private var drawableAlpha = 255
    private var currentColor = colors.defaultColor
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    override fun draw(canvas: Canvas) {
        val save = canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        paint.color = currentColor
        paint.alpha = android.graphics.Color.alpha(paint.color) * drawableAlpha / 255
        canvas.drawLine(7f, 6f, 21f, 6f, paint)
        canvas.drawLine(3f, 12f, 17f, 12f, paint)
        canvas.drawLine(7f, 18f, 21f, 18f, paint)
        canvas.restoreToCount(save)
    }
    override fun isStateful() = true
    override fun onStateChange(state: IntArray): Boolean {
        val color = colors.getColorForState(state, colors.defaultColor)
        if (color == currentColor) return false
        currentColor = color
        invalidateSelf()
        return true
    }
    override fun setAlpha(alpha: Int) {
        if (drawableAlpha == alpha) return
        drawableAlpha = alpha
        invalidateSelf()
    }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter; invalidateSelf() }
    @Deprecated("Deprecated in Android")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
