package com.justinlin099.pdx206statusbarhookyaap

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
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

    // Kept on the added Button itself, avoiding a global map retaining Launcher views.
    private class ButtonState(
        val dismissMethod: Method,
        val template: Button,
        val layout: ActionRowLayout
    ) {
        var busy = false
        var failureLogged = false
        var iconSlot = -1
        var iconWidth = -1
        var iconHeight = -1
        var iconColors: ColorStateList? = null
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

    // IDs belong to Sony's APK and cannot be compiled into this module's R class.
    @SuppressLint("DiscouragedApi")
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
        val layout = ActionRowLayout(actions, row)
        button.tag = ButtonState(dismiss, template, layout)
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
                layout.update()
            } catch (e: Throwable) {
                logFailure(button, e)
            }
        }
        row.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            try {
                layout.update()
            } catch (e: Throwable) {
                logFailure(button, e)
            }
        }
        layout.update()
        refresh(actions)
        XposedBridge.log(LOG_TAG + "clear-all button added")
    }

    @SuppressLint("DiscouragedApi") // Resolve the host Launcher's own translated string.
    private fun clearAllText(actions: View): CharSequence {
        val id = actions.resources.getIdentifier("recents_clear_all", "string", PACKAGE_NAME)
        return if (id != 0) actions.resources.getText(id) else "Clear all"
    }

    /** Follow the actual Sony button, which may be changed by runtime overlays.
     * Configuration.orientation alone did not match this device's real icon placement. */
    private fun updateIcon(button: Button, template: Button) {
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
        if (button.compoundDrawablePadding != template.compoundDrawablePadding) {
            button.compoundDrawablePadding = template.compoundDrawablePadding
        }
        val colors = button.textColors
        if (state.iconSlot == slot && state.iconWidth == width && state.iconHeight == height &&
            state.iconColors == colors) return
        val icon = ClearAllIcon(colors).apply { setBounds(0, 0, width, height) }
        val drawables = arrayOfNulls<Drawable>(4)
        drawables[slot] = icon
        button.setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3])
        state.iconSlot = slot
        state.iconWidth = width
        state.iconHeight = height
        state.iconColors = colors
    }

    private fun refreshAppearance(actions: ViewGroup) {
        try {
            val button = row(actions)?.let { addedButton(it) } ?: return
            val state = button.tag as ButtonState
            val template = state.template
            if (button.textColors != template.textColors) button.setTextColor(template.textColors)
            if (button.typeface != template.typeface) button.typeface = template.typeface
            if (button.textSize != template.textSize) {
                button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, template.textSize)
            }
            if (button.textAlignment != template.textAlignment) button.textAlignment = template.textAlignment
            val text = clearAllText(actions)
            if (button.text != text) {
                button.text = text
                button.contentDescription = text
            }
            updateIcon(button, template)
            state.layout.update()
        } catch (e: Throwable) {
            XposedBridge.log(LOG_TAG + "appearance update skipped: $e")
        }
    }

    private fun refresh(actions: ViewGroup) {
        var button: Button? = null
        try {
            button = row(actions)?.let { addedButton(it) } ?: return
            val state = button.tag as ButtonState
            val recents = XposedHelpers.getObjectField(actions, "mRecentsView")
            val hasTasks = recents != null && XposedHelpers.callMethod(recents, "hasTaskViews") == true
            val split = recents != null && XposedHelpers.callMethod(recents, "isSplitSelectionActive") == true
            val visibility = if (hasTasks && !split) View.VISIBLE else View.GONE
            if (button.visibility != visibility) button.visibility = visibility
            // Bit 1 is scrolling/animation. Screenshot rotation and screenshot-policy
            // flags (2/4) must not disable clearing the entire task list.
            val scrolling = XposedHelpers.getIntField(actions, "mDisabledFlags") and 1 != 0
            val enabled = hasTasks && !split && !scrolling && !state.busy
            if (button.isEnabled != enabled) button.isEnabled = enabled
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
}
