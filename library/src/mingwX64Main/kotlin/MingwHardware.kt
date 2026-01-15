@file:OptIn(ExperimentalForeignApi::class)
package com.ammarymn.kmp.sysutil

import com.ammarymn.kmp.sysutil.model.Cpu
import com.ammarymn.kmp.sysutil.model.StorageVolume
import com.ammarymn.kmp.sysutil.model.Memory
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

internal fun getStorageInfo(): List<StorageVolume> = memScoped {
    val volumes = mutableListOf<StorageVolume>()

    val bufferSize = GetLogicalDriveStringsW(0u, null).toInt()
    if(bufferSize == 0)
        throw Exception("Failed to get logical drive string buffer size")

    val buffer = allocArray<WCHARVar>(bufferSize.toInt())
    GetLogicalDriveStringsW(bufferSize.toUInt(), buffer)
    val drives = parseWCharArray(buffer, bufferSize.toUInt())
    for(drive in drives) {
        val volumeName = allocArray<WCHARVar>(MAX_PATH+1)
        val fileSystemName = allocArray<WCHARVar>(MAX_PATH+1)

        if(GetVolumeInformationW(
                drive,
                volumeName,
                MAX_PATH.toUInt(),
                null,
                null,
                null,
                fileSystemName,
                MAX_PATH.toUInt()) == 0)
            throw Exception("Failed to get volume information for $drive")
        val freeBytesAvailable = alloc<ULARGE_INTEGER>()
        val totalNumberOfBytes = alloc<ULARGE_INTEGER>()
        val totalNumberOfFreeBytes = alloc<ULARGE_INTEGER>()

        // Use the 'W' version explicitly
        if (GetDiskFreeSpaceExW(
                drive,
                freeBytesAvailable.ptr,
                totalNumberOfBytes.ptr,
                totalNumberOfFreeBytes.ptr
            ) == 0)
            throw Exception("Failed to get disk free space for $drive")

        val volume = StorageVolume(
            mountPoint = drive,
            label = volumeName.toKStringFromUtf16(),
            fileSystem = fileSystemName.toKStringFromUtf16(),
            totalBytes = totalNumberOfBytes.QuadPart.toLong(),
            availableBytes = freeBytesAvailable.QuadPart.toLong(),
            totalFreeBytes = totalNumberOfFreeBytes.QuadPart.toLong()
        )
        volumes.add(volume)
    }

    volumes
}

private fun parseWCharArray(buffer: CPointer<UShortVar>, len: UInt): List<String> {
    var current = buffer
    val drives = mutableListOf<String>()
    while (true) {
        val str = current.toKStringFromUtf16()
        if (str.isEmpty())
            break

        drives.add(str)
        current = current.plus(str.length + 1)!!
    }
    return drives
}