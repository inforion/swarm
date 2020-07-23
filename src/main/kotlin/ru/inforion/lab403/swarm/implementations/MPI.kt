package ru.inforion.lab403.swarm.implementations

import mpi.MPI
import mpi.Request
import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.abstracts.ARealm
import ru.inforion.lab403.swarm.common.Parcel
import ru.inforion.lab403.swarm.io.deserialize
import ru.inforion.lab403.swarm.io.serialize
import java.io.Serializable
import java.util.*
import kotlin.system.exitProcess


class MPI(vararg val args: String) : ARealm() {
    companion object {
        @Transient val log = logger()
    }

    // Suppose that queue can't reach MAX_INT size
    private var messageNo = 0

    private val requests = LinkedList<Request>()

    private fun gcRequests() {
        if (requests.size > 1024 * 1024) {
            requests.removeAll {
                val isCompleted = it.test()
                if (isCompleted)
                    it.free()
                return@removeAll isCompleted
            }
        }
    }

    override fun asyncRequestCount(): Int = requests.filter { !it.test() }.size

    override fun send(obj: Serializable, dst: Int, blocked: Boolean) {
        gcRequests()
        val buffer = obj.serialize(directed = true)
        val request = MPI.COMM_WORLD.iSend(buffer, buffer.limit(), MPI.BYTE, dst, messageNo)
        requests.add(request)
        messageNo++
    }

    override fun recv(src: Int): Parcel {
        gcRequests()

        val id = if (src != -1) src else MPI.ANY_SOURCE

        val status = MPI.COMM_WORLD.probe(id, MPI.ANY_TAG)

        val count = status.getCount(MPI.BYTE)
        val data = ByteArray(count)

        MPI.COMM_WORLD.recv(data, count, MPI.BYTE, status.source, status.tag)

//        log.finest { "[${rank()}] recv from: $src/${status.source} $count bytes" }
        return Parcel(status.source, data.deserialize())
    }

    override fun rank(): Int = MPI.COMM_WORLD.rank

    override fun total(): Int = MPI.COMM_WORLD.size

    override fun barrier() {
        gcRequests()
        MPI.COMM_WORLD.barrier()
    }

    override fun run(swarm: Swarm) {
        MPI.Init(args)
        try {
            if (rank() == 0) {
                swarm.master()
            } else {
                swarm.slave()
            }
        } catch (error: Exception) {
            MPI.Finalize()
            log.severe { "Node[${rank()}] -> execution can't be continued:\n${error.message}" }
            error.printStackTrace()
            exitProcess(-1)
        }
    }
}