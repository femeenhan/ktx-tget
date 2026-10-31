package dev.ktxtget.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilitySettingsTest {
    private val long = "dev.ktxtget/dev.ktxtget.service.KtxAccessibilityService"
    private val short = "dev.ktxtget/.service.KtxAccessibilityService"

    private fun on(list: String?): Boolean = AccessibilitySettings.isEnabledIn(list, long, short)

    @Test
    fun `null or blank list means off`() {
        assertFalse(on(null))
        assertFalse(on(""))
    }

    @Test
    fun `long form entry among others is on`() {
        val list = "com.samsung.android.app.talkback/.TalkBackService:" +
            "dev.ktxtget/dev.ktxtget.service.KtxAccessibilityService"
        assertTrue(on(list))
    }

    @Test
    fun `short form entry is on`() {
        assertTrue(on("dev.ktxtget/.service.KtxAccessibilityService"))
    }

    @Test
    fun `other package only is off`() {
        assertFalse(on("com.other/.service.KtxAccessibilityService"))
    }
}
