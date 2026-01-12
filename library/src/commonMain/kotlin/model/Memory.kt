package com.ammarymn.kmp.sysinfo.model

data class Memory(
    val total: Long,
    val available: Long,
    val totalSwap: Long,
    val availableSwap: Long,
    val used: Long = total - available,
    val usedSwap: Long = totalSwap - availableSwap
)