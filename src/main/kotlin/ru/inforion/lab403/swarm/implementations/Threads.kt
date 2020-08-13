package ru.inforion.lab403.swarm.implementations

import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.abstracts.IRealm
import ru.inforion.lab403.swarm.common.Mail
import ru.inforion.lab403.swarm.io.deserialize
import ru.inforion.lab403.swarm.io.serialize
import java.io.Serializable
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class Threads(val size: Int, val compress: Boolean) : IRealm {

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

    private val incoming = Array(size + 1) { LinkedBlockingQueue<Pair<Int, ByteArray>>() }

    private val barrier = CyclicBarrier(size + 1)

    private val threads = ArrayList<Thread>()

    override fun pack(obj: Serializable) = obj.serialize(false, compress)

    override fun send(buffer: ByteBuffer, dst: Int, blocked: Boolean) {
//        log.config { "[${rank}] obj=$buffer dst=$dst block=$blocked size = ${buffer.limit()}" }
        incoming[dst].put(rank to buffer.array())
    }

    override fun recv(src: Int): Mail {
        val queue = incoming[rank]
        val (sender, data) = if (src != -1) queue.take { it.first == src } else queue.take()
        return Mail(sender, data.deserialize(compress))
    }

    override val rank: Int
        get() {
            val thread = Thread.currentThread()
            val index = threads.indexOf(thread)
            // If thread created inside Swarm master code then id of current thread changed
            // so if we not found thread suppose that it is master. Also suppose that
            // rank can't be called inside thread inside slave node otherwise it won't work
            return if (index != -1) index else 0
        }
    override val total get() = threads.size

    override fun barrier() {
        barrier.await()
    }

    override fun run(swarm: Swarm) {
        threads.add(Thread.currentThread())

        repeat(size) {
            val slave = thread(name = "SwarmSlave-${it + 1}") {
                try {
                    swarm.slave()
                } catch (exc: InterruptedException) {
                    log.info { "Thread $rank interrupted" }
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