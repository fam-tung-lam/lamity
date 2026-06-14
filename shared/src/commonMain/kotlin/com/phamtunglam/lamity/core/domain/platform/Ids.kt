package com.phamtunglam.lamity.core.domain.platform

import kotlin.random.Random

private val HEX = "0123456789abcdef"

/** Random UUIDv4-style identifier without relying on experimental stdlib APIs. */
fun newId(): String {
    val bytes = Random.nextBytes(16)
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
    val sb = StringBuilder(36)
    for (i in 0 until 16) {
        if (i == 4 || i == 6 || i == 8 || i == 10) sb.append('-')
        val b = bytes[i].toInt() and 0xff
        sb.append(HEX[b shr 4]).append(HEX[b and 0x0f])
    }
    return sb.toString()
}
