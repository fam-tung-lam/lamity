package com.phamtunglam.lamity.core.domain.platform

import kotlin.random.Random

private const val HEX = "0123456789abcdef"
private const val UUID_BYTE_COUNT = 16
private const val UUID_STRING_LENGTH = 36

// RFC 4122: force the version (4) and variant (10xx) bits.
private const val VERSION_BYTE_INDEX = 6
private const val VARIANT_BYTE_INDEX = 8
private const val LOW_NIBBLE_MASK = 0x0f
private const val VERSION_4_FLAG = 0x40
private const val VARIANT_CLEAR_MASK = 0x3f
private const val VARIANT_SET_FLAG = 0x80
private const val BYTE_MASK = 0xff
private const val NIBBLE_BITS = 4

// Byte offsets (in the 16-byte layout) after which the UUID hyphens are inserted.
private val HYPHEN_INDICES = setOf(4, 6, 8, 10)

/** Random UUIDv4-style identifier without relying on experimental stdlib APIs. */
fun newId(): String {
    val bytes = Random.nextBytes(UUID_BYTE_COUNT)
    bytes[VERSION_BYTE_INDEX] =
        ((bytes[VERSION_BYTE_INDEX].toInt() and LOW_NIBBLE_MASK) or VERSION_4_FLAG).toByte()
    bytes[VARIANT_BYTE_INDEX] =
        ((bytes[VARIANT_BYTE_INDEX].toInt() and VARIANT_CLEAR_MASK) or VARIANT_SET_FLAG).toByte()
    val sb = StringBuilder(UUID_STRING_LENGTH)
    for (i in 0 until UUID_BYTE_COUNT) {
        if (i in HYPHEN_INDICES) sb.append('-')
        val b = bytes[i].toInt() and BYTE_MASK
        sb.append(HEX[b shr NIBBLE_BITS]).append(HEX[b and LOW_NIBBLE_MASK])
    }
    return sb.toString()
}
