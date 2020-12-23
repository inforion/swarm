@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")

package ru.inforion.lab403.swarm

import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.abstracts.IRealm
import ru.inforion.lab403.swarm.common.Slave
import ru.inforion.lab403.swarm.implementations.*
import ru.inforion.lab403.swarm.interfaces.ITask
import ru.inforion.lab403.swarm.wrappers.ParallelIterator

/**
 * Main class of Swarm library
 *
 * Class contains method to access to other nodes and methods to parallelize iterable objects
 *
 * @param realm parallelization driver to use, see [MPI] or [Threads]
 * @param code Swarm master node code, i.e. code run under Swarm library
 */
class Swarm(internal val realm: IRealm, val code: (Swarm) -> Unit) {
    companion object {
        @Transient val log = logger()
    }

    /**
     * Size of Swarm include master
     */
    val size get() = realm.total

    /**
     * Wrap the specified iterable object into Swarm [ParallelIterator] class.
     * After iterable object wrapped parallelized method can be called.
     *
     * @param iterator iterator to wrap
     *
     * @param T type of element
     */
    fun <T> parallelize(iterator: Iterator<T>) = ParallelIterator(this, iterator)

    /**
     * Create a context with type [T] on each Swarm worker.
     * This method may be useful to create stateful parallelization tasks.
     *
     * @param context factory to create context
     */
    fun <T> context(context: (Int) -> T) = forEach { it.context = context(it.rank) }

    /**
     * Executes given block of code on each slave node with previously created context
     *
     * @param action code to execute on each slave node
     */
    fun <C> eachContext(action: (context: C) -> Unit) = forEach { action(it.context as C) }

    /**
     * Executes given block of code on each slave node
     *
     * @param action code to execute on each slave node
     */
    fun each(action: (Int) -> Unit) = forEach { action(it.rank) }

    /**
     * Executes given block of code on each slave node with previously created context and get something
     *   from execution. Using this method context related or other data may be collected from slave nodes.
     *
     * @param action code to execute on each slave node
     */
    fun <C, R> get(action: (context: C) -> R): List<R> {
        forEach(false) {
            @Suppress("UNCHECKED_CAST")
            val result = action(it.context as C)
            it.response(result, it.rank)
        }
        return realm.receiveOrdered(size - 1, -1) { }
    }

    /**
     * Notifiers to execute when something receive from slave node
     */
    private val receiveNotifiers = mutableSetOf<ReceiveNotifier>()

    /**
     * Add receive notifier to be executed when something received from slave node
     *
     * @param notifier notifier callback (lambda)
     *
     * @return added callback to be able to remove it later using [removeReceiveNotifier]
     */
    fun addReceiveNotifier(notifier: ReceiveNotifier) = notifier.also { receiveNotifiers.add(it) }

    /**
     * Remove receive notifier
     *
     * @param notifier notifier callback to remove
     */
    fun removeReceiveNotifier(notifier: ReceiveNotifier) = receiveNotifiers.remove(notifier)

    internal fun notify(index: Int) = receiveNotifiers.forEach { it.invoke(index) }

    private inline fun forEach(sync: Boolean = true, crossinline block: (Slave) -> Unit): Swarm {
        val task = ITask { slave ->
            block(slave)
            if (sync) slave.barrier()
        }
        log.finest { "Send task '${task}' to all others" }
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