package com.ammarymn.kmp.sysinfo.model

data class Cpu(
    val model: String,        // e.g., "Intel(R) Core(TM) i7-12700H"
    val cores: Int,           // Logical cores (threads)
    val architecture: String  // e.g., "x64", "aarch64"
)