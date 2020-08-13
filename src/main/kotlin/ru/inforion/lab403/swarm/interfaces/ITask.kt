package ru.inforion.lab403.swarm.interfaces

import ru.inforion.lab403.swarm.common.Slave
import java.io.Serializable

internal interface ITask : Serializable {
    fun execute(slave: Slave)
}