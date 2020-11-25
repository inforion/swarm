package ru.inforion.lab403.swarm.abstracts

import ru.inforion.lab403.swarm.Swarm
import ru.inforion.lab403.swarm.common.Mail
import java.io.Serializable
import java.nio.ByteBuffer

/**
 * Interface for parallelization drivers for Swarm
 */
interface IRealm {
    /**
     * Point where all nodes must reach before continue
     */
    fun barrier()

    /**
     * Ordered number of node
     *
     * NOTE: Slave numbers start from 1, the 0 number is always assigned to Master
     */
    val rank: Int

    /**
     * Total number of calculation nodes
     */
    val total: Int

    /**
     * Method defines how to receive data from other node
     *
     * @param src node number from which waiting for data (-1 any node is good)
     */
    fun recv(src: Int): Mail

    /**
     * Method defines how to send a data to other calculation node
     *
     * @param buffer data to send
     * @param dst index of destination node
     * @param blocked wait until destination received data
     */
    fun send(buffer: ByteBuffer, dst: Int, blocked: Boolean)

    /**
     * Method defines how to pack data before send
     * This method extracted from [send] because for some task (i.e. context sending)
     *   we should not serialize context each time because it won't be differ.
     *
     * @param obj object to serialize into data buffer
     */
    fun pack(obj: Serializable): ByteBuffer

    /**
     * Method defines what to do when realm driver run
     */
    fun run(swarm: Swarm)
}