package ru.inforion.lab403.swarm


import org.junit.Test
import ru.inforion.lab403.common.extensions.hexlify
import ru.inforion.lab403.common.extensions.random
import kotlin.test.assertEquals

class SwarmTests {

    @Test
    fun simpleContextTest() {
        data class Context(var x: Int, val snapshot: String)

        threadsSwarm(4) { swarm ->
            swarm.context {
                val snapshot = random.randbytes(10)
                Context(10, snapshot.hexlify())
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
                value.toUpperCase().split("/")[1].toInt(16)
            }

            assertEquals(listOf(0xAB, 0xCD, 0xDD, 0xAB, 0xCD, 0xDD, 0xAB, 0xCD, 0xDD, 0xAB, 0xCD, 0xDD), mapResults)

            val getResults = swarm.get { context: Context -> context.x }

            assertEquals(listOf(21, 21, 21, 21), getResults)
        }
    }

    @Test
    fun simpleNoContextTest() {
        threadsSwarm(4) { swarm ->
            val result = listOf("test/ab", "test/cd", "test/dd").parallelize(swarm).map {
                it.toUpperCase().split("/")[1].toInt(16)
            }

            assertEquals(listOf(0xAB, 0xCD, 0xDD), result)
        }
    }

    @Test
    fun kopycatTest() {
//    val device = stm32f042_example(null, "top", "example:gpiox_led")
//
//    val top = Module(null, "top")
//    val ram = RAM(top, "ram", 2000)
//
//    val result = listOf(1, 2, 3).parallelMap {
//        ram.toString()
//        val kopycat = Kopycat(null).apply { open(device, false, null) }
//        repeat(it * 2048) { kopycat.step() }
//        kopycat.core.stringify()
//    }.forEach { println(it) }
    }
}
