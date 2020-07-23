package ru.inforion.lab403.swarm

class Parallel<T>(val swarm: Swarm, private val iterable: Iterable<T>) {
    fun <R>map(block: (T) -> R) = swarm.map(iterable, block)

    fun <C, R>mapContext(block: (C, T) -> R) = swarm.mapContext(iterable, block)

    fun filter(predicate: (T) -> T): Collection<T> = TODO()

    fun reduce(block: (T) -> T): T = TODO()
}