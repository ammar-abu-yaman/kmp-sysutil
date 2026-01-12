@file:OptIn(ExperimentalForeignApi::class)
package com.ammarymn.kmp.sysinfo

import com.ammarymn.kmp.sysinfo.model.Memory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import platform.windows.GlobalMemoryStatusEx
import platform.windows.MEMORYSTATUSEX

internal fun getMemorySnapshot(): Memory = memScoped {
    val memInfo = alloc<MEMORYSTATUSEX>()
    memInfo.dwLength = sizeOf<MEMORYSTATUSEX>().toUInt()

    if(GlobalMemoryStatusEx(memInfo.ptr) == 0)
        throw Exception("Unable to get memory status")

    val totalRam = memInfo.ullTotalPhys.toLong()
    val availableRam = memInfo.ullAvailPhys.toLong()

    // Windows "PageFile" value is actually "Commit Limit" (RAM + Swap).
    // To get just the Swap (disk) size, we subtract RAM from the Commit Limit.
    // Note: This is an approximation, but standard for system monitors.
    val totalCommit = memInfo.ullTotalPageFile.toLong()
    val availableCommit = memInfo.ullAvailPageFile.toLong()

    Memory(
        total = totalRam,
        available = availableRam,
        totalSwap = totalCommit - totalRam,
        availableSwap = availableCommit - availableRam,
    )
}