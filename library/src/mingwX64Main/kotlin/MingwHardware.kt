@file:OptIn(ExperimentalForeignApi::class)
package com.ammarymn.kmp.sysutil

import com.ammarymn.kmp.sysutil.model.hardware.Cpu
import com.ammarymn.kmp.sysutil.model.hardware.StorageVolume
import com.ammarymn.kmp.sysutil.model.hardware.Memory
import com.ammarymn.kmp.sysutil.model.hardware.PowerStatus
import com.ammarymn.kmp.sysutil.unit.ByteSize.Companion.bytes
import com.ammarymn.kmp.sysutil.util.readWinRegistryString
import kotlinx.cinterop.*
import kotlinx.cinterop.get
import platform.windows.*
import kotlin.time.Duration.Companion.seconds

internal const val PROCESSOR_REGISTRY_KEY = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0"

object MingwHardware: Hardware {
    override val memory get() = getMemorySnapshot()
    override val cpu get() = getCpuInfo()
    override val volumes get() = getStorageInfo()
    override val power get() = getPowerInfo()
}

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
        total = totalRam.bytes,
        available = availableRam.bytes,
        totalSwap = (totalCommit - totalRam).bytes,
        availableSwap = (availableCommit - availableRam).bytes,
    )
}

internal fun getCpuInfo(): Cpu = memScoped {
    val sysInfo = alloc<SYSTEM_INFO>()

    GetNativeSystemInfo(sysInfo.ptr)

    val cores = sysInfo.dwNumberOfProcessors.toInt()
    val physicalCores = getPhysicalProcessorCount()
    val architecture = getWinArchitectureString(sysInfo.wProcessorArchitecture.toInt())
    val model = readWinRegistryString(PROCESSOR_REGISTRY_KEY, "ProcessorNameString")

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
    val drives = parseWCharArray(buffer)
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
            totalSize = totalNumberOfBytes.QuadPart.toLong().bytes,
            availableSize = freeBytesAvailable.QuadPart.toLong().bytes
        )
        volumes.add(volume)
    }

    volumes
}

internal fun getPowerInfo(): PowerStatus = memScoped {
    val status = alloc<SYSTEM_POWER_STATUS>()

    // 1. Call the API
    if (GetSystemPowerStatus(status.ptr) == 0) {
        // If it fails (rare), return a default "Desktop" state
        return PowerStatus(
            hasBattery = false,
            isPluggedIn = true,
            isCharging = false,
            batteryLevel = 100,
            timeRemaining = null
        )
    }

    // 2. Parse "ACLineStatus" (1 Byte)
    // 0 = Offline (Battery), 1 = Online (Plugged In), 255 = Unknown
    val isPluggedIn = status.ACLineStatus.toInt() == 1

    // 3. Parse "BatteryFlag" (1 Byte)
    // Bitfield:
    // 1=High, 2=Low, 4=Critical, 8=Charging, 128=No System Battery, 255=Unknown
    val flag = status.BatteryFlag.toInt()
    val hasBattery = (flag and 128) == 0 && (flag != 255)
    val isCharging = (flag and 8) != 0

    // 4. Parse "BatteryLifePercent" (1 Byte)
    // 0-100, or 255 if unknown
    val rawLevel = status.BatteryLifePercent.toInt()
    val batteryLevel = if (rawLevel in 0..100) rawLevel else 100

    // 5. Parse "BatteryLifeTime" (Int/Long)
    // Number of seconds remaining, or -1 if unknown (e.g., charging or calculating)
    val secondsRemaining = status.BatteryLifeTime.toInt()
    val timeRemaining = if (secondsRemaining != -1) secondsRemaining.seconds else null


    return PowerStatus(
        hasBattery = hasBattery,
        isPluggedIn = isPluggedIn,
        isCharging = isCharging,
        batteryLevel = batteryLevel,
        timeRemaining = timeRemaining
    )
}

private fun parseWCharArray(buffer: CPointer<UShortVar>): List<String> {
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