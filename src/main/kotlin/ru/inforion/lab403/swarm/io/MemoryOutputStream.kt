package ru.inforion.lab403.swarm.io

import java.io.OutputStream
import java.nio.ByteBuffer

internal class MemoryOutputStream(size: Int, directed: Boolean) : OutputStream() {
    val buffer: ByteBuffer =
        if (size > 0) {
            if (directed) ByteBuffer.allocateDirect(size) else ByteBuffer.allocate(size)
        } else
            throw IllegalArgumentException("Negative initial size: $size")

    override fun write(b: Int) {
        buffer.put(b.toByte())
    }
}