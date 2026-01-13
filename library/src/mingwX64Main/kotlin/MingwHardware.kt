@file:OptIn(ExperimentalForeignApi::class)
package com.ammarymn.kmp.sysinfo

import com.ammarymn.kmp.sysinfo.model.Cpu
import com.ammarymn.kmp.sysinfo.model.Memory
import kotlinx.cinterop.*
import platform.windows.*

internal const val PROCESSOR_REGISTRY_KEY = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0"

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

internal fun getCpuInfo(): Cpu = memScoped {
    val sysInfo = alloc<SYSTEM_INFO>()

    GetNativeSystemInfo(sysInfo.ptr)

    val cores = sysInfo.dwNumberOfProcessors.toInt()
    val physicalCores = getPhysicalProcessorCount()
    val architecture = getWinArchitectureString(sysInfo.wProcessorArchitecture.toInt())
    val model = getWinRegistryString(PROCESSOR_REGISTRY_KEY, "ProcessorNameString")

    Cpu(
        model,
        cores,
        physicalCores,
        architecture,
    )
}

internal fun getWinArchitectureString(architecture: Int) = when(architecture) {
    PROCESSOR_ARCHITECTURE_AMD64 -> "x64"
    PROCESSOR_ARCHITECTURE_INTEL -> "x86"
    PROCESSOR_ARCHITECTURE_ARM64 -> "ARM64"
    PROCESSOR_ARCHITECTURE_ARM   -> "ARM"
    PROCESSOR_ARCHITECTURE_IA64  -> "IA64" // Itanium
    else -> "Unknown ($architecture)"
}

internal fun getWinRegistryString(key: String, valueName: String): String = memScoped {
    val keyHandle = alloc<HKEYVar>()

    // Use ExW for Unicode support.
    if (RegOpenKeyExW(
            HKEY_LOCAL_MACHINE,
            key,
            0u,
            KEY_READ.toUInt(),
            keyHandle.ptr
        ) != ERROR_SUCCESS)
        throw Exception("Unknown (Open Failed)")


    try {
        val dataSize = alloc<DWORDVar>()

        if (RegQueryValueExW(
                keyHandle.value,
                valueName,
                null,
                null,
                null,
                dataSize.ptr) != ERROR_SUCCESS) {
            throw Exception("Unknown (Query Size Failed)")
        }

        val sizeInBytes = dataSize.value.toInt()
        val buffer = allocArray<UByteVar>(sizeInBytes)

        if (RegQueryValueExW(keyHandle.value,
                valueName,
                null,
                null,
                buffer,
                dataSize.ptr) == ERROR_SUCCESS) {
            // CRITICAL STEP:
            // Reinterpret the byte pointer as a UShort (UTF-16 char) pointer,
            // then convert to Kotlin String.
            return buffer.reinterpret<UShortVar>().toKStringFromUtf16()
        }

        throw Exception("Unknown (Read Failed)")

    } finally {
        RegCloseKey(keyHandle.value)
    }
}

internal fun getPhysicalProcessorCount(): Int = memScoped {
    val returnLength = alloc<DWORDVar>()
    returnLength.value = 0u

    // Will populate the required length in returnLength.
    GetLogicalProcessorInformation(null, returnLength.ptr)

    if(GetLastError().toInt() != ERROR_INSUFFICIENT_BUFFER)
        return 1

    val sizeInBytes = returnLength.value.toInt()
    val buffer = allocArray<ByteVar>(sizeInBytes)
    val ptr = buffer.reinterpret<SYSTEM_LOGICAL_PROCESSOR_INFORMATION>()

    if(GetLogicalProcessorInformation(ptr, returnLength.ptr) == 0)
        throw Exception("Failed to get physical processor information")

    val structSize = sizeOf<SYSTEM_LOGICAL_PROCESSOR_INFORMATION>().toInt()
    val count = sizeInBytes / structSize

    var physicalCores = 0
    for(i in 0..<count) {
        val info = ptr[i]
        if(info.Relationship == RelationProcessorCore)
            physicalCores++
    }

    physicalCores
}