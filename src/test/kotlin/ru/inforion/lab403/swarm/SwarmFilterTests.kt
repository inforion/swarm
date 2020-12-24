package ru.inforion.lab403.swarm


import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import ru.inforion.lab403.common.logging.logger
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


internal fun sha256_filter_list(swarm: Swarm, size: Int, count: Int, prefix: String) =
    List(size) { it }.parallelize(swarm).filter { sha256Test(it, count).startsWith(prefix) }


@ExperimentalTime
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
internal abstract class SwarmFilterTestsBase(private val threads: Int, private val size: Int, private val count: Int) {
    companion object {
        val log = logger()
    }

    private val expected = measureTimedValue { List(size) { it }.filter { sha256Test(it, count).startsWith("1") } }

    @Test
    internal fun t01_filter_list() = threadsSwarm(threads) { swarm ->
        val actual = measureTimedValue { sha256_filter_list(swarm, size, count, "1") }
        log.info { "filter_list_${threads}_${size}_${count}_Test single time = ${expected.duration} parallel time = ${actual.duration} result size = ${expected.value.size}" }
        assertEquals(expected.value, actual.value)
    }
}

@ExperimentalTime internal class SwarmFilterTestG00 : SwarmFilterTestsBase(6, 5000, 128)
@ExperimentalTime internal class SwarmFilterTestG01 : SwarmFilterTestsBase(6, 1000, 128)
@ExperimentalTime internal class SwarmFilterTestG02 : SwarmFilterTestsBase(6, 1000, 1024)