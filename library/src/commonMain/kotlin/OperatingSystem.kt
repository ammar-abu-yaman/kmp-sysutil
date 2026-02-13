package com.ammarymn.kmp.sysutil

import com.ammarymn.kmp.sysutil.model.hardware.OsFamily
import kotlin.time.Duration

interface OperatingSystem {
    val family: OsFamily          // "Windows"
    val version: String         // "11 Pro"
    val buildNumber: String     // "22621"
    val uptime: Duration        // 5.hours
    val processId: Int          // 1234
    val isPrivileged: Boolean     // true (if Admin/Root)
}