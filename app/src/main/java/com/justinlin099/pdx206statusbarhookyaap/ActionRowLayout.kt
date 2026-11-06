package com.justinlin099.pdx206statusbarhookyaap

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.roundToInt

/** Layout work runs on view/appearance changes, independently of task-state callbacks. */
internal class ActionRowLayout(private val actions: ViewGroup, private val row: LinearLayout) {
    companion object {
        private const val ACTION_ROW_WIDTH_FRACTION = 0.88f
        private const val ACTION_ROW_MAX_WIDTH_DP = 480f
        private const val ACTION_GAP_DP = 4f
        private const val ACTION_MIN_HEIGHT_DP = 76f
        private const val ACTION_CORNER_RADIUS_DP = 16f
    }

    /** A compact, centered row with complete icon-and-label touch feedback. */
    fun update() {
        val density = actions.resources.displayMetrics.density
        fun dp(value: Float) = (value * density).roundToInt()
        val available = actions.width - actions.paddingLeft - actions.paddingRight
        val params = row.layoutParams as? FrameLayout.LayoutParams
        if (available > 0 && params != null) {
            val width = minOf((available * ACTION_ROW_WIDTH_FRACTION).roundToInt(), dp(ACTION_ROW_MAX_WIDTH_DP))
            val gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            if (params.width != width || params.gravity != gravity) {
                params.width = width
                params.gravity = gravity
                row.layoutParams = params
            }
        }
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i) as? Button ?: continue
            val lp = child.layoutParams as? LinearLayout.LayoutParams ?: continue
            val margin = dp(ACTION_GAP_DP / 2)
            if (lp.width != 0 || lp.weight != 1f || lp.marginStart != margin || lp.marginEnd != margin) {
                lp.width = 0
                lp.weight = 1f
                lp.marginStart = margin
                lp.marginEnd = margin
                child.layoutParams = lp
            }
            if (child.background !is ActionRipple) {
                // Remove the stock inset/height-limited Button background. A bounded
                // ripple mask and outline now use the entire measured button bounds.
                child.backgroundTintList = null
                child.background = ActionRipple(feedbackColor(child), dp(ACTION_CORNER_RADIUS_DP).toFloat())
                child.stateListAnimator = null
                child.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        val radius = ACTION_CORNER_RADIUS_DP * view.resources.displayMetrics.density
                        outline.setRoundRect(0, 0, view.width, view.height, radius)
                    }
                }
                child.clipToOutline = true
                child.minWidth = 0
                child.minimumWidth = 0
                child.maxHeight = Int.MAX_VALUE
                child.maxLines = 2
                child.minHeight = dp(ACTION_MIN_HEIGHT_DP)
                child.minimumHeight = dp(ACTION_MIN_HEIGHT_DP)
                child.includeFontPadding = false
                child.setPaddingRelative(dp(4f), dp(8f), dp(4f), dp(8f))
                child.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    ActionContentLayout.center(view as Button)
                }
            }
            (child.background as ActionRipple).updateColor(feedbackColor(child))
            child.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        // Reserve a stable common height. Do not use the dynamic centering padding
        // in this calculation, or repeated refreshes would increase the height.
        var commonHeight = dp(ACTION_MIN_HEIGHT_DP)
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i) as? Button ?: continue
            if (child.visibility == View.GONE) continue
            val icons = child.compoundDrawablesRelative
            fun iconHeight(index: Int): Int = icons[index]?.let {
                it.bounds.height().takeIf { height -> height > 0 } ?: it.intrinsicHeight.coerceAtLeast(0)
            } ?: 0
            val textHeight = child.lineHeight * 2
            val top = iconHeight(1)
            val bottom = iconHeight(3)
            val contentHeight = if (top > 0 || bottom > 0) {
                textHeight + top + bottom + child.compoundDrawablePadding *
                    ((if (top > 0) 1 else 0) + (if (bottom > 0) 1 else 0))
            } else {
                maxOf(textHeight, iconHeight(0), iconHeight(2))
            }
            commonHeight = maxOf(commonHeight, contentHeight + dp(16f))
        }
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i) as? Button ?: continue
            val lp = child.layoutParams as? LinearLayout.LayoutParams ?: continue
            if (lp.height != commonHeight) {
                lp.height = commonHeight
                child.layoutParams = lp
            }
            ActionContentLayout.center(child)
        }
        val rowParams = row.layoutParams
        if (rowParams.height in 1 until commonHeight) {
            rowParams.height = commonHeight
            row.layoutParams = rowParams
        }
    }

    private fun feedbackColor(button: Button): Int =
        (0x30 shl 24) or (button.textColors.defaultColor and 0x00ffffff)

    private class ActionRipple(private var feedback: Int, radius: Float) : RippleDrawable(
        ColorStateList.valueOf(feedback),
        null,
        GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = radius }
    ) {
        fun updateColor(color: Int) {
            if (feedback != color) {
                feedback = color
                setColor(ColorStateList.valueOf(color))
            }
        }
    }
}
