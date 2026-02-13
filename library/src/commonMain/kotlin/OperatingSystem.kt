package com.ammarymn.kmp.sysutil

import kotlin.time.Duration

interface OperatingSystem {
    val family: String          // "Windows"
    val version: String         // "11 Pro"
    val buildNumber: String     // "22621"
    val uptime: Duration        // 5.hours
    val processId: Int          // 1234
    val isElevated: Boolean     // true (if Admin/Root)
}