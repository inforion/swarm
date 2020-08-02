package ru.inforion.lab403.swarm


import org.junit.Test
import ru.inforion.lab403.common.extensions.hexlify
import ru.inforion.lab403.common.extensions.random
import ru.inforion.lab403.common.extensions.serialize
import ru.inforion.lab403.common.logging.logger
import ru.inforion.lab403.swarm.io.deserialize
import ru.inforion.lab403.swarm.io.serialize
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SwarmTests {
    companion object {
        val log = logger()
    }

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
            log.info { context.toString() }
            value.toUpperCase().split("/")[1].toInt(16)
        }

        assertEquals(listOf(0xAB, 0xCD, 0xDD, 0xAB, 0xCD, 0xDD, 0xAB, 0xCD, 0xDD, 0xAB, 0xCD, 0xDD), mapResults)

        val getResults = swarm.get { context: Context -> context.x }

        log.info { getResults.toString() }

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
        val no1 = swarm.addReceiveNotifier { log.info { "1-$it" } }
        val no2 = swarm.addReceiveNotifier { log.info { "2-$it" } }
        swarm.removeReceiveNotifier(no1)
        swarm.removeReceiveNotifier { log.info { "2-$it" } }
        val result = Array(100) { it }.parallelize(swarm).map { it.toString() }
        assertEquals(Array(100) { it.toString() }.toList(), result)
    }

    private class MemoryConsumption(val max: Long, val free: Long, val total: Long) {
        companion object {
            fun get() = with (Runtime.getRuntime()) {
                val max = maxMemory() / 1024 / 1024
                val free = freeMemory() / 1024 / 1024
                val total = totalMemory() / 1024 / 1024
                MemoryConsumption(max, free, total)
            }
        }

        override fun toString() = "max=${max}MB free=${free}MB total=${total}MB"
    }

    @Test
    fun objectGzipStream() {
        val string = "Some insignificant string"

        val baos = ByteArrayOutputStream()
        val gos = GZIPOutputStream(baos)
        val oos = ObjectOutputStream(gos)

        oos.writeUnshared(string)

        gos.finish()

        baos.write(0x69)

        val array = baos.toByteArray()

        log.info { array.hexlify() }

        val bais = array.inputStream()

        log.info {"bais.available=${bais.available()}" }

        val gis = GZIPInputStream(bais)
        val ois = ObjectInputStream(gis)

        val result = ois.readUnshared() as String

        log.config { "bais.available=${bais.available()} due to GZIP = 0 but should be 1 (marker)" }

        assertEquals(string, result)
    }

    @Test
    fun serializationGzipStream() {
        val string = "Some insignificant string-".repeat(1000)

        val array = string.serialize(false, true).array()
        log.info { array.hexlify() }
        val result = array.deserialize<String>(true)

        assertEquals(string, result)
    }

    private fun largeHeapObjectRun(array: ByteArray) {
        System.gc()

        threadsSwarm(size, true) { swarm ->
            val start = System.currentTimeMillis()
            swarm.context { array }
            val time = System.currentTimeMillis() - start
            log.info { "Finish context compress, time = $time" }

            swarm.eachContext { context: ByteArray -> context.sum() }

            val mc2 = MemoryConsumption.get()
            log.info { mc2.toString() }
        }

        System.gc()

        threadsSwarm(size, false) { swarm ->
            val start = System.currentTimeMillis()
            swarm.context { array }
            val time = System.currentTimeMillis() - start
            log.info { "Finish context no compress, time = $time" }

            swarm.eachContext { context: ByteArray -> context.sum() }

            val mc2 = MemoryConsumption.get()
            log.info { mc2.toString() }
        }
    }

    @Test
    fun largeZeroHeapObjectTest() {
        log.config { "Staring zero object..." }
        val mc0 = MemoryConsumption.get()
        log.info { mc0.toString() }

        val array = ByteArray(0x1000_0000 ushr 2)

        val mc1 = MemoryConsumption.get()
        log.info { mc1.toString() }

        largeHeapObjectRun(array)
    }

    @Test
    fun largeRandomHeapObjectTest() {
        log.config { "Staring random object..." }
        val mc0 = MemoryConsumption.get()
        log.info { mc0.toString() }

        val array = random.randbytes(0x1000_0000 ushr 2)

        val mc1 = MemoryConsumption.get()
        log.info { mc1.toString() }

        largeHeapObjectRun(array)
    }
}
