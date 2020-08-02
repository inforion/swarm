package ru.inforion.lab403.swarm.abstracts

import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.common.Parcel
import java.net.InetAddress
import java.io.Serializable


abstract class ARealm {

    val hostname: String get() = InetAddress.getLocalHost().hostName

    val others by lazy { (0 until total).filter { it != rank }.toSet() }

    // Point where all nodes must reach before continue
    abstract fun barrier()

    abstract val rank: Int
    abstract val total: Int

    abstract fun recv(src: Int): Parcel

    abstract fun run(swarm: Swarm)

    inline fun recvFromMaster(): Parcel = recv(0)
    inline fun recvFromSlave(src: Int): Parcel = recv(src)

    inline fun recvFromAny(): Parcel = recv(-1)

    inline fun recvFromSomeUntil(block: (data: Parcel) -> Boolean, nodes: Set<Int>) {
        val received = mutableSetOf<Int>()
        do {
            val parcel = recv(-1)
            received.add(parcel.sender)
            val cont = block(parcel)
        } while (cont || received != nodes)
    }

    inline fun recvFromAllUntil(block: (data: Parcel) -> Boolean) {
        val received = mutableSetOf<Int>()
        do {
            val parcel = recv(-1)
            received.add(parcel.sender)
            val cont = block(parcel)
        } while (cont || received != others)
    }

    inline fun recvFromAnyUntil(block: (data: Parcel) -> Boolean) {
        do {
            val parcel = recv(-1)
            val cont = block(parcel)
        } while (cont)
    }


    inline fun recvFromAll(block: (data: Parcel) -> Unit) = recvFromAllUntil { parcel -> block(parcel); false }

    @Suppress("UNCHECKED_CAST")
    inline fun <T: Serializable> recvFromAllItUntil(block: (data: T) -> Boolean) = recvFromAllUntil { parcel -> block(parcel.obj as T) }

    @Suppress("UNCHECKED_CAST")
    inline fun <T: Serializable> recvFromAllIt(block: (data: T) -> Unit) = recvFromAllUntil { parcel -> block(parcel.obj as T); false }

    abstract fun send(obj: Serializable, dst: Int, blocked: Boolean)

    inline fun sendToMaster(obj: Serializable, blocked: Boolean = true) = send(obj, 0, blocked)
    inline fun sendToSlave(obj: Serializable, dst: Int, blocked: Boolean = true) = send(obj, dst, blocked)

    inline fun sendToAllEvenly(collection: Collection<Serializable>, blocked: Boolean = true) {
        // TODO: Add not 0 exclusion but rank
        collection.forEachIndexed { i, obj ->
            val indx = (i % (total - 1)) + 1
            send(obj, indx, blocked)
        }
    }

    inline fun <T> sendToAllEvenly(collection: Collection<T>, blocked: Boolean, block: (T) -> Serializable) {
        // TODO: Add not 0 exclusion but rank
        collection.forEachIndexed { k, item ->
            val indx = (k % (total - 1)) + 1
            val obj = block(item)
            send(obj, indx, blocked)
        }
    }

    inline fun sendToAllExceptMe(obj: Serializable, blocked: Boolean = true, block: (obj: Serializable) -> Unit) {
        (0 until total).filter { it != rank }.forEach {
            block(obj)
            send(obj, it, blocked)
        }
    }

    inline fun sendToAllExceptMe(obj: Serializable, blocked: Boolean = true) = sendToAllExceptMe(obj, blocked) { }

    inline fun sendToAll(obj: Serializable, blocked: Boolean = true) = (0 until total).forEach { send(obj, it, blocked) }

    abstract fun asyncRequestCount(): Int
}