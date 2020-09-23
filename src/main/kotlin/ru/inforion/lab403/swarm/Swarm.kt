@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")

package ru.inforion.lab403.swarm

import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.abstracts.IRealm
import ru.inforion.lab403.swarm.common.Response
import ru.inforion.lab403.swarm.common.Slave
import ru.inforion.lab403.swarm.implementations.*
import ru.inforion.lab403.swarm.interfaces.ITask
import ru.inforion.lab403.swarm.tasks.IndexedCommonTask
import ru.inforion.lab403.swarm.wrappers.ParallelIterable
import ru.inforion.lab403.swarm.wrappers.ParallelSequence
import java.io.Serializable

/**
 * {EN}
 * Main class of Swarm library
 *
 * Class contains method to access to other nodes and methods to parallelize iterable objects
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
     * Wrap the specified iterable object into Swarm [ParallelIterable] class.
     * After iterable object wrapped parallelized method can be called.
     *
     * @param iterable iterable to wrap
     *
     * @param T type of element
     * {EN}
     */
    fun <T> parallelize(iterable: Iterable<T>) = ParallelIterable(this, iterable)

    /**
     * {EN}
     * Wrap the specified iterable object into Swarm [ParallelIterable] class.
     * After iterable object wrapped parallelized method can be called.
     *
     * @param sequence sequence to wrap
     *
     * @param T type of element
     * {EN}
     */
    fun <T> parallelize(sequence: Sequence<T>) = ParallelSequence(this, sequence)

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
     * @param action code to execute on each slave node
     * {EN}
     */
    fun <C> eachContext(action: (context: C) -> Unit) = forEach { action(it.context as C) }

    /**
     * {EN}
     * Executes given block of code on each slave node
     *
     * @param action code to execute on each slave node
     * {EN}
     */
    fun each(action: (Int) -> Unit) = forEach { action(it.rank) }

    /**
     * {EN}
     * Executes given block of code on each slave node with previously created context and get something
     *   from execution. Using this method context related or other data may be collected from slave nodes.
     *
     * @param action code to execute on each slave node
     * {EN}
     */
    fun <C, R> get(action: (context: C) -> R): List<R> {
        forEach(false) {
            @Suppress("UNCHECKED_CAST")
            val result = action(it.context as C)
            it.response(result, it.rank)
        }
        return receiveOrdered(size - 1, -1)
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

    private inline fun <T, C: Collection<T>, R> fold(
        size: Int,
        initial: C,
        block: (acc: C, response: Response<R>) -> Unit
    ): C {
        realm.recvCountFromOthers(size) { mail ->
            val response = mail.objectAs<Response<R>>()
            block(initial, response)
            log.finest { "Received mail -> $mail" }
            receiveNotifiers.forEach { it.invoke(mail.sender) }
        }

        return initial
    }

    internal fun sendToAllEvenly(sequence: Sequence<Serializable>, blocked: Boolean = true) =
        realm.sendToAllEvenly(sequence, blocked) { it }

    internal fun sendToAllEvenly(iterable: Iterable<Serializable>, blocked: Boolean = true) =
        realm.sendToAllEvenly(iterable, blocked) { it }

    internal inline fun <T> receiveFiltered(tasks: List<IndexedCommonTask<T, *>>): List<T> {
        val result = fold(size, listOfNulls<T?>(size)) { acc, response: Response<Boolean> ->
            if (response.data) acc[response.index] = tasks[response.index].value
        }
        val notNulls = ArrayList<T>(size / 2)
        result.forEach { if (it != null) notNulls.add(it) }
        return notNulls
    }

    internal inline fun <T> receiveOrdered(size: Int, offset: Int) =
        fold(size, listOfNulls<T?>(size)) { acc, response: Response<T> ->
            acc[response.index + offset] = response.data
        } as List<T>

    // arrayOfNulls require reified T parameter and thus all upper parameter must be reified
    // that will lead to expose all private non-inline function
    private inline fun <T> listOfNulls(size: Int) = Array<Any?>(size) { null }.asList() as MutableList<T?>

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