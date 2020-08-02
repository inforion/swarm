package ru.inforion.lab403.swarm

import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.abstracts.IRealm
import ru.inforion.lab403.swarm.common.Slave
import ru.inforion.lab403.swarm.common.Response
import ru.inforion.lab403.swarm.implementations.recvFromOthers
import ru.inforion.lab403.swarm.implementations.recvFromOthersUntil
import ru.inforion.lab403.swarm.implementations.sendToAllEvenly
import ru.inforion.lab403.swarm.implementations.sendToOthers
import ru.inforion.lab403.swarm.interfaces.ITask

class Swarm(private val realm: IRealm, val code: (Swarm) -> Unit) {
    companion object {
        @Transient val log = logger()
    }

    val size get() = realm.total

    private val receiveNotifiers = mutableSetOf<ReceiveNotifier>()

    fun <T> parallelize(collection: Collection<T>) = Parallel(this, collection)

    fun <T> context(context: (Int) -> T) = forEach { it.context = context(it.rank) }

    fun <C> eachContext(block: (context: C) -> Unit) = forEach { block(it.context as C) }

    fun each(block: (Int) -> Unit) = forEach { block(it.rank) }

    fun <C, R> get(block: (context: C) -> R): List<R> {
        forEach(false) {
            @Suppress("UNCHECKED_CAST")
            val result = block(it.context as C)
            it.response(result, it.rank)
        }
        return receiveOrdered(size - 1, -1)
    }

    fun <C, T, R>mapContext(collection: Collection<T>, block: (C, T) -> R): Collection<R> {
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

    fun <T, R>map(collection: Collection<T>, block: (T) -> R): Collection<R> {
        val tasks = collection.mapIndexed { index, value ->
            object : ITask {
                override fun execute(slave: Slave) {
                    val result = block(value)
                    slave.response(result, index)
                }
            }
        }
        realm.sendToAllEvenly(tasks, true)
        return receiveOrdered(tasks.size, 0)
    }

    fun addReceiveNotifier(notifier: ReceiveNotifier) = notifier.also { receiveNotifiers.add(it) }

    fun removeReceiveNotifier(notifier: ReceiveNotifier) = receiveNotifiers.remove(notifier)

    private fun <R>receiveOrdered(size: Int, offset: Int): List<R> {
        val result = ArrayList<R?>(size)

        repeat(size) { result.add(null) }

        var count = 0

        realm.recvFromOthersUntil { mail ->
            @Suppress("UNCHECKED_CAST")
            val response = mail.obj as Response<R>
            result[response.index + offset] = response.obj
            receiveNotifiers.forEach { it.invoke(mail.sender) }
            ++count != size
        }

        @Suppress("UNCHECKED_CAST")
        return result as ArrayList<R>
    }

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