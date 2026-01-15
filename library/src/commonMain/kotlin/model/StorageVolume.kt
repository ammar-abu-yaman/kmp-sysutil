package com.ammarymn.kmp.sysutil.model

data class StorageVolume(
    val mountPoint: String,    // e.g., "C:\"
    val label: String,         // e.g., "Windows", "Data", "USB Drive"
    val fileSystem: String,    // e.g., "NTFS", "FAT32"
    val totalBytes: Long,
    val availableBytes: Long,
    val totalFreeBytes: Long = totalBytes - availableBytes
)