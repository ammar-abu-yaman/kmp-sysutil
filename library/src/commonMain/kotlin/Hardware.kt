package com.ammarymn.kmp.sysinfo

import com.ammarymn.kmp.sysinfo.model.Cpu
import com.ammarymn.kmp.sysinfo.model.Memory

interface Hardware {
    val memory: Memory
    val cpu: Cpu
}