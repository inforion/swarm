package ru.inforion.lab403.swarm.implementations

import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.abstracts.ARealm
import ru.inforion.lab403.swarm.common.Parcel
import ru.inforion.lab403.swarm.io.deserialize
import ru.inforion.lab403.swarm.io.serialize
import java.io.Serializable
import java.util.ArrayList
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class Threads(val size: Int) : ARealm() {

    companion object {
        @Transient val log = logger()
    }

    private fun <T> LinkedBlockingQueue<T>.take(predicate: (T) -> Boolean): T {
        while (true) {
            val item = take()
            if (predicate(item)) return item
            put(item)
        }
    }

    private class QueueEntry(val sender: Int, val data: ByteArray)

    private val incoming = Array(size + 1) { LinkedBlockingQueue<QueueEntry>() }

    private val barrier = CyclicBarrier(size + 1)

    private val threads = ArrayList<Thread>()

    override fun asyncRequestCount(): Int = 0

    override fun send(obj: Serializable, dst: Int, blocked: Boolean) {
        log.config { "[${rank}] obj=$obj dst=$dst block=$blocked" }
        val buffer = obj.serialize(directed = false, compress = true)
        incoming[dst].put(QueueEntry(rank, buffer.array()))
    }

    override fun recv(src: Int): Parcel {
        val queue = incoming[rank]
        val entry = if (src != -1) queue.take { it.sender == src } else queue.take()
        return Parcel(entry.sender, entry.data.deserialize(compress = true))
    }

    override val rank get() = threads.indexOf(Thread.currentThread())
    override val total get() = threads.size

    override fun barrier() {
        barrier.await()
    }

    override fun run(swarm: Swarm) {
        threads.add(Thread.currentThread())

        repeat(size) {
            val slave = thread {
                try {
                    swarm.slave()
                } catch (exc: InterruptedException) {
                    log.info { "Thread ${rank} interrupted" }
                } catch (error: Throwable) {
                    log.severe { "Node[${rank}] -> execution can't be continued:" }
                    error.printStackTrace()
                    exitProcess(-1)
                }
            }
            threads.add(slave)
        }

        swarm.master()
    }
}