package ru.inforion.lab403.swarm

import ru.inforion.lab403.common.extensions.convertToBytes
import ru.inforion.lab403.common.extensions.hexlify
import java.security.MessageDigest

internal fun String.sha256() = MessageDigest.getInstance("SHA-256").digest(convertToBytes()).hexlify()

internal fun sha256Test(value: Int, count: Int): String {
    var result = value.toString()
    repeat(count) { result = result.sha256() }
    return result
}