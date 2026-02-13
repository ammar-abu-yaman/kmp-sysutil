package com.ammarymn.kmp.sysutil

object SystemInfo {
    val hardware: Hardware = platformHardware
    val os: OperatingSystem = platformOs
}

internal expect val platformHardware: Hardware
internal expect val platformOs: OperatingSystem