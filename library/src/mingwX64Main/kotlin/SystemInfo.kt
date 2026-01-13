package com.ammarymn.kmp.sysinfo

internal actual val platformHardware = object: Hardware {
    override val memory
        get() = getMemorySnapshot()

    override val cpu
        get() = getCpuInfo()
}
