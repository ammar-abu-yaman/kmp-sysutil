package com.ammarymn.kmp.sysutil.util

import platform.windows.*
import kotlinx.cinterop.*


@OptIn(ExperimentalForeignApi::class)
internal fun readWinRegistryString(key: String, valueName: String): String = memScoped {
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