package ru.inforion.lab403.swarm.io

import java.io.OutputStream
import java.nio.ByteBuffer

internal class BufferOutputStream(size: Int, directed: Boolean) : OutputStream() {
    init {
        require(size > 0) { "Negative initial size: $size" }
    }

    val buffer: ByteBuffer = if (directed) ByteBuffer.allocateDirect(size) else ByteBuffer.allocate(size)

    override fun write(b: Int) {
        buffer.put(b.toByte())
    }
}