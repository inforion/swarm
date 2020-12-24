package ru.inforion.lab403.swarm


import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import ru.inforion.lab403.common.extensions.*
import ru.inforion.lab403.common.logging.logger
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


internal fun sha256_map_list(swarm: Swarm, size: Int, count: Int) = List(size) { it }.parallelize(swarm).map { sha256Test(it, count) }
internal fun sha256_map2_list(swarm: Swarm, size: Int, count: Int) = List(size) { it }.parallelize(swarm).map2 { sha256Test(it, count) }
internal fun sha256_map_seq(swarm: Swarm, size: Int, count: Int) = sequence(size) { it }.parallelize(swarm).map { sha256Test(it, count) }
internal fun sha256_map2_seq(swarm: Swarm, size: Int, count: Int) = sequence(size) { it }.parallelize(swarm).map2 { sha256Test(it, count) }

@ExperimentalTime
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
internal abstract class SwarmMapTestsBase(private val threads: Int, private val size: Int, private val count: Int) {
    companion object {
        val log = logger()
    }

    private val list = measureTimedValue { List(size) { sha256Test(it, count) } }
    private val seq = measureTimedValue { sequence(size) { sha256Test(it, count) }.toList() }

    @Test
    internal fun t01_map_list() = threadsSwarm(threads) { swarm ->
        val actual = measureTimedValue { sha256_map_list(swarm, size, count) }
        log.info { "map_list_${threads}_${size}_${count}_Test single time = ${list.duration} parallel time = ${actual.duration}" }
        assertEquals(list.value, actual.value)
    }

    @Test
    internal fun t02_map2_list() = threadsSwarm(threads) { swarm ->
        val actual = measureTimedValue { sha256_map2_list(swarm, size, count) }
        log.info { "map2_list_${threads}_${size}_${count}_Test single time = ${list.duration} parallel time = ${actual.duration}" }
        assertEquals(list.value, actual.value)
    }

    @Test
    internal fun t03_map_seq() = threadsSwarm(threads) { swarm ->
        val actual = measureTimedValue { sha256_map_seq(swarm, size, count) }
        log.info { "map_seq_${threads}_${size}_${count}_Test time = ${seq.duration} parallel time = ${actual.duration}" }
        assertEquals(seq.value, actual.value)
    }

    @Test
    internal fun t04_map2_seq() = threadsSwarm(threads) { swarm ->
        val actual = measureTimedValue { sha256_map2_seq(swarm, size, count) }
        log.info { "map2_seq_${threads}_${size}_${count}_Test single time = ${seq.duration} parallel time = ${actual.duration}" }
        assertEquals(seq.value, actual.value)
    }
}

@ExperimentalTime internal class SwarmMapTestG00 : SwarmMapTestsBase(6, 5000, 1)
@ExperimentalTime internal class SwarmMapTestG01 : SwarmMapTestsBase(6, 5000, 64)
@ExperimentalTime internal class SwarmMapTestG02 : SwarmMapTestsBase(6, 5000, 128)
@ExperimentalTime internal class SwarmMapTestG03 : SwarmMapTestsBase(6, 5000, 256)
@ExperimentalTime internal class SwarmMapTestG04 : SwarmMapTestsBase(6, 5000, 1024)
@ExperimentalTime internal class SwarmMapTestG05 : SwarmMapTestsBase(6, 5000, 2048)

@ExperimentalTime internal class SwarmMapTestG06 : SwarmMapTestsBase(6, 1000, 4096)
@ExperimentalTime internal class SwarmMapTestG07 : SwarmMapTestsBase(6, 1000, 8192)

@ExperimentalTime internal class SwarmMapTestG08 : SwarmMapTestsBase(2, 5000, 256)
@ExperimentalTime internal class SwarmMapTestG09 : SwarmMapTestsBase(2, 5000, 1024)
