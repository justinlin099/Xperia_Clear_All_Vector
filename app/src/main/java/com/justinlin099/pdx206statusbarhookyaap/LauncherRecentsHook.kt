package com.justinlin099.pdx206statusbarhookyaap

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Button
import android.widget.LinearLayout
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Method
import kotlin.math.roundToInt

/** Xperia Home 17 (versionCode 37). No dependency on private Launcher classes at compile time. */
internal object LauncherRecentsHook {
    const val PACKAGE_NAME = "com.sonymobile.launcher"
    private const val ACTIONS_CLASS = "com.android.quickstep.views.OverviewActionsView"
    private const val RECENTS_CLASS = "com.android.quickstep.views.RecentsView"
    private const val LOG_TAG = "XPERIA_RECENTS: "
    private const val ACTION_ROW_WIDTH_FRACTION = 0.88f
    private const val ACTION_ROW_MAX_WIDTH_DP = 480f
    private const val ACTION_GAP_DP = 4f
    private const val ACTION_MIN_HEIGHT_DP = 76f
    private const val ACTION_CORNER_RADIUS_DP = 16f

    // Kept on the added Button itself, avoiding a global map retaining Launcher views.
    private class ButtonState(val dismissMethod: Method) {
        var busy = false
        var failureLogged = false
        var iconSlot = -1
        var iconWidth = -1
        var iconHeight = -1
    }

    fun install(param: XC_LoadPackage.LoadPackageParam) {
        if (param.packageName != PACKAGE_NAME) return
        try {
            val actionsClass = XposedHelpers.findClass(ACTIONS_CLASS, param.classLoader)
            val recentsClass = XposedHelpers.findClass(RECENTS_CLASS, param.classLoader)
            val dismiss = XposedHelpers.findMethodExact(recentsClass, "dismissAllTasks", View::class.java)
            // Validate the actual Sony fields before installing any hook.
            listOf("mActionButtons", "mRecentsView", "mDisabledFlags").forEach {
                XposedHelpers.findField(actionsClass, it)
            }
            recentsClass.getDeclaredMethod("hasTaskViews")
            recentsClass.getDeclaredMethod("isSplitSelectionActive")

            XposedHelpers.findAndHookMethod(actionsClass, "onFinishInflate", object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    if (p.hasThrowable()) return
                    try {
                        addButton(p.thisObject as ViewGroup, dismiss)
                    } catch (e: Throwable) {
                        XposedBridge.log(LOG_TAG + "button injection skipped: $e")
                    }
                }
            })

            // These methods run after Launcher changes visibility, grouping or enabled state.
            listOf("setRecentsView", "updateDisabledFlags", "updateHiddenFlags", "updateForGroupedTask",
                "onConfigurationChanged", "updateScreenshotButtonDrawable").forEach { name ->
                XposedBridge.hookAllMethods(actionsClass, name, object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        if (!p.hasThrowable()) {
                            val actions = p.thisObject as ViewGroup
                            if (name == "onConfigurationChanged" || name == "updateScreenshotButtonDrawable") {
                                refreshAppearance(actions)
                            }
                            refresh(actions)
                        }
                    }
                })
            }
            XposedBridge.hookAllMethods(recentsClass, "updateEmptyMessage", object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    if (p.hasThrowable()) return
                    try {
                        val actions = XposedHelpers.callMethod(p.thisObject, "getActionsView") as? ViewGroup
                        if (actions != null) refresh(actions)
                    } catch (e: Throwable) {
                        // Called during construction too, before the actions view is available.
                    }
                }
            })
            XposedBridge.log(LOG_TAG + "Launcher hooks installed in " + param.processName)
        } catch (e: Throwable) {
            XposedBridge.log(LOG_TAG + "unsupported Launcher; clear-all hook skipped: $e")
        }
    }

    private fun row(actions: ViewGroup): LinearLayout? =
        XposedHelpers.getObjectField(actions, "mActionButtons") as? LinearLayout

    private fun addedButton(row: LinearLayout): Button? {
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i)
            if (child is Button && child.tag is ButtonState) return child
        }
        return null
    }

    private fun addButton(actions: ViewGroup, dismiss: Method) {
        val row = row(actions) ?: return
        if (addedButton(row) != null) return
        val resources = actions.resources
        val screenshotId = resources.getIdentifier("action_screenshot", "id", PACKAGE_NAME)
        val template = actions.findViewById<Button>(screenshotId) ?: return
        val style = resources.getIdentifier("OverviewActionButton", "style", PACKAGE_NAME)
        if (style == 0) return
        val button = Button(template.context, null, 0, style)
        button.id = View.generateViewId()
        button.tag = ButtonState(dismiss)
        button.text = clearAllText(actions)
        button.contentDescription = button.text
        button.setTextColor(template.textColors)
        button.typeface = template.typeface
        button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, template.textSize)
        button.isAllCaps = false
        button.maxLines = 2
        button.ellipsize = TextUtils.TruncateAt.END
        button.setPaddingRelative(template.paddingStart, template.paddingTop,
            template.paddingEnd, template.paddingBottom)
        button.gravity = template.gravity
        button.textAlignment = template.textAlignment
        button.includeFontPadding = template.includeFontPadding
        button.compoundDrawablePadding = template.compoundDrawablePadding
        updateIcon(button, template)
        button.setOnClickListener { onClearAll(actions, button) }
        row.addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        // Width is recalculated from the actual view bounds, including after rotation.
        actions.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            try {
                styleActionRow(actions)
            } catch (e: Throwable) {
                logFailure(button, e)
            }
        }
        refresh(actions)
        XposedBridge.log(LOG_TAG + "clear-all button added")
    }

    /** A compact, centered row with complete icon-and-label touch feedback. */
    private fun styleActionRow(actions: ViewGroup) {
        val row = row(actions) ?: return
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

    private fun clearAllText(actions: View): CharSequence {
        val id = actions.resources.getIdentifier("recents_clear_all", "string", PACKAGE_NAME)
        return if (id != 0) actions.resources.getText(id) else "Clear all"
    }

    /** Follow the actual Sony button, which may be changed by runtime overlays.
     * Configuration.orientation alone did not match this device's real icon placement. */
    private fun updateIcon(button: Button, template: Button, force: Boolean = false) {
        val state = button.tag as ButtonState
        val originals = template.compoundDrawablesRelative
        val slot = when {
            originals[1] != null -> 1 // top (observed on the user's Xperia)
            originals[0] != null -> 0 // start
            originals[2] != null -> 2 // end
            originals[3] != null -> 3 // bottom
            else -> 1
        }
        val original = originals[slot]
        val defaultSize = (24 * button.resources.displayMetrics.density).roundToInt()
        val width = original?.bounds?.width()?.takeIf { it > 0 }
            ?: original?.intrinsicWidth?.takeIf { it > 0 } ?: defaultSize
        val height = original?.bounds?.height()?.takeIf { it > 0 }
            ?: original?.intrinsicHeight?.takeIf { it > 0 } ?: defaultSize
        button.compoundDrawablePadding = template.compoundDrawablePadding
        button.gravity = template.gravity
        button.textAlignment = template.textAlignment
        if (!force && state.iconSlot == slot && state.iconWidth == width && state.iconHeight == height) return
        val icon = ClearAllIcon(button.textColors).apply { setBounds(0, 0, width, height) }
        val drawables = arrayOfNulls<Drawable>(4)
        drawables[slot] = icon
        button.setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3])
        state.iconSlot = slot
        state.iconWidth = width
        state.iconHeight = height
    }

    private fun screenshotButton(actions: ViewGroup): Button? {
        val id = actions.resources.getIdentifier("action_screenshot", "id", PACKAGE_NAME)
        return actions.findViewById(id)
    }

    private fun refreshAppearance(actions: ViewGroup) {
        try {
            val button = row(actions)?.let { addedButton(it) } ?: return
            val template = screenshotButton(actions) ?: return
            button.setTextColor(template.textColors)
            button.text = clearAllText(actions)
            button.contentDescription = button.text
            updateIcon(button, template, force = true)
        } catch (e: Throwable) {
            XposedBridge.log(LOG_TAG + "appearance update skipped: $e")
        }
    }

    private fun refresh(actions: ViewGroup) {
        var button: Button? = null
        try {
            button = row(actions)?.let { addedButton(it) } ?: return
            val state = button.tag as ButtonState
            screenshotButton(actions)?.let { updateIcon(button, it) }
            styleActionRow(actions)
            val recents = XposedHelpers.getObjectField(actions, "mRecentsView")
            val hasTasks = recents != null && XposedHelpers.callMethod(recents, "hasTaskViews") == true
            val split = recents != null && XposedHelpers.callMethod(recents, "isSplitSelectionActive") == true
            button.visibility = if (hasTasks && !split) View.VISIBLE else View.GONE
            // Bit 1 is scrolling/animation. Screenshot rotation and screenshot-policy
            // flags (2/4) must not disable clearing the entire task list.
            val scrolling = XposedHelpers.getIntField(actions, "mDisabledFlags") and 1 != 0
            button.isEnabled = hasTasks && !split && !scrolling && !state.busy
        } catch (e: Throwable) {
            button?.visibility = View.GONE
            logFailure(button, e)
        }
    }

    private fun onClearAll(actions: ViewGroup, button: Button) {
        val state = button.tag as ButtonState
        if (state.busy) return
        try {
            refresh(actions)
            if (!button.isEnabled || !button.isShown) return
            val row = row(actions) ?: return
            if (row.alpha < 0.99f || actions.alpha < 0.99f) return
            val recents = XposedHelpers.getObjectField(actions, "mRecentsView") ?: return
            if (XposedHelpers.callMethod(recents, "isHandlingTouch") == true) return
            val manager = XposedHelpers.callMethod(recents, "getStateManager") ?: return
            if (XposedHelpers.callMethod(manager, "isInTransition") == true) return
            state.busy = true
            button.isEnabled = false
            // The same entry point used by the original leftmost ClearAllButton:
            // includes jank monitoring, native dismissal animations, removal and logging.
            state.dismissMethod.invoke(recents, button)
        } catch (e: Throwable) {
            logFailure(button, e)
        } finally {
            if (state.busy) {
                button.postDelayed({
                    state.busy = false
                    refresh(actions)
                }, 1200L)
            }
        }
    }

    private fun logFailure(button: Button?, error: Throwable) {
        val state = button?.tag as? ButtonState
        if (state == null || !state.failureLogged) {
            XposedBridge.log(LOG_TAG + "clear-all unavailable: $error")
            state?.failureLogged = true
        }
    }

    /** Small, theme-aware clear-all glyph; does not load resources from the module APK. */
    private class ClearAllIcon(private val colors: ColorStateList) : Drawable() {
        private var drawableAlpha = 255
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        override fun draw(canvas: Canvas) {
            val save = canvas.save()
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
            paint.color = colors.getColorForState(state, colors.defaultColor)
            paint.alpha = android.graphics.Color.alpha(paint.color) * drawableAlpha / 255
            canvas.drawLine(7f, 6f, 21f, 6f, paint)
            canvas.drawLine(3f, 12f, 17f, 12f, paint)
            canvas.drawLine(7f, 18f, 21f, 18f, paint)
            canvas.restoreToCount(save)
        }
        override fun isStateful() = true
        override fun onStateChange(state: IntArray): Boolean { invalidateSelf(); return true }
        override fun setAlpha(alpha: Int) { drawableAlpha = alpha; invalidateSelf() }
        override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter; invalidateSelf() }
        @Deprecated("Deprecated in Android")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
