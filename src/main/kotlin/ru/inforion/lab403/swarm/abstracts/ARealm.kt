package ru.inforion.lab403.swarm.abstracts

import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.common.Parcel
import java.net.InetAddress
import java.io.Serializable


abstract class ARealm {

    val hostname: String get() = InetAddress.getLocalHost().hostName

    fun others() = (0 until total()).filter { it != rank() }.toSet()

    // Point where all nodes must reach before continue
    abstract fun barrier()

    abstract fun rank(): Int
    abstract fun total(): Int

    abstract fun recv(src: Int): Parcel

    abstract fun run(swarm: Swarm)

    fun recvFromMaster(): Parcel = recv(0)
    fun recvFromSlave(src: Int): Parcel = recv(src)

    fun recvFromAny(): Parcel = recv(-1)

    fun recvFromSomeUntil(block: (data: Parcel) -> Boolean, nodes: Set<Int>) {
        val received = mutableSetOf<Int>()
        do {
            val parcel = recv(-1)
            received.add(parcel.sender)
            val cont = block(parcel)
        } while (cont || received != nodes)
    }

    fun recvFromAllUntil(block: (data: Parcel) -> Boolean) {
        val received = mutableSetOf<Int>()
        do {
            val parcel = recv(-1)
            received.add(parcel.sender)
            val cont = block(parcel)
        } while (cont || received != others())
    }

    fun recvFromAnyUntil(block: (data: Parcel) -> Boolean) {
        do {
            val parcel = recv(-1)
            val cont = block(parcel)
        } while (cont)
    }


    fun recvFromAll(block: (data: Parcel) -> Unit) = recvFromAllUntil { parcel -> block(parcel); false }

    @Suppress("UNCHECKED_CAST")
    fun <T: Serializable> recvFromAllItUntil(block: (data: T) -> Boolean) = recvFromAllUntil { parcel -> block(parcel.obj as T) }

    @Suppress("UNCHECKED_CAST")
    fun <T: Serializable> recvFromAllIt(block: (data: T) -> Unit) = recvFromAllUntil { parcel -> block(parcel.obj as T); false }

    abstract fun send(obj: Serializable, dst: Int, blocked: Boolean)
    fun sendToMaster(obj: Serializable, blocked: Boolean = true) = send(obj, 0, blocked)
    fun sendToSlave(obj: Serializable, dst: Int, blocked: Boolean = true) = send(obj, dst, blocked)

    fun sendToAllEvenly(collection: Collection<Serializable>, blocked: Boolean = true) {
        // TODO: Add not 0 exclusion but rank
        collection.forEachIndexed { i, obj ->
            val indx = (i % (total() - 1)) + 1
            send(obj, indx, blocked)
        }
    }

    fun <T> sendToAllEvenly(collection: Collection<T>, blocked: Boolean, block: (T) -> Serializable) {
        // TODO: Add not 0 exclusion but rank
        collection.forEachIndexed { k, item ->
            val indx = (k % (total() - 1)) + 1
            val obj = block(item)
            send(obj, indx, blocked)
        }
    }

    fun sendToAllExceptMe(obj: Serializable, blocked: Boolean = true, block: (obj: Serializable) -> Unit) {
        (0 until total()).filter { it != rank() }.forEach {
            block(obj)
            send(obj, it, blocked)
        }
    }

    fun sendToAllExceptMe(obj: Serializable, blocked: Boolean = true) = sendToAllExceptMe(obj, blocked) { }

    fun sendToAll(obj: Serializable, blocked: Boolean = true) {
        (0 until total()).forEach { send(obj, it, blocked) }
    }

    abstract fun asyncRequestCount(): Int
}