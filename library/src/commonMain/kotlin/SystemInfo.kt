package com.ammarymn.kmp.sysinfo

object SystemInfo {
    val hardware: Hardware = platformHardware
}

internal expect val platformHardware: Hardware