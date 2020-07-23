package ru.inforion.lab403.swarm

fun <T> Collection<T>.parallelize(swarm: Swarm) = swarm.parallelize(this)

fun threadsSwarm(size: Int, code: (Swarm) -> Unit) = Swarm(Threads(size), code)