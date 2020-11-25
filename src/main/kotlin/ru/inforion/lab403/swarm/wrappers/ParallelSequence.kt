package ru.inforion.lab403.swarm.wrappers

import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.tasks.IndexedCommonTask
import ru.inforion.lab403.swarm.tasks.IndexedContextTask

/**
 * Class wrapper for sequences to parallelize them
 *
 * @param swarm Swarm to use for parallelization
 * @param sequence sequence to wrap
 */
class ParallelSequence<T>(val swarm: Swarm, private val sequence: Sequence<T>) {

    /**
     * Returns a list containing the results of applying the given [transform]
     *   function to each element in the original iterable object using parallelization.
     *
     * @param transform code to apply to each element of iterable
     */
    fun <R> map(transform: (T) -> R) = with(swarm) {
        val tasks = sequence.mapIndexed { index, value -> IndexedCommonTask(index, value, transform) }
        val count = sendToAllEvenly(tasks, true)
        receiveOrdered<R>(count, 0)
    }

    /**
     * Returns a list containing the results of applying the given [transform]
     *   function to each element in the original iterable object using
     *   parallelization with previously created context.
     *
     * @param transform code to apply to each element of iterable object
     */
    fun <C, R> mapContext(transform: (C, T) -> R) = with(swarm) {
        val tasks = sequence.mapIndexed { index, value -> IndexedContextTask(index, value, transform) }
        val count = sendToAllEvenly(tasks, true)
        receiveOrdered<R>(count, 0)
    }
}