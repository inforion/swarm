package ru.inforion.lab403.swarm

import ru.inforion.lab403.swarm.implementations.MPI
import ru.inforion.lab403.swarm.implementations.Threads

/**
 * Makes parallelization of [this] iterator object under the Swarm
 *   After that it's possible to use parallel methods
 */
fun <T> Iterator<T>.parallelize(swarm: Swarm) = swarm.parallelize(this)

/**
 * Makes parallelization of [this] iterable object under the Swarm
 *   After that it's possible to use parallel methods
 */
fun <T> Iterable<T>.parallelize(swarm: Swarm) = swarm.parallelize(iterator())

/**
 * Makes [this] array parallelization under the Swarm
 *   After that it's possible to use parallel methods
 */
fun <T> Array<T>.parallelize(swarm: Swarm) = swarm.parallelize(iterator())

/**
 * Makes parallelization of [this] sequence under the Swarm
 *   After that it's possible to use parallel methods
 */
fun <T> Sequence<T>.parallelize(swarm: Swarm) = swarm.parallelize(iterator())

/**
 * Creates Swarm on thread implementation
 *
 * @param size number of slaves
 * @param compress compress data when transfer it by net or not
 * @param action code to execute under Swarm control
 */
fun threadsSwarm(size: Int, compress: Boolean = false, action: (Swarm) -> Unit) {
    Swarm(Threads(size, compress), action)
}

/**
 * Creates Swarm on MPI implementation (openMPI)
 *
 * @param args MPI arguments
 * @param compress compress data when transfer it by net or not
 * @param action code to execute under Swarm control
 */
fun mpiSwarm(vararg args: String, compress: Boolean = false, action: (Swarm) -> Unit) {
    Swarm(MPI(*args, compress = compress), action)
}

/**
 * Separates given [this] collection on specified [count] of chunks
 *
 * @param count number of chunks
 */
fun <T> Collection<T>.separate(count: Int): Collection<Collection<T>> {
    require(count > 0) { "Size must be > 0 to separate" }
    val chunkSize = size / count + 1
    return windowed(chunkSize, chunkSize, true)
}