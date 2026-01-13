package com.ammarymn.kmp.sysutil

internal actual val platformHardware = object: Hardware {
    override val memory
        get() = getMemorySnapshot()

    override val cpu
        get() = getCpuInfo()
}
