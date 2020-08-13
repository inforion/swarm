@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")

package ru.inforion.lab403.swarm

import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.abstracts.IRealm
import ru.inforion.lab403.swarm.common.Slave
import ru.inforion.lab403.swarm.common.Response
import ru.inforion.lab403.swarm.implementations.*
import ru.inforion.lab403.swarm.interfaces.ITask

/**
 * {EN}
 * Main class of Swarm library
 *
 * Class contains method to access to other nodes and methods to parallelize collections
 *
 * @param realm parallelization driver to use, see [MPI] or [Threads]
 * @param code Swarm master node code, i.e. code run under Swarm library
 * {EN}
 */
class Swarm(private val realm: IRealm, val code: (Swarm) -> Unit) {
    companion object {
        @Transient val log = logger()
    }

    /**
     * {EN} Size of Swarm include master {EN}
     */
    val size get() = realm.total

    /**
     * {EN}
     * Wrap the specified collection into Swarm [Parallel] class.
     * After collection wrapped parallelized collection method can be called.
     *
     * @param collection collection to wrap
     *
     * @param T type of collection element
     * {EN}
     */
    fun <T> parallelize(collection: Collection<T>) = Parallel(this, collection)

    /**
     * {EN}
     * Create a context with type [T] on each Swarm worker.
     * This method may be useful to create stateful parallelization tasks.
     *
     * @param context factory to create context
     * {EN}
     */
    fun <T> context(context: (Int) -> T) = forEach { it.context = context(it.rank) }

    /**
     * {EN}
     * Executes given block of code on each slave node with previously created context
     *
     * @param block code to execute on each slave node
     * {EN}
     */
    fun <C> eachContext(block: (context: C) -> Unit) = forEach { block(it.context as C) }

    /**
     * {EN}
     * Executes given block of code on each slave node
     *
     * @param block code to execute on each slave node
     * {EN}
     */
    fun each(block: (Int) -> Unit) = forEach { block(it.rank) }

    /**
     * {EN}
     * Executes given block of code on each slave node with previously created context and get something
     *   from execution. Using this method context related or other data may be collected from slave nodes.
     *
     * @param block code to execute on each slave node
     * {EN}
     */
    fun <C, R> get(block: (context: C) -> R): List<R> {
        forEach(false) {
            @Suppress("UNCHECKED_CAST")
            val result = block(it.context as C)
            it.response(result, it.rank)
        }
        return receiveOrdered(size - 1, -1)
    }

    /**
     * {EN}
     * Returns a list containing the results of applying the given [block]
     *   function to each element in the original collection using
     *   parallelization with previously created context.
     *
     * @param collection collection to transform
     * @param block code to apply to each element of collection
     * {EN}
     */
    fun <C, T, R> mapContext(collection: Collection<T>, block: (C, T) -> R): Collection<R> {
        val tasks = collection.mapIndexed { index, value ->
            object : ITask {
                override fun execute(slave: Slave) {
                    @Suppress("UNCHECKED_CAST")
                    val result = block(slave.context as C, value)
                    slave.response(result, index)
                }
            }
        }
        realm.sendToAllEvenly(tasks, true)
        return receiveOrdered(tasks.size, 0)
    }

    /**
     * {EN}
     * Returns a list containing the results of applying the given [block]
     *   function to each element in the original collection using parallelization.
     *
     * @param collection collection to transform
     * @param block code to apply to each element of collection
     * {EN}
     */
    fun <T, R> map(collection: Collection<T>, block: (T) -> R): Collection<R> {
        val tasks = collection.mapIndexed { index, value -> IndexedCommonTask(index, value, block) }
        realm.sendToAllEvenly(tasks, true)
        return receiveOrdered(tasks.size, 0)
    }

    /**
     * {EN}
     * Returns a list containing only elements matching the given [predicate] using parallelization.
     *
     * @param collection collection to filter
     * @param predicate code to apply to each element of collection
     * {EN}
     */
    fun <T> filter(collection: Collection<T>, predicate: (T) -> Boolean): Collection<T> {
        val tasks = collection.mapIndexed { index, value -> IndexedCommonTask(index, value, predicate) }
        realm.sendToAllEvenly(tasks, true)
        return receiveFiltered(tasks)
    }

    /**
     * {EN} Notifiers to execute when something receive from slave node {EN}
     */
    private val receiveNotifiers = mutableSetOf<ReceiveNotifier>()

    /**
     * {EN}
     * Add receive notifier to be executed when something received from slave node
     *
     * @param notifier notifier callback (lambda)
     *
     * @return added callback to be able to remove it later using [removeReceiveNotifier]
     * {EN}
     */
    fun addReceiveNotifier(notifier: ReceiveNotifier) = notifier.also { receiveNotifiers.add(it) }

    /**
     * {EN}
     * Remove receive notifier
     *
     * @param notifier notifier callback to remove
     * {EN}
     */
    fun removeReceiveNotifier(notifier: ReceiveNotifier) = receiveNotifiers.remove(notifier)

    private class IndexedCommonTask<T, R>(val index: Int, val value: T, val block: (T) -> R) : ITask {
        override fun execute(slave: Slave) {
            val result = block(value)
            slave.response(result, index)
        }
    }

    private inline fun <T, C: Collection<T>, R> fold(
        size: Int,
        initial: C,
        block: (acc: C, response: Response<R>) -> Unit
    ): C {
        realm.recvCountFromOthers(size) { mail ->
            val response = mail.objectAs<Response<R>>()
            block(initial, response)
            receiveNotifiers.forEach { it.invoke(mail.sender) }
        }

        return initial
    }

    private inline fun <T> receiveFiltered(tasks: List<IndexedCommonTask<T, *>>): List<T> {
        val result = fold(size, listOfNulls<T?>(size)) { acc, response: Response<Boolean> ->
            if (response.data) acc[response.index] = tasks[response.index].value
        }
        val notNulls = ArrayList<T>(size / 2)
        result.forEach { if (it != null) notNulls.add(it) }
        return notNulls
    }

    private inline fun <T> receiveOrdered(size: Int, offset: Int) =
        fold(size, listOfNulls<T?>(size)) { acc, response: Response<T> ->
            acc[response.index + offset] = response.data
        } as List<T>

    // arrayOfNulls require reified T parameter and thus all upper parameter must be reified
    // that will lead to expose all private non-inline function
    private inline fun <T> listOfNulls(size: Int) = Array<Any?>(size) { null }.asList() as MutableList<T?>

    private inline fun forEach(sync: Boolean = true, crossinline block: (Slave) -> Unit): Swarm {
        val task = object : ITask {
            override fun execute(slave: Slave) {
                block(slave)
                if (sync) slave.barrier()
            }
        }
        realm.sendToOthers(task)
        if (sync) realm.barrier()
        return this
    }

    internal fun master() {
        code(this)
        log.finest { "Stopping master..." }
        forEach { it.working = false }
    }

    internal fun slave() = Slave(realm).run()

    init {
        realm.run(this)
    }
}