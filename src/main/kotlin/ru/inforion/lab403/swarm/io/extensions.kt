package ru.inforion.lab403.swarm.io

import java.io.*
import java.nio.ByteBuffer

fun <T: Serializable> T.calcObjectSize(): Int {
    val output = DataOutputStream(DummyOutputStream())
    serialize(output)
    return output.size()
}

fun <T: Serializable> T.serialize(output: OutputStream) {
    ObjectOutputStream(output).apply { writeUnshared(this) }
}

fun <T: Serializable> T.serialize(directed: Boolean): ByteBuffer {
    val size = calcObjectSize()
    val output = MemoryOutputStream(size, directed)
    ObjectOutputStream(output).apply { writeUnshared(this) }
    return output.buffer
}

@Suppress("UNCHECKED_CAST")
fun <T: Serializable, S: InputStream> S.deserialize(): T = ObjectInputStream(this).readUnshared() as T

fun <T: Serializable> ByteArray.deserialize(): T = inputStream().deserialize()

fun <T: Serializable> ByteBuffer.deserialize(): T = array().deserialize()

fun <T: Serializable> T.serialCopy(): T = serialize(directed = false).deserialize()
