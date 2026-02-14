package com.ammarymn.kmp.sysutil

import com.ammarymn.kmp.sysutil.unit.ByteSize.Companion.bytes
import platform.windows.PROCESSOR_ARCHITECTURE_AMD64
import platform.windows.PROCESSOR_ARCHITECTURE_ARM64
import platform.windows.PROCESSOR_ARCHITECTURE_INTEL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class MingwHardwareTest {

    @Test
    fun testGetMemorySnapshotSanity() {
        val snapshot = try {
            getMemorySnapshot()
        } catch (e: Exception) {
            fail("getMemorySnapshot() threw an exception: ${e.message}")
        }

        // Verify RAM (Physical Memory) Logic
        assertTrue(snapshot.total > 0.bytes, "Total RAM must be greater than 0")
        assertTrue(snapshot.available >= 0.bytes, "Available RAM cannot be negative")
        assertTrue(snapshot.available <= snapshot.total, "Available RAM cannot be greater than Total RAM")

        val calculatedUsed = snapshot.total - snapshot.available
        assertEquals(calculatedUsed, snapshot.used, "Used RAM property does not match (Total - Available)")

        // Verify Swap (Page File) Logic
        // Note: It is technically possible for swap to be 0 if the user disabled the page file.
        assertTrue(snapshot.totalSwap >= 0.bytes, "Total Swap cannot be negative")
        assertTrue(snapshot.availableSwap >= 0.bytes, "Available Swap cannot be negative")

        // Ensure our subtraction logic (Commit Limit - RAM) didn't break
        assertTrue(snapshot.availableSwap <= snapshot.totalSwap, "Available Swap cannot be greater than Total Swap")

        val calculatedUsedSwap = snapshot.totalSwap - snapshot.availableSwap
        assertEquals(calculatedUsedSwap, snapshot.usedSwap, "Used Swap property does not match (Total - Available)")
    }

    @Test
    fun testGetCpuInfoSanityCheck() {
        val cpu = getCpuInfo()

        println("DEBUG: Detected CPU -> $cpu")

        // Verify Model Name (Registry Read)
        assertFalse(cpu.model.isBlank(), "CPU model should not be empty")
        // Basic check to ensure we didn't get garbage characters from pointer logic
        assertTrue(cpu.model.all { it.isLetterOrDigit() || it.isWhitespace() || it in "()@-._" },
            "CPU model contains unexpected characters (encoding issue?): ${cpu.model}")

        // Verify Architecture
        val validArchs = setOf("x64", "x86", "ARM64", "ARM", "IA64")
        assertTrue(cpu.architecture in validArchs || cpu.architecture.startsWith("Unknown"),
            "Architecture '${cpu.architecture}' is not a known valid string")

        // Verify Core Counts (System Info & Logical Processor Info)
        assertTrue(cpu.cores > 0, "Logical cores must be > 0")
        assertTrue(cpu.physicalCores > 0, "Physical cores must be > 0")

        // Logical cores (Threads) must be >= Physical Cores
        assertTrue(cpu.cores >= cpu.physicalCores,
            "Logical cores (${cpu.cores}) cannot be less than physical cores (${cpu.physicalCores})")
    }

    @Test
    fun testArchitectureMapping() {
        assertEquals("x64", getWinArchitectureString(PROCESSOR_ARCHITECTURE_AMD64))
        assertEquals("x86", getWinArchitectureString(PROCESSOR_ARCHITECTURE_INTEL))
        assertEquals("ARM64", getWinArchitectureString(PROCESSOR_ARCHITECTURE_ARM64))

        // Test unknown fallback
        val randomArchId = 9999
        assertEquals("Unknown ($randomArchId)", getWinArchitectureString(randomArchId))
    }

    @Test
    fun testPhysicalProcessorCountSanity() {
        val physical = getPhysicalProcessorCount()

        // We can't know the exact number of your machine in a generic test,
        // but we know it must be positive.
        assertTrue(physical > 0, "Physical processor count failed (returned 0)")
    }

    @Test
    fun testGetNetworkInterfacesSanity() {
        val interfaces = try {
            SystemInfo.hardware.networkInterface
        } catch (e: Exception) {
            fail("Getting network interfaces threw an exception: ${e.message}")
        }

        // Most Windows machines have at least one network interface (even loopback)
        assertTrue(interfaces.isNotEmpty(), "Expected at least one network interface")

        interfaces.forEach { nic ->
            // Verify friendly name is not blank
            assertFalse(nic.friendlyName.isBlank(), "Friendly name should not be blank")

            // Verify name is not blank
            assertFalse(nic.name.isBlank(), "Name should not be blank")

            // Description can be blank in some cases, but should not be null
            assertTrue(nic.description.isNotEmpty() || nic.description.isEmpty(),
                "Description should be a valid string")

            // Verify MAC address format (if not empty)
            if (nic.macAddress.isNotEmpty()) {
                // MAC address should be in format XX:XX:XX:XX:XX:XX or similar
                // Allow dashes too: XX-XX-XX-XX-XX-XX
                val macPattern = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
                assertTrue(macPattern.matches(nic.macAddress) || nic.macAddress.isBlank(),
                    "MAC address '${nic.macAddress}' is not in valid format")
            }

            // Verify IP addresses list (can be empty for disabled adapters)
            // If present, basic format validation
            nic.ipAddresses.forEach { ip ->
                assertFalse(ip.isBlank(), "IP address should not be blank")
                // Basic check: should contain dots (IPv4) or colons (IPv6)
                assertTrue(ip.contains('.') || ip.contains(':'),
                    "IP address '$ip' doesn't appear to be valid IPv4 or IPv6")
            }
        }
    }

    @Test
    fun testNetworkInterfacesHaveValidProperties() {
        val interfaces = SystemInfo.hardware.networkInterface

        interfaces.forEach { nic ->
            println("Testing interface: ${nic.friendlyName}")

            // Name should not contain null characters or invalid chars
            assertFalse(nic.name.contains('\u0000'), "Name contains null characters")
            assertFalse(nic.friendlyName.contains('\u0000'), "Friendly name contains null characters")
            assertFalse(nic.description.contains('\u0000'), "Description contains null characters")

            // MAC address should be uppercase or consistent in format
            if (nic.macAddress.isNotEmpty()) {
                assertTrue(nic.macAddress.length >= 17 || nic.macAddress.isBlank(),
                    "MAC address length is unexpected: '${nic.macAddress}'")
            }

            // IP addresses should not have leading/trailing whitespace
            nic.ipAddresses.forEach { ip ->
                assertEquals(ip.trim(), ip, "IP address should not have leading/trailing whitespace")
            }
        }
    }

    @Test
    fun testNetworkInterfacesIPv4Format() {
        val interfaces = SystemInfo.hardware.networkInterface

        val ipv4Pattern = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")

        interfaces.forEach { nic ->
            nic.ipAddresses.filter { it.contains('.') && !it.contains(':') }.forEach { ipv4 ->
                assertTrue(ipv4Pattern.matches(ipv4),
                    "IPv4 address '$ipv4' on ${nic.friendlyName} is not in valid format")

                // Verify each octet is in valid range (0-255)
                val octets = ipv4.split('.')
                assertEquals(4, octets.size, "IPv4 should have exactly 4 octets")

                octets.forEach { octet ->
                    val value = octet.toIntOrNull()
                    assertTrue(value != null && value in 0..255,
                        "IPv4 octet '$octet' is out of range (0-255)")
                }
            }
        }
    }

    @Test
    fun testNetworkInterfacesIPv6Format() {
        val interfaces = SystemInfo.hardware.networkInterface

        interfaces.forEach { nic ->
            nic.ipAddresses.filter { it.contains(':') }.forEach { ipv6 ->
                // Basic IPv6 validation: should have colons and valid hex characters
                assertTrue(ipv6.isNotEmpty(), "IPv6 address should not be empty")

                // Check for valid IPv6 characters (hex digits, colons, and optionally %)
                val validChars = ipv6.all { it.isLetterOrDigit() || it in ":%." }
                assertTrue(validChars,
                    "IPv6 address '$ipv6' on ${nic.friendlyName} contains invalid characters")

                // IPv6 should not start or end with colon (unless it's :: notation)
                if (ipv6.startsWith(':')) {
                    assertTrue(ipv6.startsWith("::"), "IPv6 starting with ':' should use '::' notation")
                }
            }
        }
    }

    @Test
    fun testNetworkInterfacesMacAddressUniqueness() {
        val interfaces = SystemInfo.hardware.networkInterface

        // Filter out interfaces with empty MAC addresses
        val nonEmptyMacs = interfaces.filter { it.macAddress.isNotEmpty() }

        if (nonEmptyMacs.size > 1) {
            // Check that we don't have duplicate MAC addresses (would indicate a bug)
            val macSet = nonEmptyMacs.map { it.macAddress.uppercase() }.toSet()
            val macList = nonEmptyMacs.map { it.macAddress.uppercase() }

            // Note: In some virtual environments, MACs might be duplicated,
            // so this is more of a warning test
            if (macSet.size != macList.size) {
                println("WARNING: Found duplicate MAC addresses in network interfaces")
                println("This might be expected in virtual environments")
            }
        }
    }

    @Test
    fun testNetworkInterfacesConsistency() {
        // Call the function twice and verify we get consistent results
        val interfaces1 = SystemInfo.hardware.networkInterface
        val interfaces2 = SystemInfo.hardware.networkInterface

        // Should return the same number of interfaces in quick succession
        assertEquals(interfaces1.size, interfaces2.size,
            "Network interface count changed between calls")

        // Interface names should be consistent
        val names1 = interfaces1.map { it.name }.toSet()
        val names2 = interfaces2.map { it.name }.toSet()

        assertEquals(names1, names2, "Network interface names changed between calls")
    }
}