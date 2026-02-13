package com.ammarymn.kmp.sysutil.model.hardware

import kotlin.time.Duration

data class PowerStatus(
    val hasBattery: Boolean,      // True for Laptops, False for Desktops
    val isPluggedIn: Boolean,     // True if AC adapter is connected
    val isCharging: Boolean,      // True if battery is filling up
    val batteryLevel: Int,        // 0 to 100 (%)
    val timeRemaining: Duration?  // Null if calculating or unknown (e.g. Desktop)
)