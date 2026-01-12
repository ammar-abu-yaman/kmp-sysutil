import com.ammarymn.kmp.sysinfo.getMemorySnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class MingwHardwareTest {

    @Test
    fun testGetMemorySnapshotSanity() {
        // 1. Execute the function
        val snapshot = try {
            getMemorySnapshot()
        } catch (e: Exception) {
            fail("getMemorySnapshot() threw an exception: ${e.message}")
        }

        // 3. Verify RAM (Physical Memory) Logic
        assertTrue(snapshot.total > 0, "Total RAM must be greater than 0")
        assertTrue(snapshot.available >= 0, "Available RAM cannot be negative")
        assertTrue(snapshot.available <= snapshot.total, "Available RAM cannot be greater than Total RAM")

        val calculatedUsed = snapshot.total - snapshot.available
        assertEquals(calculatedUsed, snapshot.used, "Used RAM property does not match (Total - Available)")

        // 4. Verify Swap (Page File) Logic
        // Note: It is technically possible for swap to be 0 if the user disabled the page file.
        assertTrue(snapshot.totalSwap >= 0, "Total Swap cannot be negative")
        assertTrue(snapshot.availableSwap >= 0, "Available Swap cannot be negative")

        // Ensure our subtraction logic (Commit Limit - RAM) didn't break
        assertTrue(snapshot.availableSwap <= snapshot.totalSwap, "Available Swap cannot be greater than Total Swap")

        val calculatedUsedSwap = snapshot.totalSwap - snapshot.availableSwap
        assertEquals(calculatedUsedSwap, snapshot.usedSwap, "Used Swap property does not match (Total - Available)")
    }
}