package com.ammarymn.kmp.sysutil

import com.ammarymn.kmp.sysutil.model.Cpu
import com.ammarymn.kmp.sysutil.model.Memory

interface Hardware {
    val memory: Memory
    val cpu: Cpu
}