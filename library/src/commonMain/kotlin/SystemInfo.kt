package com.ammarymn.kmp.sysutil

object SystemInfo {
    val hardware: Hardware = platformHardware
}

internal expect val platformHardware: Hardware