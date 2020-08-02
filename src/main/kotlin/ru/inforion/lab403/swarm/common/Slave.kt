package ru.inforion.lab403.swarm.common

import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.abstracts.IRealm
import ru.inforion.lab403.swarm.implementations.recvFromAny
import ru.inforion.lab403.swarm.implementations.sendToMaster
import ru.inforion.lab403.swarm.interfaces.ITask

class Slave(private val realm: IRealm, var working: Boolean = true, var context: Any? = null) {
    companion object {
        @Transient val log = logger()
    }

    fun barrier() = realm.barrier()

    val rank get() = realm.rank

    fun <R> response(result: R, index: Int) = realm.sendToMaster(Response(result, index), true)

    fun run() {
        while (working) {
            val parcel = realm.recvFromAny()
            require(parcel.obj is ITask)
            parcel.obj.execute(this)
        }
        log.finest { "Stopping slave $rank" }
    }
}
