@file:Suppress("NOTHING_TO_INLINE")

package ru.inforion.lab403.swarm.io

import ru.inforion.lab403.common.extensions.asByte
import java.io.OutputStream

class DummyOutputStream(private val template: DummyOutputStream? = null) : OutputStream() {

    companion object {
        const val verifiable: Boolean = false
    }

    var written = 0
        private set

    // for verification purpose
    private val list = mutableListOf<Int>()
    private val data = mutableListOf<ByteArray>()

    private inline fun verify(bytes: ByteArray, offset: Int, count: Int) {
        if (!verifiable) return

        list.add(count)
        data.add(bytes.sliceArray(offset until offset + count))

        if (template != null) {
            val index = list.size - 1
            check(template.list[index] == list.last()) {
                val expected = template.list.take(index + 1).sum()
                "Object state changed during serialization (current size = $written, expected = $expected)"
            }
        }
    }

    override fun write(b: Int) {
        written += 1
        verify(byteArrayOf(b.asByte), 0, 1)
    }

    override fun write(b: ByteArray) {
        written += b.size
        verify(b, 0, b.size)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        written += len
        verify(b, off, len)
    }
}