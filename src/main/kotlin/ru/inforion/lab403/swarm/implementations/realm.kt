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

inline fun IRealm.recvFromOthersUntil(predicate: (data: Mail) -> Boolean) {
    do {
        val parcel = recvFromAny()
        val cont = predicate(parcel)
    } while (cont)
}

inline fun IRealm.recvCountFromOthers(count: Int, action: (data: Mail) -> Unit) {
    var remain = count

    recvFromOthersUntil { mail ->
        action(mail)
        --remain != 0
    }
}

inline fun IRealm.recvFromOthers(action: (data: Mail) -> Unit) =
    recvFromOthersUntil { parcel -> action(parcel); false }

inline fun IRealm.sendToMaster(obj: Serializable, blocked: Boolean = true) = send(pack(obj), 0, blocked)
inline fun IRealm.sendToSlave(obj: Serializable, dst: Int, blocked: Boolean = true) = send(pack(obj), dst, blocked)

/**
 * Sends item from iterable or collection or sequence to slave
 *
 * @param index iterable item index
 * @param item iterable data to send
 * @param blocked wait until received or not
 * @param transform make a transformation of [item] before send
 */
inline fun <T> IRealm.sendIterableItem(index: Int, item: T, blocked: Boolean, transform: (T) -> Serializable) {
    // TODO: Add not 0 exclusion but rank
    val slave = index % (total - 1) + 1
    val obj = transform(item)
    send(pack(obj), slave, blocked)
}

inline fun <T> IRealm.sendToAllEvenly(sequence: Sequence<T>, blocked: Boolean, transform: (T) -> Serializable): Int {
    var total = 0
    sequence.forEachIndexed { k, item -> sendIterableItem(k, item, blocked, transform); total++ }
    return total
}

inline fun <T> IRealm.sendToAllEvenly(iterable: Iterable<T>, blocked: Boolean, transform: (T) -> Serializable): Int {
    var total = 0
    iterable.forEachIndexed { k, item -> sendIterableItem(k, item, blocked, transform); total++ }
    return total
}

inline fun IRealm.sendToOthers(obj: Serializable, blocked: Boolean = true) {
    val data = pack(obj)
    others.forEach { send(data, it, blocked) }
}

inline fun IRealm.sendToAll(obj: Serializable, blocked: Boolean = true) = all.forEach { send(pack(obj), it, blocked) }