package ru.inforion.lab403.swarm


import org.junit.Test
import ru.inforion.lab403.common.extensions.hexlify
import ru.inforion.lab403.common.extensions.random
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwarmTests {
    private val size = 4  // tests depend on it and should be changed

    @Test
    fun rankTest() = threadsSwarm(size) { swarm ->
        val result = swarm.context { it }.get { rank: Int -> rank }
        assertEquals(listOf(1, 2, 3, 4), result.toList())
    }

    @Test
    fun mapContextTest() = threadsSwarm(size) { swarm ->
        data class Context(var x: Int, val snapshot: String)

        swarm.context {
            val snapshot = random.randbytes(10)
            Context(0, snapshot.hexlify())
        }

        val mapResults = listOf(
            "test/ab",
            "test/cd",
            "test/dd",
            "test/ab",
            "test/cd",
            "test/dd",
            "test/ab",
            "test/cd",
            "test/dd",
            "test/ab",
            "test/cd",
            "test/dd"
        ).parallelize(swarm).mapContext { context: Context, value ->
            context.x += 1
            println(context)
            value.toUpperCase().split("/")[1].toInt(16)
        }

        assertEquals(listOf(0xAB, 0xCD, 0xDD, 0xAB, 0xCD, 0xDD, 0xAB, 0xCD, 0xDD, 0xAB, 0xCD, 0xDD), mapResults)

        val getResults = swarm.get { context: Context -> context.x }

        println(getResults)

        assertEquals(getResults.size, size)
        assertTrue { getResults.none { it > 11 } }
    }

    @Test
    fun mapTest() = threadsSwarm(size) { swarm ->
        val result = listOf("test/ab", "test/cd", "test/dd").parallelize(swarm).map {
            it.toUpperCase().split("/")[1].toInt(16)
        }

        assertEquals(listOf(0xAB, 0xCD, 0xDD), result)
    }

    @Test
    fun contextCreateTest() = threadsSwarm(size) { swarm ->
        val result = swarm.context { "context-$it" }.get { context: String -> context }
        assertEquals(listOf("context-1", "context-2", "context-3", "context-4"), result)
    }

    @Test
    fun notifyReceiveTest() = threadsSwarm(size) { swarm ->
        val no1 = swarm.addReceiveNotifier { println("1-$it") }
        val no2 = swarm.addReceiveNotifier { println("2-$it") }
        swarm.removeReceiveNotifier(no1)
        swarm.removeReceiveNotifier { println("2-$it") }
        val result = Array(100) { it }.parallelize(swarm).map { it.toString() }
        assertEquals(Array(100) { it.toString() }.toList(), result)
    }
}
