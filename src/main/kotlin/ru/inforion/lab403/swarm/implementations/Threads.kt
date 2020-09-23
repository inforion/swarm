package ru.inforion.lab403.swarm.implementations

import ru.inforion.lab403.common.extensions.BlockingValue
import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.abstracts.IRealm
import ru.inforion.lab403.swarm.common.Mail
import ru.inforion.lab403.swarm.io.deserialize
import ru.inforion.lab403.swarm.io.serialize
import java.io.Serializable
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * Threads realm for [Swarm]
 *
 * @property size number of slaves
 * @property compress compress data when transfer it by net or not
 */
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

    private val exception = BlockingValue<Throwable>()

    private fun worker(name: String, action: () -> Unit) = thread(name = name) {
        runCatching {
            action()
        }.onFailure { error ->
            when (error) {
                is InterruptedException -> log.info { "Thread $rank interrupted" }
                is BrokenBarrierException -> log.warning { "Thread $rank barrier damaged due to other exception!" }
                else -> {
                    log.severe { "Node[${rank}] -> execution can't be continued: $error" }
                    exception.offer(error)
                }
            }
        }
    }.also { threads.add(it) }

    private fun checkAndThrowIfExceptionOccurred() {
        val error = exception.poll(100)
        if (error != null) {
            threads.forEach { it.interrupt() }
            throw error
        }
    }

    override fun run(swarm: Swarm) {
        val master = worker("SwarmMaster[0]") { swarm.master() }

        repeat(size) {
            worker("SwarmSlave[${it + 1}]") { swarm.slave() }
        }

        while (master.isAlive)
            checkAndThrowIfExceptionOccurred()

        // check one more time if exception was in master
        checkAndThrowIfExceptionOccurred()
    }
}