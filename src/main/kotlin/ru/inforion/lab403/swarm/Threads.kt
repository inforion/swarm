package ru.inforion.lab403.swarm

import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.abstracts.ARealm
import ru.inforion.lab403.swarm.common.Parcel
import ru.inforion.lab403.swarm.io.deserialize
import ru.inforion.lab403.swarm.io.serialize
import java.io.Serializable
import java.util.ArrayList
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import kotlin.system.exitProcess

class Threads(val size: Int) : ARealm() {

    companion object {
        @Transient val log = logger()
    }

    class Worker(val block: () -> Unit) : Thread() {
        val incoming = LinkedBlockingQueue<Pair<Int, ByteArray>>()

        init {
            start()
        }

        override fun run() {
            block()
        }
    }

    override fun asyncRequestCount(): Int = 0

    override fun send(obj: Serializable, dst: Int, blocked: Boolean) {
        val buffer = obj.serialize(directed = false)
        threads[dst].incoming.put(Pair(rank(), buffer.array()))
    }

    override fun recv(src: Int): Parcel {
        if (src != -1) {
            while (true) {
                val info = threads[rank()].incoming.take()
                if (info.first == src)
                    return Parcel(info.first, info.second.deserialize())
                threads[rank()].incoming.put(info)
            }
        } else {
            val info = threads[rank()].incoming.take()
            return Parcel(info.first, info.second.deserialize())
        }
    }

    override fun rank(): Int = threads.indexOf(Thread.currentThread())
    override fun total(): Int = threads.size

    private val barrier = CyclicBarrier(size)

    override fun barrier() {
        barrier.await()
    }

    private val threads = ArrayList<Worker>()

    override fun run(swarm: Swarm) {
        threads.add(Worker { swarm.master() })

        for (k in 2..size) {
            threads.add(Worker {
                try {
                    swarm.slave()
                } catch (exc: InterruptedException) {
                    log.info { "Thread ${rank()} interrupted" }
                } catch (error: Exception) {
                    log.severe { "Node[${rank()}] -> execution can't be continued:\n${error.message}" }
                    error.printStackTrace()
                    exitProcess(-1)
                }
            })
        }

        threads.forEach { it.join() }
    }
}