package com.ammarymn.kmp.sysutil.model.hardware

import com.ammarymn.kmp.sysutil.unit.ByteSize

data class Memory(
    val total: ByteSize,
    val available: ByteSize,
    val totalSwap: ByteSize,
    val availableSwap: ByteSize,
    val used: ByteSize = total - available,
    val usedSwap: ByteSize = totalSwap - availableSwap
)