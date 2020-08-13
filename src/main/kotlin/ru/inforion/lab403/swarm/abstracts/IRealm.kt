package ru.inforion.lab403.swarm.abstracts

import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.common.Mail
import java.io.Serializable
import java.nio.ByteBuffer

/**
 * {EN} Interface for parallelization drivers for Swarm {EN}
 */
interface IRealm {
    /**
     * {EN} Point where all nodes must reach before continue {EN}
     */
    fun barrier()

    /**
     * {EN}
     * Ordered number of node
     *
     * NOTE: Slave numbers start from 1, the 0 number is always assigned to Master
     * {EN}
     */
    val rank: Int

    /**
     * {EN} Total number of calculation nodes {EN}
     */
    val total: Int

    /**
     * {EN}
     * Method defines how to receive data from other node
     *
     * @param src node number from which waiting for data (-1 any node is good)
     * {EN}
     */
    fun recv(src: Int): Mail

    /**
     * {EN}
     * Method defines how to send a data to other calculation node
     *
     * @param buffer data to send
     * @param dst index of destination node
     * @param blocked wait until destination received data
     * {EN}
     */
    fun send(buffer: ByteBuffer, dst: Int, blocked: Boolean)

    /**
     * {EN}
     * Method defines how to pack data before send
     * This method extracted from [send] because for some task (i.e. context sending)
     *   we should not serialize context each time because it won't be differ.
     *
     * @param obj object to serialize into data buffer
     * {EN}
     */
    fun pack(obj: Serializable): ByteBuffer

    /**
     * {EN} Method defines what to do when realm driver run {EN}
     */
    fun run(swarm: Swarm)
}