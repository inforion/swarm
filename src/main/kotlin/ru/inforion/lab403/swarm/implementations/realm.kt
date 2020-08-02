@file:Suppress("NOTHING_TO_INLINE")

package ru.inforion.lab403.swarm.implementations

import ru.inforion.lab403.swarm.abstracts.IRealm
import ru.inforion.lab403.swarm.common.Mail
import java.io.Serializable

val IRealm.all get() = (0 until total)
val IRealm.others get() = all.filter { it != rank }

inline fun IRealm.recvFromMaster(): Mail = recv(0)
inline fun IRealm.recvFromSlave(src: Int): Mail = recv(src)

inline fun IRealm.recvFromAny(): Mail = recv(-1)

inline fun IRealm.recvFromOthersUntil(block: (data: Mail) -> Boolean) {
    do {
        val parcel = recv(-1)
        val cont = block(parcel)
    } while (cont)
}

inline fun IRealm.recvFromOthers(block: (data: Mail) -> Unit) =
    recvFromOthersUntil { parcel -> block(parcel); false }

inline fun IRealm.sendToMaster(obj: Serializable, blocked: Boolean = true) = send(pack(obj), 0, blocked)
inline fun IRealm.sendToSlave(obj: Serializable, dst: Int, blocked: Boolean = true) = send(pack(obj), dst, blocked)

inline fun <T> IRealm.sendToAllEvenly(collection: Collection<T>, blocked: Boolean, block: (T) -> Serializable) {
    // TODO: Add not 0 exclusion but rank
    collection.forEachIndexed { k, item ->
        val index = k % (total - 1) + 1
        val obj = block(item)
        send(pack(obj), index, blocked)
    }
}

inline fun IRealm.sendToAllEvenly(collection: Collection<Serializable>, blocked: Boolean = true) =
    sendToAllEvenly(collection, blocked) { it }

inline fun IRealm.sendToOthers(obj: Serializable, blocked: Boolean = true) {
    val data = pack(obj)
    others.forEach { send(data, it, blocked) }
}

inline fun IRealm.sendToAll(obj: Serializable, blocked: Boolean = true) = all.forEach { send(pack(obj), it, blocked) }