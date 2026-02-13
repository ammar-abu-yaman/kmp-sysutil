package com.ammarymn.kmp.sysutil.unit

import com.ammarymn.kmp.sysutil.unit.ByteSize.Companion.bytes
import com.ammarymn.kmp.sysutil.unit.ByteSize.Companion.gigabytes
import com.ammarymn.kmp.sysutil.unit.ByteSize.Companion.kilobytes
import com.ammarymn.kmp.sysutil.unit.ByteSize.Companion.megabytes
import com.ammarymn.kmp.sysutil.unit.ByteSize.Companion.terabytes
import com.ammarymn.kmp.sysutil.unit.ByteSize.Companion.toByteSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ByteSizeTest {

    // --- Constructor and Initialization Tests ---

    @Test
    fun `should create ByteSize with valid positive value`() {
        val size = ByteSize(1024)
        assertEquals(1024L, size.bytes)
    }

    @Test
    fun `should create ByteSize with zero value`() {
        val size = ByteSize(0)
        assertEquals(0L, size.bytes)
    }

    @Test
    fun `should throw exception for negative bytes`() {
        assertFailsWith<IllegalArgumentException> {
            ByteSize(-1)
        }
    }

    @Test
    fun `should throw exception with correct message for negative bytes`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ByteSize(-100)
        }
        assertTrue(exception.message?.contains("non-negative") == true)
    }

    // --- Comparison Tests ---

    @Test
    fun `should compare equal ByteSize values`() {
        val size1 = ByteSize(1024)
        val size2 = ByteSize(1024)
        assertEquals(0, size1.compareTo(size2))
    }

    @Test
    fun `should compare smaller ByteSize as less than larger`() {
        val smaller = ByteSize(512)
        val larger = ByteSize(1024)
        assertTrue(smaller < larger)
    }

    @Test
    fun `should compare larger ByteSize as greater than smaller`() {
        val smaller = ByteSize(512)
        val larger = ByteSize(1024)
        assertTrue(larger > smaller)
    }

    // --- Addition Tests ---

    @Test
    fun `should add two ByteSize values`() {
        val size1 = ByteSize(1024)
        val size2 = ByteSize(512)
        val result = size1 + size2
        assertEquals(1536L, result.bytes)
    }

    @Test
    fun `should add zero ByteSize`() {
        val size = ByteSize(1024)
        val result = size + ByteSize(0)
        assertEquals(1024L, result.bytes)
    }

    // --- Subtraction Tests ---

    @Test
    fun `should subtract two ByteSize values`() {
        val size1 = ByteSize(1024)
        val size2 = ByteSize(512)
        val result = size1 - size2
        assertEquals(512L, result.bytes)
    }

    @Test
    fun `should subtract equal ByteSize values to zero`() {
        val size = ByteSize(1024)
        val result = size - size
        assertEquals(0L, result.bytes)
    }

    @Test
    fun `should throw exception when subtraction results in negative`() {
        val smaller = ByteSize(512)
        val larger = ByteSize(1024)
        assertFailsWith<IllegalArgumentException> {
            smaller - larger
        }
    }

    // --- Multiplication Tests ---

    @Test
    fun `should multiply ByteSize by Int`() {
        val size = ByteSize(1024)
        val result = size * 3
        assertEquals(3072L, result.bytes)
    }

    @Test
    fun `should multiply ByteSize by Long`() {
        val size = ByteSize(1024)
        val result = size * 3L
        assertEquals(3072L, result.bytes)
    }

    @Test
    fun `should multiply ByteSize by Double`() {
        val size = ByteSize(1000)
        val result = size * 1.5
        assertEquals(1500L, result.bytes)
    }

    @Test
    fun `should multiply ByteSize by zero`() {
        val size = ByteSize(1024)
        val result = size * 0
        assertEquals(0L, result.bytes)
    }

    @Test
    fun `should support commutative multiplication with Int`() {
        val size = ByteSize(1024)
        val result = 3 * size
        assertEquals(3072L, result.bytes)
    }

    @Test
    fun `should support commutative multiplication with Long`() {
        val size = ByteSize(1024)
        val result = 3L * size
        assertEquals(3072L, result.bytes)
    }

    @Test
    fun `should support commutative multiplication with Double`() {
        val size = ByteSize(1000)
        val result = 1.5 * size
        assertEquals(1500L, result.bytes)
    }

    // --- Division Tests ---

    @Test
    fun `should divide ByteSize by Int`() {
        val size = ByteSize(1024)
        val result = size / 2
        assertEquals(512L, result.bytes)
    }

    @Test
    fun `should divide ByteSize by Long`() {
        val size = ByteSize(1024)
        val result = size / 2L
        assertEquals(512L, result.bytes)
    }

    @Test
    fun `should divide ByteSize by ByteSize returning Double`() {
        val size1 = ByteSize(1024)
        val size2 = ByteSize(512)
        val result = size1 / size2
        assertEquals(2.0, result)
    }

    @Test
    fun `should divide ByteSize by ByteSize with fractional result`() {
        val size1 = ByteSize(1000)
        val size2 = ByteSize(300)
        val result = size1 / size2
        assertEquals(3.3333333333333335, result)
    }

    // --- Conversion Tests (toLong) ---

    @Test
    fun `should convert to Long in BYTE unit`() {
        val size = ByteSize(1024)
        assertEquals(1024L, size.toLong(ByteUnit.BYTE))
    }

    @Test
    fun `should convert to Long in KILOBYTE unit`() {
        val size = ByteSize(2048)
        assertEquals(2L, size.toLong(ByteUnit.KILOBYTE))
    }

    @Test
    fun `should convert to Long in MEGABYTE unit`() {
        val size = ByteSize(2 * 1024 * 1024)
        assertEquals(2L, size.toLong(ByteUnit.MEGABYTE))
    }

    @Test
    fun `should convert to Long in GIGABYTE unit`() {
        val size = ByteSize(3L * 1024 * 1024 * 1024)
        assertEquals(3L, size.toLong(ByteUnit.GIGABYTE))
    }

    @Test
    fun `should convert to Long with truncation`() {
        val size = ByteSize(1536) // 1.5 KB
        assertEquals(1L, size.toLong(ByteUnit.KILOBYTE))
    }

    // --- Conversion Tests (toDouble) ---

    @Test
    fun `should convert to Double in BYTE unit`() {
        val size = ByteSize(1024)
        assertEquals(1024.0, size.toDouble(ByteUnit.BYTE))
    }

    @Test
    fun `should convert to Double in KILOBYTE unit`() {
        val size = ByteSize(1536)
        assertEquals(1.5, size.toDouble(ByteUnit.KILOBYTE))
    }

    @Test
    fun `should convert to Double in MEGABYTE unit`() {
        val size = ByteSize((1.5 * 1024 * 1024).toLong())
        assertEquals(1.5, size.toDouble(ByteUnit.MEGABYTE))
    }

    @Test
    fun `should convert to Double in GIGABYTE unit`() {
        val size = ByteSize((2.75 * 1024 * 1024 * 1024).toLong())
        assertEquals(2.75, size.toDouble(ByteUnit.GIGABYTE), 0.01)
    }

    // --- toString(unit, decimals) Tests ---

    @Test
    fun `should format toString with default 2 decimals`() {
        val size = ByteSize(1536)
        assertEquals("1.50 KB", size.toString(ByteUnit.KILOBYTE))
    }

    @Test
    fun `should format toString with custom decimals`() {
        val size = ByteSize((1.23456 * 1024 * 1024).toLong())
        assertEquals("1.235 MB", size.toString(ByteUnit.MEGABYTE, 3))
    }

    @Test
    fun `should format toString with zero decimals`() {
        val size = ByteSize(1536)
        assertEquals("1 KB", size.toString(ByteUnit.KILOBYTE, 0))
    }

    @Test
    fun `should format toString with one decimal`() {
        val size = ByteSize((1.56 * 1024).toLong())
        assertEquals("1.6 KB", size.toString(ByteUnit.KILOBYTE, 1))
    }

    @Test
    fun `should format toString padding decimals with zeros`() {
        val size = ByteSize(1024)
        assertEquals("1.00 KB", size.toString(ByteUnit.KILOBYTE, 2))
    }

    // --- Auto toString() Tests ---

    @Test
    fun `should auto-select BYTE unit for small values`() {
        val size = ByteSize(512)
        assertEquals("512.00 B", size.toString())
    }

    @Test
    fun `should auto-select KILOBYTE unit`() {
        val size = ByteSize(1536)
        assertEquals("1.50 KB", size.toString())
    }

    @Test
    fun `should auto-select MEGABYTE unit`() {
        val size = ByteSize(2 * 1024 * 1024)
        assertEquals("2.00 MB", size.toString())
    }

    @Test
    fun `should auto-select GIGABYTE unit`() {
        val size = ByteSize(3L * 1024 * 1024 * 1024)
        assertEquals("3.00 GB", size.toString())
    }

    @Test
    fun `should auto-select TERABYTE unit`() {
        val size = ByteSize(2L * 1024 * 1024 * 1024 * 1024)
        assertEquals("2.00 TB", size.toString())
    }

    @Test
    fun `should handle zero bytes in toString`() {
        val size = ByteSize(0)
        assertEquals("0.00 B", size.toString())
    }

    @Test
    fun `should auto-select largest applicable unit`() {
        val size = ByteSize((1.5 * 1024 * 1024 * 1024).toLong())
        assertEquals("1.50 GB", size.toString())
    }

    // --- Extension Properties Tests ---

    @Test
    fun `should create ByteSize from Int bytes extension`() {
        val size = 1024.bytes
        assertEquals(1024L, size.bytes)
    }

    @Test
    fun `should create ByteSize from Long bytes extension`() {
        val size = 2048L.bytes
        assertEquals(2048L, size.bytes)
    }

    @Test
    fun `should create ByteSize from Double bytes extension`() {
        val size = 1500.0.bytes
        assertEquals(1500L, size.bytes)
    }

    @Test
    fun `should create ByteSize from kilobytes extension`() {
        val size = 2.kilobytes
        assertEquals(2048L, size.bytes)
    }

    @Test
    fun `should create ByteSize from Double kilobytes extension`() {
        val size = 1.5.kilobytes
        assertEquals(1536L, size.bytes)
    }

    @Test
    fun `should create ByteSize from megabytes extension`() {
        val size = 2.megabytes
        assertEquals(2 * 1024 * 1024L, size.bytes)
    }

    @Test
    fun `should create ByteSize from Double megabytes extension`() {
        val size = 1.5.megabytes
        assertEquals((1.5 * 1024 * 1024).toLong(), size.bytes)
    }

    @Test
    fun `should create ByteSize from gigabytes extension`() {
        val size = 2.gigabytes
        assertEquals(2L * 1024 * 1024 * 1024, size.bytes)
    }

    @Test
    fun `should create ByteSize from Double gigabytes extension`() {
        val size = 1.5.gigabytes
        assertEquals((1.5 * 1024 * 1024 * 1024).toLong(), size.bytes)
    }

    @Test
    fun `should create ByteSize from terabytes extension`() {
        val size = 2.terabytes
        assertEquals(2L * 1024 * 1024 * 1024 * 1024, size.bytes)
    }

    @Test
    fun `should create ByteSize from Double terabytes extension`() {
        val size = 1.5.terabytes
        assertEquals((1.5 * 1024 * 1024 * 1024 * 1024).toLong(), size.bytes)
    }

    // --- toByteSize Tests ---

    @Test
    fun `should convert Number to ByteSize with PETABYTE unit`() {
        val size = 2.toByteSize(ByteUnit.PETABYTE)
        assertEquals(2L * 1024 * 1024 * 1024 * 1024 * 1024, size.bytes)
    }

    @Test
    fun `should convert Number to ByteSize with EXABYTE unit`() {
        val size = 1.toByteSize(ByteUnit.EXABYTE)
        assertEquals(1L shl 60, size.bytes)
    }

    @Test
    fun `should convert Double to ByteSize with fractional unit`() {
        val size = 1.5.toByteSize(ByteUnit.KILOBYTE)
        assertEquals(1536L, size.bytes)
    }

    // --- Edge Cases ---

    @Test
    fun `should handle maximum safe Long value`() {
        val maxSafe = Long.MAX_VALUE
        val size = ByteSize(maxSafe)
        assertEquals(maxSafe, size.bytes)
    }

    @Test
    fun `should handle large multiplication without overflow`() {
        val size = ByteSize(1000)
        val result = size * 1000000
        assertEquals(1000000000L, result.bytes)
    }

    @Test
    fun `should handle division by 1`() {
        val size = ByteSize(1024)
        val result = size / 1
        assertEquals(1024L, result.bytes)
    }

    @Test
    fun `should handle division resulting in zero`() {
        val size = ByteSize(512)
        val result = size / 1024
        assertEquals(0L, result.bytes)
    }

    @Test
    fun `should handle very small Double multiplication`() {
        val size = ByteSize(1000)
        val result = size * 0.001
        assertEquals(1L, result.bytes)
    }

    @Test
    fun `should handle chain operations`() {
        val size = 1.kilobytes + 512.bytes - 256.bytes
        assertEquals(1280L, size.bytes)
    }

    @Test
    fun `should handle complex chain with multiplication and division`() {
        val size = (2.kilobytes * 3) / 2
        assertEquals(3072L, size.bytes)
    }
}
