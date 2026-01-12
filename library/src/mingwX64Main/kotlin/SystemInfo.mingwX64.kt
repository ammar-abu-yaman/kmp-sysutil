package com.ammarymn.kmp.sysinfo

actual object SystemInfo {
    actual val hardware = object: Hardware {
        override val memory
            get() = getMemorySnapshot()
    }
}

