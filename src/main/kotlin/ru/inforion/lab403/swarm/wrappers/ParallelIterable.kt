package ru.inforion.lab403.swarm.wrappers

import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.tasks.IndexedCommonTask
import ru.inforion.lab403.swarm.tasks.IndexedContextTask

/**
 * {EN}
 * Class wrapper for iterables to parallelize them
 *
 * @param swarm Swarm to use for parallelization
 * @param iterable iterable to wrap
 * {EN}
 */
class ParallelIterable<T>(val swarm: Swarm, private val iterable: Iterable<T>) {
    /**
     * {EN}
     * Returns a list containing the results of applying the given [transform]
     *   function to each element in the original iterable object using parallelization.
     *
     * @param transform code to apply to each element of iterable
     * {EN}
     */
    fun <R> map(transform: (T) -> R) = with(swarm) {
        val tasks = iterable.mapIndexed { index, value -> IndexedCommonTask(index, value, transform) }
        val count = sendToAllEvenly(tasks, true)
        receiveOrdered<R>(count, 0)
    }

    /**
     * {EN}
     * Returns a list containing the results of applying the given [transform]
     *   function to each element in the original iterable object using
     *   parallelization with previously created context.
     *
     * @param transform code to apply to each element of iterable object
     * {EN}
     */
    fun <C, R> mapContext(transform: (C, T) -> R) = with(swarm) {
        val tasks = iterable.mapIndexed { index, value -> IndexedContextTask(index, value, transform) }
        val count = sendToAllEvenly(tasks, true)
        receiveOrdered<R>(count, 0)
    }

    /**
     * {EN}
     * Returns a list containing only elements matching the given [predicate] using parallelization.
     *
     * @param predicate code to apply to each element of iterable object
     * {EN}
     */
    fun filter(predicate: (T) -> Boolean) = with(swarm) {
        val tasks = iterable.mapIndexed { index, value -> IndexedCommonTask(index, value, predicate) }
        sendToAllEvenly(tasks, true)
        receiveFiltered(tasks)
    }
}