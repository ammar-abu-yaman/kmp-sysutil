package com.ammarymn.kmp.sysutil

import com.ammarymn.kmp.sysutil.model.hardware.Cpu
import com.ammarymn.kmp.sysutil.model.hardware.StorageVolume
import com.ammarymn.kmp.sysutil.model.hardware.Memory
import com.ammarymn.kmp.sysutil.model.hardware.PowerStatus

interface Hardware {
    val memory: Memory
    val cpu: Cpu
    val volumes: List<StorageVolume>
    val power: PowerStatus
}