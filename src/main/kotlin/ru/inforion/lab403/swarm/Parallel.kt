package ru.inforion.lab403.swarm

class Parallel<T>(val swarm: Swarm, val collection: Collection<T>) {
    fun <R>map(block: (T) -> R) = swarm.map(collection, block)

    fun <C, R>mapContext(block: (C, T) -> R) = swarm.mapContext(collection, block)

    fun filter(predicate: (T) -> T): Collection<T> = TODO()

    fun reduce(block: (T) -> T): T = TODO()
}