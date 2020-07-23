package ru.inforion.lab403.swarm.common

import ru.inforion.lab403.swarm.abstracts.ARealm
import ru.inforion.lab403.swarm.interfaces.ITask

class Slave(private val realm: ARealm, var working: Boolean = true, var context: Any? = null) {
    fun barrier() = realm.barrier()

    val rank get() = realm.rank()

    fun <R> response(result: R, index: Int) = realm.sendToMaster(Response(result, index), true)

    fun run() {
        while (working) {
            val parcel = realm.recvFromAny()
            require(parcel.obj is ITask)
            parcel.obj.execute(this)
        }
    }
}
