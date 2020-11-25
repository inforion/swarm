package ru.inforion.lab403.swarm.common

import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.abstracts.IRealm
import ru.inforion.lab403.swarm.implementations.MPI
import ru.inforion.lab403.swarm.implementations.Threads
import ru.inforion.lab403.swarm.implementations.recvFromAny
import ru.inforion.lab403.swarm.implementations.sendToMaster
import ru.inforion.lab403.swarm.interfaces.ITask

/**
 * Class defines slave nodes workers
 *
 * @param realm parallelization driver to use, see [MPI] or [Threads]
 * @param working start slave or not, after starting indicate slave running state
 * @param context current slave context, may be used for stateful parallelization tasks
 */
internal class Slave(private val realm: IRealm, var working: Boolean = true, var context: Any? = null) {
    companion object {
        @Transient val log = logger()
    }

    /**
     * Synchronize slave with others.
     * Execution will continue after all nodes call [barrier]
     */
    fun barrier() = realm.barrier()

    /**
     * Ordered number of slave
     *
     * NOTE: Slave numbers start from 1, the 0 number is always assigned to Master
     */
    val rank get() = realm.rank

    /**
     * Send response to master after action
     *
     * @param result execution result
     * @param index order of result (i.e. index in from input or slave rank)
     */
    fun <R> response(result: R, index: Int) = realm.sendToMaster(Response(result, index), true)

    /**
     * Method executes when slave started
     */
    fun run() {
        while (working) {
            val parcel = realm.recvFromAny()
            require(parcel.obj is ITask)
            parcel.obj.execute(this)
        }
        log.finest { "Stopping slave $rank" }
    }
}
