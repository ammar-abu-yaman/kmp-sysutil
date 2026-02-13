package com.ammarymn.kmp.sysutil

import getCpuInfo
import getMemorySnapshot
import getStorageInfo

internal actual val platformHardware = object: Hardware {
    override val memory
        get() = getMemorySnapshot()

    override val cpu
        get() = getCpuInfo()

    override val volumes
        get() = getStorageInfo()
}
