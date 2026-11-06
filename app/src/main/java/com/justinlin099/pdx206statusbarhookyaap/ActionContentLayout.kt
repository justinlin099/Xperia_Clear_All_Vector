package com.justinlin099.pdx206statusbarhookyaap

import android.graphics.Rect
import android.view.Gravity
import android.widget.Button

/** TextView draws its top compound drawable at paddingTop, independent of text gravity. */
internal object ActionContentLayout {
    fun center(button: Button) {
        val layout = button.layout ?: return
        if (button.height <= 0 || button.width <= 0) return
        val icons = button.compoundDrawablesRelative
        fun iconHeight(index: Int): Int = icons[index]?.let {
            it.bounds.height().takeIf { height -> height > 0 } ?: it.intrinsicHeight.coerceAtLeast(0)
        } ?: 0
        val count = minOf(layout.lineCount, button.maxLines.takeIf { it > 0 } ?: layout.lineCount)
        val top = iconHeight(1)
        val bottom = iconHeight(3)
        var textHeight = layout.getLineTop(count)
        // Do not count the unused font descent below the final visible glyph as
        // bottom content. This makes the visible icon/label margins symmetrical.
        if (top > 0 && bottom == 0 && count > 0) {
            val last = count - 1
            val start = layout.getLineStart(last)
            val end = layout.getLineVisibleEnd(last)
            if (end > start) {
                val ink = Rect()
                button.paint.getTextBounds(layout.text, start, end, ink)
                if (!ink.isEmpty) textHeight = minOf(textHeight, layout.getLineBaseline(last) + ink.bottom)
            }
        }
        val contentHeight = if (top > 0 || bottom > 0) {
            textHeight + top + bottom + button.compoundDrawablePadding *
                ((if (top > 0) 1 else 0) + (if (bottom > 0) 1 else 0))
        } else {
            maxOf(textHeight, iconHeight(0), iconHeight(2))
        }
        val space = (button.height - contentHeight).coerceAtLeast(0)
        val paddingTop = (space + 1) / 2
        val paddingBottom = space - paddingTop
        button.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        if (button.paddingTop != paddingTop || button.paddingBottom != paddingBottom) {
            button.setPaddingRelative(button.paddingStart, paddingTop, button.paddingEnd, paddingBottom)
        }
    }
}
