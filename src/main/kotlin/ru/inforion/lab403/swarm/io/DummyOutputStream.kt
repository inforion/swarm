package ru.inforion.lab403.swarm.io

import ru.inforion.lab403.common.extensions.hex2
import java.io.OutputStream

internal class DummyOutputStream : OutputStream() {
    var written = 0
        private set

    override fun write(b: Int) {
        written += 1
        println(b.hex2)
    }
}