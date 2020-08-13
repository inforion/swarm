package ru.inforion.lab403.swarm

/**
 * {EN}
 * Class wrapper for collections to parallelize them
 *
 * @param swarm Swarm to use for parallelization
 * @param collection collection to wrap
 * {EN}
 */
class Parallel<T>(val swarm: Swarm, private val collection: Collection<T>) {
    /**
     * {EN}
     * Returns a list containing the results of applying the given [block]
     *   function to each element in the original collection using parallelization.
     *
     * @param block code to apply to each element of collection
     * {EN}
     */
    fun <R>map(block: (T) -> R) = swarm.map(collection, block)

    /**
     * {EN}
     * Returns a list containing the results of applying the given [block]
     *   function to each element in the original collection using
     *   parallelization with previously created context.
     *
     * @param block code to apply to each element of collection
     * {EN}
     */
    fun <C, R>mapContext(block: (C, T) -> R) = swarm.mapContext(collection, block)

    /**
     * {EN}
     * Returns a list containing only elements matching the given [predicate] using parallelization.
     *
     * @param predicate code to apply to each element of collection
     * {EN}
     */
    fun filter(predicate: (T) -> Boolean): Collection<T> = swarm.filter(collection, predicate)
}