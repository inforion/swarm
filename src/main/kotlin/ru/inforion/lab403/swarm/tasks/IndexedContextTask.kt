package ru.inforion.lab403.swarm.tasks

import ru.inforion.lab403.swarm.common.Slave
import ru.inforion.lab403.swarm.interfaces.ITask

internal class IndexedContextTask<C, T, R>(val index: Int, val value: T, val transform: (C, T) -> R) : ITask {
    override fun execute(slave: Slave) {
        @Suppress("UNCHECKED_CAST")
        val result = transform(slave.context as C, value)
        slave.response(result, index)
    }
}