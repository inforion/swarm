package ru.inforion.lab403.swarm.wrappers

import ru.inforion.lab403.common.extensions.launch
import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.implementations.receiveFiltered
import ru.inforion.lab403.swarm.implementations.receiveOrderedAll
import ru.inforion.lab403.swarm.implementations.receiveOrderedWhile
import ru.inforion.lab403.swarm.implementations.sendToAllEvenly
import ru.inforion.lab403.swarm.tasks.IndexedCommonTask
import ru.inforion.lab403.swarm.tasks.IndexedContextTask

/**
 * Class wrapper for sequences to parallelize them
 *
 * @param swarm Swarm to use for parallelization
 * @param iterator is a iterator to wrap
 */
class ParallelIterator<T>(val swarm: Swarm, private val iterator: Iterator<T>) {

    /**
     * Returns a list containing the results of applying the given [transform]
     *   function to each element in the original iterable object using parallelization.
     *
     * @param transform code to apply to each element of iterable
     */
    fun <R> map(transform: (T) -> R) = with(swarm) {
        val target = iterator
        val count = realm.sendToAllEvenly(target, true) { index, value ->
            IndexedCommonTask(index, value, transform)
        }
        realm.receiveOrderedAll<R>(count, 0) { swarm.notify(it) }
    }

    /**
     * Returns a list containing the results of applying the given [transform]
     *   function to each element in the original iterable object using parallelization.
     *
     * This function differs from [map] by using asynchronous send/receive loop and should be faster
     *
     * @since 0.2.1
     *
     * @param transform code to apply to each element of iterable
     */
    fun <R> map2(transform: (T) -> R) = with(swarm) {
        var total = -1

        launch {
            total = realm.sendToAllEvenly(iterator, true) { index, value ->
                IndexedCommonTask(index, value, transform)
            }
        }

        realm.receiveOrderedWhile<R> { received, index ->
            swarm.notify(index)
            // FIXME: Infinite block may occurred if all mails were received before total set to actual count
            total == -1 || received != total
        }
    }

    /**
     * Returns a list containing the results of applying the given [transform]
     *   function to each element in the original iterable object using
     *   parallelization with previously created context.
     *
     * @param transform code to apply to each element of iterable object
     */
    fun <C, R> mapContext(transform: (C, T) -> R) = with(swarm) {
        val count = realm.sendToAllEvenly(iterator, true) { index, value ->
            IndexedContextTask(index, value, transform)
        }
        realm.receiveOrderedAll<R>(count, 0) { swarm.notify(it) }
    }

    /**
     * Returns a list containing the results of applying the given [transform]
     *   function to each element in the original iterable object using
     *   parallelization with previously created context.
     *
     * This function differs from [mapContext] by using asynchronous send/receive loop and should be faster
     *
     * @since 0.2.1
     *
     * @param transform code to apply to each element of iterable object
     */
    fun <C, R> map2Context(transform: (C, T) -> R) = with(swarm) {
        var total = -1

        launch {
            total = realm.sendToAllEvenly(iterator, true) { index, value ->
                IndexedContextTask(index, value, transform)
            }
        }

        realm.receiveOrderedWhile<R> { received, response ->
            swarm.notify(response)
            // FIXME: Infinite block may occurred if all mails were received before total set to actual count
            total == -1 || received != total
        }
    }

    /**
     * Returns a list containing only elements matching the given [predicate] using parallelization.
     *
     * @param predicate code to apply to each element of iterable object
     */
    fun filter(predicate: (T) -> Boolean) = with(swarm) {
        val tasks = mutableListOf<IndexedCommonTask<T, *>>()
        val count = realm.sendToAllEvenly(iterator, true) { index, value ->
            IndexedCommonTask(index, value, predicate).also { tasks.add(it) }
        }
        realm.receiveFiltered(count, tasks) { swarm.notify(it) }
    }
}