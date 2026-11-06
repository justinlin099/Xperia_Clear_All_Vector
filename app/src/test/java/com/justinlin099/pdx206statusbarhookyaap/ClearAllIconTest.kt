package com.justinlin099.pdx206statusbarhookyaap

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ClearAllIconTest {
    private class Callback : Drawable.Callback {
        var invalidations = 0
        override fun invalidateDrawable(who: Drawable) { invalidations++ }
        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = Unit
        override fun unscheduleDrawable(who: Drawable, what: Runnable) = Unit
    }

    @Test fun unrelatedStateAndUnchangedAlphaDoNotInvalidate() {
        val colors = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(Color.CYAN, Color.GRAY))
        val icon = ClearAllIcon(colors)
        val callback = Callback()
        icon.callback = callback
        assertTrue(icon.setState(intArrayOf(android.R.attr.state_enabled)))
        callback.invalidations = 0
        assertFalse(icon.setState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)))
        icon.alpha = 255
        assertEquals(0, callback.invalidations)
        assertTrue(icon.setState(intArrayOf()))
        assertEquals(1, callback.invalidations)
    }

    @Test fun disabledColorAndDrawableAlphaAreRendered() {
        val icon = ClearAllIcon(ColorStateList.valueOf(Color.argb(128, 10, 80, 160)))
        icon.setBounds(0, 0, 24, 24)
        icon.alpha = 128
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        icon.draw(Canvas(bitmap))
        val pixel = bitmap.getPixel(12, 6)
        assertEquals(64, Color.alpha(pixel))
        assertTrue(Color.blue(pixel) > Color.green(pixel))
    }
}
