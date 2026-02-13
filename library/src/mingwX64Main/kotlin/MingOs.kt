package com.ammarymn.kmp.sysutil

import com.ammarymn.kmp.sysutil.model.hardware.OsFamily
import com.ammarymn.kmp.sysutil.util.readWinRegistryString
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import platform.windows.AllocateAndInitializeSid
import platform.windows.BOOLVar
import platform.windows.CheckTokenMembership
import platform.windows.FreeSid
import platform.windows.GetCurrentProcessId
import platform.windows.GetTickCount64
import platform.windows.PSIDVar
import platform.windows.SID_IDENTIFIER_AUTHORITY
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

import kotlinx.cinterop.*

private const val VERSION_REGISTRY_KEY = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion"

object MingOs: OperatingSystem {
    override val family: OsFamily get() = OsFamily.Windows
    override val version: String get() = getOsVersion()
    override val buildNumber: String
        get() = runCatching {
            readWinRegistryString(VERSION_REGISTRY_KEY, "CurrentBuild")
        }.getOrDefault("0")
    override val uptime: Duration get() = getSystemUpTimeMillis().toLong().milliseconds
    override val processId: Int get() = getProcessId().toInt()
    override val isPrivileged: Boolean get() = isProcessElevated()
}

internal fun getSystemUpTimeMillis(): ULong = GetTickCount64()

internal fun getProcessId(): UInt = GetCurrentProcessId()

internal fun getOsVersion(): String {
    val productName = runCatching {
        readWinRegistryString(VERSION_REGISTRY_KEY, "ProductName")
    }.getOrDefault("Windows")

    val currentBuild = runCatching {
        readWinRegistryString(VERSION_REGISTRY_KEY, "CurrentBuild")
    }.getOrDefault("0")

    // 3. Apply the "Windows 11" logic (Fixing the Microsoft lie)
    var finalName = productName
    val buildNum = currentBuild.toIntOrNull() ?: 0

    // Windows 11 starts at build 22000, but Registry often still says "Windows 10"
    if (buildNum >= 22000 && productName.startsWith("Windows 10")) {
        finalName = productName.replace("Windows 10", "Windows 11")
    }
    return finalName
}

@OptIn(ExperimentalForeignApi::class)
internal fun isProcessElevated(): Boolean = memScoped {
    var isElevated = false

    // 1. Setup the Identifier Authority
    // SECURITY_NT_AUTHORITY is defined as {0, 0, 0, 0, 0, 5}
    val ntAuthority = alloc<SID_IDENTIFIER_AUTHORITY>()
    ntAuthority.Value[5] = 5u

    // 2. Allocate space for the SID pointer
    // PSIDVar is a pointer to a SID (Security Identifier)
    val administratorsGroup = alloc<PSIDVar>()

    // 3. Create the "Administrators" SID
    // We are asking Windows for the SID: S-1-5-32-544
    // 0x20  = SECURITY_BUILTIN_DOMAIN_RID
    // 0x220 = DOMAIN_ALIAS_RID_ADMINS (The "Administrators" group)
    if (AllocateAndInitializeSid(
            ntAuthority.ptr,
            2u,          // We are using 2 Sub-Authorities (0x20 and 0x220)
            0x20u,
            0x220u,
            0u, 0u, 0u, 0u, 0u, 0u, // Remaining 6 are unused
            administratorsGroup.ptr
        ) != 0) {

        // 4. Check if the current process token includes this SID
        // CheckTokenMembership(null, ...) checks the calling thread's token.
        val isMember = alloc<BOOLVar>()
        if (CheckTokenMembership(null, administratorsGroup.value, isMember.ptr) != 0) {
            // If the call succeeds, check the boolean result
            isElevated = (isMember.value != 0)
        }

        // 5. Free the SID
        // Important: SIDs created by AllocateAndInitializeSid must be freed
        // using FreeSid, not the standard C free().
        FreeSid(administratorsGroup.value)
    }

    return isElevated
}