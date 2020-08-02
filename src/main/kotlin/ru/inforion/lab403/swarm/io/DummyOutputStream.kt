package ru.inforion.lab403.swarm.io

import java.io.OutputStream

internal class DummyOutputStream : OutputStream() {
    var written = 0
        private set

    override fun write(b: Int) {
        written += 1
    }
}