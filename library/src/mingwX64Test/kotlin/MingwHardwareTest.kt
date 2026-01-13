import com.ammarymn.kmp.sysinfo.*
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
        assertTrue(snapshot.total > 0, "Total RAM must be greater than 0")
        assertTrue(snapshot.available >= 0, "Available RAM cannot be negative")
        assertTrue(snapshot.available <= snapshot.total, "Available RAM cannot be greater than Total RAM")

        val calculatedUsed = snapshot.total - snapshot.available
        assertEquals(calculatedUsed, snapshot.used, "Used RAM property does not match (Total - Available)")

        // Verify Swap (Page File) Logic
        // Note: It is technically possible for swap to be 0 if the user disabled the page file.
        assertTrue(snapshot.totalSwap >= 0, "Total Swap cannot be negative")
        assertTrue(snapshot.availableSwap >= 0, "Available Swap cannot be negative")

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
}