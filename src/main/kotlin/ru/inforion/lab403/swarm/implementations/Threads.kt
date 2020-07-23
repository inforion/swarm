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

    private val incoming = Array(size + 1) { LinkedBlockingQueue<Pair<Int, ByteArray>>() }

    private val barrier = CyclicBarrier(size + 1)

    private val threads = ArrayList<Thread>()

    override fun asyncRequestCount(): Int = 0

    override fun send(obj: Serializable, dst: Int, blocked: Boolean) {
        val buffer = obj.serialize(directed = false)
        incoming[dst].put(Pair(rank(), buffer.array()))
    }

    override fun recv(src: Int): Parcel {
        if (src != -1) {
            while (true) {
                val info = incoming[rank()].take()
                if (info.first == src)
                    return Parcel(info.first, info.second.deserialize())
                incoming[rank()].put(info)
            }
        } else {
            val info = incoming[rank()].take()
            return Parcel(info.first, info.second.deserialize())
        }
    }

    override fun rank(): Int = threads.indexOf(Thread.currentThread())
    override fun total(): Int = threads.size

    override fun barrier() {
        barrier.await()
    }

    override fun run(swarm: Swarm) {
        threads.add(Thread.currentThread())

        repeat(size) {
            threads.add(thread {
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

        swarm.master()
    }
}