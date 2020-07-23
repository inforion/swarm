package ru.inforion.lab403.swarm

import ru.inforion.lab403.swarm.implementations.MPI
import ru.inforion.lab403.swarm.implementations.Threads
import java.lang.IllegalArgumentException

fun <T> Collection<T>.parallelize(swarm: Swarm) = swarm.parallelize(this)

fun threadsSwarm(size: Int, code: (Swarm) -> Unit) {
    Swarm(Threads(size), code)
}

fun mpiSwarm(vararg args: String, code: (Swarm) -> Unit) {
    Swarm(MPI(*args), code)
}

fun swarm(name: String, vararg args: Any, code: (Swarm) -> Unit) = when(name) {
    "THREAD" -> threadsSwarm(args[0] as Int, code)
    "MPI" -> mpiSwarm(*args as Array<out String>, code = code)
    else -> throw IllegalArgumentException("Unknown realm '$name' for Swarm!")
}