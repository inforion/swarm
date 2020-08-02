package ru.inforion.lab403.swarm.implementations

import mpi.MPI
import mpi.Request
import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.abstracts.IRealm
import ru.inforion.lab403.swarm.common.Mail
import ru.inforion.lab403.swarm.io.deserialize
import ru.inforion.lab403.swarm.io.serialize
import java.io.Serializable
import java.nio.ByteBuffer
import java.util.*
import kotlin.system.exitProcess


class MPI(vararg val args: String, val compress: Boolean) : IRealm {
    companion object {
        @Transient val log = logger()

        const val gcRequestsThreshold = 1024 * 1024
    }

    // Suppose that queue can't reach MAX_INT size
    private var messageNo = 0

    private val requests = LinkedList<Request>()

    private fun gcRequests() {
        if (requests.size > gcRequestsThreshold) {
            requests.removeAll {
                val isCompleted = it.test()
                if (isCompleted)
                    it.free()
                return@removeAll isCompleted
            }
        }
    }

    private fun asyncRequestCount(): Int = requests.filter { !it.test() }.size

    override fun pack(obj: Serializable): ByteBuffer {
        gcRequests()
        return obj.serialize(true, compress)
    }

    override fun send(buffer: ByteBuffer, dst: Int, blocked: Boolean) {
        val request = MPI.COMM_WORLD.iSend(buffer, buffer.limit(), MPI.BYTE, dst, messageNo)
        requests.add(request)
        messageNo++
    }

    override fun recv(src: Int): Mail {
        gcRequests()

        val id = if (src != -1) src else MPI.ANY_SOURCE

        val status = MPI.COMM_WORLD.probe(id, MPI.ANY_TAG)

        val count = status.getCount(MPI.BYTE)
        val data = ByteArray(count)

        MPI.COMM_WORLD.recv(data, count, MPI.BYTE, status.source, status.tag)

//        log.finest { "[${rank()}] recv from: $src/${status.source} $count bytes" }
        return Mail(status.source, data.deserialize(compress))
    }

    override val rank = MPI.COMM_WORLD.rank

    override val total = MPI.COMM_WORLD.size

    override fun barrier() {
        gcRequests()
        MPI.COMM_WORLD.barrier()
    }

    override fun run(swarm: Swarm) {
        MPI.Init(args)
        try {
            if (rank == 0) {
                swarm.master()
            } else {
                swarm.slave()
            }
        } catch (error: Exception) {
            MPI.Finalize()
            log.severe { "Node[${rank}] -> execution can't be continued:\n${error.message}" }
            error.printStackTrace()
            exitProcess(-1)
        }
    }
}