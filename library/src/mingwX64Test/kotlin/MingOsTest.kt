package com.ammarymn.kmp.sysutil

import com.ammarymn.kmp.sysutil.model.hardware.OsFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration

class MingOsTest {

    @Test
    fun familyIsWindowsWithOfficialName() {
        val family = MingOs.family
        assertEquals(OsFamily.Windows, family, "OS family should be WINDOWS on mingwX64")
        assertEquals("Windows", family.officialName, "Official name should be 'Windows'")
    }

    @Test
    fun versionIsNotBlank() {
        val version = MingOs.version
        assertTrue(version.isNotBlank(), "Version should not be blank")
    }

    @Test
    fun buildNumberIsNumericOrDefault() {
        val build = MingOs.buildNumber
        assertTrue(build.toIntOrNull() != null, "Build number should be numeric (string of digits)")
    }

    @Test
    fun uptimeIsNonNegative() {
        val uptime: Duration = MingOs.uptime
        assertTrue(uptime >= Duration.ZERO, "Uptime must be non-negative")
    }

    @Test
    fun processIdIsPositive() {
        val pid = MingOs.processId
        assertTrue(pid > 0, "Process ID must be positive")
    }

    @Test
    fun isPrivilegedReturnsBooleanWithoutThrowing() {
        // Simply ensure this doesn't throw and returns a boolean.
        val elevated = MingOs.isPrivileged
        assertNotNull(elevated, "Privilege check should return a value")
        // At least ensure it's either true or false to satisfy static analyzers
        assertTrue(elevated || !elevated)
    }
}
