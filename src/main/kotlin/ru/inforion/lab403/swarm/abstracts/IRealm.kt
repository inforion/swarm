package ru.inforion.lab403.swarm.abstracts

import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.common.Mail
import java.io.Serializable
import java.nio.ByteBuffer


interface IRealm {
    /**
     * {EN} Point where all nodes must reach before continue {EN}
     */
    fun barrier()

    /**
     * {RU}
     * Свойство возвращает номер данного узла из всего списка, например:
     *   Для исполнения на потоках - номер потока
     *   для исполнения на MPI - номер узла
     * {RU}
     */
    val rank: Int

    /**
     * {RU}
     * Общее количество вычислительных узлов
     * {RU}
     */
    val total: Int

    /**
     * {RU}
     * Принять посылку от заданного узла (-1 от любого узла)
     *
     * @param src номер узла, от которого следует принять посылку
     * {RU}
     */
    fun recv(src: Int): Mail

    /**
     * {RU}
     *
     * {RU}
     */
    fun send(buffer: ByteBuffer, dst: Int, blocked: Boolean)

    fun pack(obj: Serializable): ByteBuffer

    fun run(swarm: Swarm)
}