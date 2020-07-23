package ru.inforion.lab403.swarm

import ru.inforion.lab403.swarm.abstracts.ARealm
import ru.inforion.lab403.swarm.common.Slave
import ru.inforion.lab403.swarm.common.Response
import ru.inforion.lab403.swarm.interfaces.ITask

class Swarm(private val realm: ARealm, val code: (Swarm) -> Unit) {
    val size get() = realm.total()

    fun <T> parallelize(collection: Collection<T>) = Parallel(this, collection)

    fun <T> context(context: () -> T) = forEach { it.context = context() }

    fun <C> eachContext(block: (context: C) -> Unit) = forEach { block(it.context as C) }

    fun each(block: () -> Unit) = forEach { block() }

    fun <C, R> get(block: (context: C) -> R): List<R> {
        forEach(false) {
            val result = block(it.context as C)
            it.response(result, it.rank)
        }
        return receivedOrdered(size)
    }

    fun <C, T, R>mapContext(collection: Collection<T>, block: (C, T) -> R): Collection<R> {
        val tasks = collection.mapIndexed { index, value ->
            object : ITask {
                override fun execute(slave: Slave) {
                    val result = block(slave.context as C, value)
                    slave.response(result, index)
                }
            }
        }
        realm.sendToAllEvenly(tasks, true)
        return receivedOrdered(collection.size)
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
        return receivedOrdered(collection.size)
    }

    private fun <R>receivedOrdered(size: Int): List<R> {
        val result = ArrayList<R?>(size)

        repeat(size) { result.add(null) }

        var count = 0

        realm.recvFromAnyUntil { parcel ->
            @Suppress("UNCHECKED_CAST")
            val task = parcel.obj as Response<R>
            result[task.index] = task.obj
            ++count != size
        }

        @Suppress("UNCHECKED_CAST")
        return result as ArrayList<R>
    }

    private inline fun forEach(sync: Boolean = true, crossinline block: (Slave) -> Unit) {
        val task = object : ITask {
            override fun execute(slave: Slave) {
                block(slave)
                if (sync) slave.barrier()
            }
        }
        realm.sendToAllExceptMe(task)
        if (sync) realm.barrier()
    }

    fun master() {
        code(this)
        println("Stopping master...")
        forEach { it.working = false }
    }

    fun slave() = Slave(realm).run()
}