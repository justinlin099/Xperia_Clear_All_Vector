package com.justinlin099.pdx206statusbarhookyaap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActionContentLayoutTest {
    private fun button(text: String, width: Int): Button {
        val button = Button(RuntimeEnvironment.getApplication())
        button.background = null
        button.stateListAnimator = null
        button.text = text
        button.isAllCaps = false
        button.setTextColor(Color.RED)
        button.textSize = 14f
        button.includeFontPadding = false
        button.maxLines = 2
        button.ellipsize = android.text.TextUtils.TruncateAt.END
        button.minHeight = 0
        button.minimumHeight = 0
        button.setPadding(4, 8, 4, 8)
        button.compoundDrawablePadding = 8
        button.setCompoundDrawables(null, ColorDrawable(Color.GREEN).apply { setBounds(0, 0, 24, 24) }, null, null)
        button.gravity = Gravity.CENTER
        measure(button, width)
        return button
    }

    private fun measure(button: Button, width: Int) {
        button.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(90, View.MeasureSpec.EXACTLY))
        button.layout(0, 0, width, 90)
    }

    private fun gaps(button: Button): Pair<Int, Int> {
        val bitmap = Bitmap.createBitmap(button.width, button.height, Bitmap.Config.ARGB_8888)
        button.draw(Canvas(bitmap))
        var first = bitmap.height
        var last = -1
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            val c = bitmap.getPixel(x, y)
            if (Color.alpha(c) > 32 && ((Color.red(c) > 160 && Color.green(c) < 80) ||
                    (Color.green(c) > 160 && Color.red(c) < 80))) {
                first = minOf(first, y)
                last = maxOf(last, y)
            }
        }
        assertTrue("Native rendering must draw icon/text pixels", last >= first)
        val bottom = bitmap.height - 1 - last
        println("Rendered '${button.text}': top=$first bottom=$bottom padding=${button.paddingTop}/${button.paddingBottom}")
        val output = java.io.File("build/outputs/layout-checks").apply { mkdirs() }
        java.io.FileOutputStream(java.io.File(output, "${button.text.toString().replace(' ', '_')}_${button.gravity}.png")).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return first to bottom
    }

    @Test fun stockCenterGravityDoesNotCenterCompoundGroup() {
        val (top, bottom) = gaps(button("Clear all", 120))
        assertTrue("Regression fixture must reproduce asymmetric margins: $top/$bottom", abs(top - bottom) > 6)
    }

    @Test fun centersSingleLineUsingRenderedContent() {
        val b = button("Clear all", 120)
        ActionContentLayout.center(b)
        measure(b, 120)
        val (top, bottom) = gaps(b)
        assertTrue("Rendered single-line gaps: $top/$bottom", abs(top - bottom) <= 1)
    }

    @Test fun centersChineseActionLabel() {
        val b = button("多視窗模式", 120)
        ActionContentLayout.center(b)
        measure(b, 120)
        val (top, bottom) = gaps(b)
        assertTrue("Rendered Chinese label gaps: $top/$bottom", abs(top - bottom) <= 1)
    }

    @Test fun centersWrappedLabelAndRemainsStable() {
        val b = button("Multi window mode", 72)
        assertTrue("Fixture must wrap", b.layout.lineCount == 2)
        repeat(4) { ActionContentLayout.center(b); measure(b, 72) }
        val (top, bottom) = gaps(b)
        assertTrue("Rendered wrapped gaps: $top/$bottom", abs(top - bottom) <= 1)
        val padding = b.paddingTop to b.paddingBottom
        repeat(20) { ActionContentLayout.center(b); measure(b, 72) }
        assertTrue("Repeated refresh must not accumulate padding", padding == (b.paddingTop to b.paddingBottom))
    }
}
