package com.ammarymn.kmp.sysutil.unit

import kotlin.jvm.JvmInline
import kotlin.math.pow
import kotlin.math.roundToInt

@JvmInline
value class ByteSize(val bytes: Long) : Comparable<ByteSize> {

    init {
        require(bytes >= 0) { "Byte size must be non-negative" }
    }

    override fun compareTo(other: ByteSize) = bytes.compareTo(other.bytes)

    // --- Math ---
    operator fun plus(other: ByteSize) = ByteSize(bytes + other.bytes)
    operator fun minus(other: ByteSize) = ByteSize(bytes - other.bytes)

    operator fun times(factor: Int) = ByteSize(bytes * factor)
    operator fun times(factor: Long) = ByteSize(bytes * factor)
    operator fun times(factor: Double) = ByteSize((bytes * factor).toLong())

    operator fun div(factor: Int) = ByteSize(bytes / factor)
    operator fun div(factor: Long) = ByteSize(bytes / factor)
    operator fun div(other: ByteSize): Double = bytes.toDouble() / other.bytes.toDouble()

    // --- Conversions ---
    fun toLong(unit: ByteUnit): Long = bytes / unit.weight
    fun toDouble(unit: ByteUnit): Double = bytes.toDouble() / unit.weight

    /** e.g. toString(ByteUnit.GIGABYTE) -> "1.52 GB" */
    fun toString(unit: ByteUnit, decimals: Int = 2): String {
        return "${toDouble(unit).format(decimals)} ${unit.shortName}"
    }

    /** Auto-selects unit: "1.40 GB" */
    override fun toString(): String {
        val unit = ByteUnit.entries.reversed().firstOrNull { bytes >= it.weight } ?: ByteUnit.BYTE
        return toString(unit)
    }

    companion object {
        inline val Number.bytes get() = ByteSize(this.toLong())
        inline val Number.kilobytes get() = toByteSize(ByteUnit.KILOBYTE)
        inline val Number.megabytes get() = toByteSize(ByteUnit.MEGABYTE)
        inline val Number.gigabytes get() = toByteSize(ByteUnit.GIGABYTE)
        inline val Number.terabytes get() = toByteSize(ByteUnit.TERABYTE)

        fun Number.toByteSize(unit: ByteUnit): ByteSize {
            return ByteSize((this.toDouble() * unit.weight).toLong())
        }
    }
}

// 2. The Scale (e.g., MB, GB)
enum class ByteUnit(val weight: Long, val shortName: String) {
    BYTE(1L, "B"),
    KILOBYTE(1L shl 10, "KB"),
    MEGABYTE(1L shl 20, "MB"),
    GIGABYTE(1L shl 30, "GB"),
    TERABYTE(1L shl 40, "TB"),
    PETABYTE(1L shl 50, "PB"),
    EXABYTE(1L shl 60, "EB");
}

// 3. Commutative Operators (Enable: 5 * 10.mb)
operator fun Int.times(size: ByteSize) = size * this
operator fun Long.times(size: ByteSize) = size * this
operator fun Double.times(size: ByteSize) = size * this

// Helper
private fun Double.format(decimals: Int): String {
    if (decimals <= 0) return this.toLong().toString()
    val factor = 10.0.pow(decimals)
    val rounded = (this * factor).roundToInt() / factor
    val str = rounded.toString()
    val parts = str.split('.')
    val integer = parts[0]
    val fraction = if (parts.size > 1) parts[1] else ""
    return if (fraction.length >= decimals) "$integer.${fraction.take(decimals)}"
    else "$integer.${fraction.padEnd(decimals, '0')}"
}