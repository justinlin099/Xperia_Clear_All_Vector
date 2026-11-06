package com.justinlin099.pdx206statusbarhookyaap

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActionRowLayoutTest {
    private class CountingButton : Button(RuntimeEnvironment.getApplication()) {
        var layoutRequests = 0
        override fun requestLayout() {
            layoutRequests++
            super.requestLayout()
        }
    }

    private class Fixture {
        val actions = FrameLayout(RuntimeEnvironment.getApplication())
        val row = LinearLayout(actions.context)
        val buttons = listOf("Screenshot", "Multi window mode", "Clear all").map { label ->
            CountingButton().apply {
                text = label
                isAllCaps = false
                textSize = 14f
                compoundDrawablePadding = 8
                setCompoundDrawables(null, ColorDrawable(Color.GREEN).apply { setBounds(0, 0, 24, 24) }, null, null)
                row.addView(this, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
        }
        val layout = ActionRowLayout(actions, row)

        init {
            actions.addView(row, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 60))
            actions.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> layout.update() }
            row.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> layout.update() }
            layout.update()
        }

        fun settle(width: Int) {
            var previous: List<Int>? = null
            repeat(12) {
                // This detached fixture has no ViewRoot to schedule the second pass
                // requested by onLayoutChange; explicitly drive that traversal.
                actions.forceLayout()
                row.forceLayout()
                buttons.forEach { it.forceLayout() }
                actions.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY))
                actions.layout(0, 0, width, 400)
                val current = listOf(row.left, row.width, row.height) + buttons.flatMap {
                    listOf(it.width, it.height, it.paddingTop, it.paddingBottom)
                }
                if (current == previous && row.width == row.layoutParams.width) return
                previous = current
            }
            fail("Action row must settle without an unbounded layout loop")
        }
    }

    @Test fun repeatedUpdatesKeepBoundsAndDoNotRequestLayout() {
        val fixture = Fixture()
        fixture.settle(400)
        val heights = fixture.buttons.map { it.height }
        val padding = fixture.buttons.map { it.paddingTop to it.paddingBottom }
        val backgrounds = fixture.buttons.map { it.background }
        fixture.buttons.forEach { it.layoutRequests = 0 }
        repeat(100) { fixture.layout.update() }
        assertEquals(heights, fixture.buttons.map { it.height })
        assertEquals(padding, fixture.buttons.map { it.paddingTop to it.paddingBottom })
        fixture.buttons.forEachIndexed { index, button ->
            assertSame(backgrounds[index], button.background)
            assertEquals("Unchanged appearance must not request another layout", 0, button.layoutRequests)
        }
    }

    @Test fun rowResizesOnRotationAndVisibleButtonsShareAvailableWidth() {
        val fixture = Fixture()
        fixture.settle(400)
        assertEquals(352, fixture.row.width)
        assertTrue(fixture.buttons.all { it.height >= 76 })
        assertEquals(1, fixture.buttons.map { it.height }.distinct().size)
        fixture.settle(800)
        assertEquals(480, fixture.row.width)
        assertEquals(160, fixture.row.left)
        val originalWidth = fixture.buttons.last().width
        fixture.buttons[1].visibility = View.GONE
        fixture.settle(800)
        assertTrue(fixture.buttons.last().width > originalWidth)
        assertEquals(fixture.buttons.first().width, fixture.buttons.last().width)
        fixture.buttons[1].visibility = View.VISIBLE
        fixture.settle(400)
        assertEquals(352, fixture.row.width)
    }

    @Test fun appearanceChangesRecalculateCommonHeightForLargeText() {
        val fixture = Fixture()
        fixture.settle(400)
        val originalHeight = fixture.buttons.first().height
        fixture.buttons[1].textSize = 28f
        fixture.layout.update()
        fixture.settle(400)
        assertTrue(fixture.buttons.first().height > originalHeight)
        assertEquals(1, fixture.buttons.map { it.height }.distinct().size)
        assertTrue(fixture.buttons.all { it.paddingTop >= 0 && it.paddingBottom >= 0 })
    }
}
