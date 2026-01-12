package com.ammarymn.kmp.sysinfo

import kotlin.test.Test
import kotlin.test.assertTrue

class HardwareTest {

    @Test
    fun `Test hardware memory`() {
        val memory = SystemInfo.hardware.memory

        assertTrue(memory.total > 0, "Total memory should be > 0")
        assertTrue(memory.available >= 0, "Available memory should be >= 0")
        assertTrue(memory.available <= memory.total, "Available cannot be > Total")
    }

}