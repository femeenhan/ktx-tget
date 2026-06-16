package dev.ktxtget.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLicenseTest {
    @Test
    fun deviceIdsMatch_when_equal_after_normalization() {
        assertTrue(DeviceLicense.deviceIdsMatch("AbC123", "abc123"))
    }

    @Test
    fun deviceIdsMatch_when_whitespace_trimmed() {
        assertTrue(DeviceLicense.deviceIdsMatch(" abc123 ", "abc123"))
    }

    @Test
    fun deviceIdsMatch_returns_false_when_allowed_empty() {
        assertFalse(DeviceLicense.deviceIdsMatch("", "abc123"))
    }

    @Test
    fun deviceIdsMatch_returns_false_when_current_empty() {
        assertFalse(DeviceLicense.deviceIdsMatch("abc123", ""))
    }

    @Test
    fun deviceIdsMatch_returns_false_when_different() {
        assertFalse(DeviceLicense.deviceIdsMatch("abc123", "def456"))
    }
}
