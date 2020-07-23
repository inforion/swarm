package ru.inforion.lab403.swarm.io

import java.io.OutputStream

internal class DummyOutputStream() : OutputStream() {
    override fun write(b: Int) {

    }
}