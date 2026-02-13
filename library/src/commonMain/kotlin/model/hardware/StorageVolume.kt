package com.ammarymn.kmp.sysutil.model.hardware

import com.ammarymn.kmp.sysutil.unit.ByteSize

data class StorageVolume(
    val mountPoint: String,    // e.g., "C:\"
    val label: String,         // e.g., "Windows", "Data", "USB Drive"
    val fileSystem: String,    // e.g., "NTFS", "FAT32"
    val totalSize: ByteSize,
    val availableSize: ByteSize,
    val totalFreeSize: ByteSize = totalSize - availableSize
)