package com.justinlin099.pdx206statusbarhookyaap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BrandingResourcesTest {
    @Test fun adaptiveIconsLoadWithThemedLayerAndRootMarkerInsideSafeCircle() {
        val context = RuntimeEnvironment.getApplication()
        for (id in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
            val icon = context.getDrawable(id) as AdaptiveIconDrawable
            assertNotNull(icon.monochrome)
            val foreground = icon.foreground
            foreground.setBounds(0, 0, 432, 432)
            val bitmap = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
            foreground.draw(Canvas(bitmap))
            for (y in 0 until 432) for (x in 0 until 432) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 32) {
                    val dx = (x + 0.5) / 4 - 54
                    val dy = (y + 0.5) / 4 - 54
                    assertTrue("Foreground must survive circular launcher masks", dx*dx + dy*dy < 33*33)
                }
            }
            assertEquals("Root badge", Color.rgb(24,38,61), bitmap.getPixel(40*4, 40*4))
            assertEquals("White # stroke", Color.WHITE, bitmap.getPixel(40*4, 38*4))
            if (id == R.mipmap.ic_launcher) {
                val directory = File("build/branding-preview").apply { mkdirs() }
                File(directory, "android-foreground.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }
}
