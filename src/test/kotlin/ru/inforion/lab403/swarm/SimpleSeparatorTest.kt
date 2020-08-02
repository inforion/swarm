package ru.inforion.lab403.swarm

import org.junit.Test
import java.lang.IllegalArgumentException
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SimpleSeparatorTest {
    private val rand = Random(98271387612637)

    private fun <T>checkSeparation(src: Collection<T>, chunkNum: Int, result: Iterable<Collection<T>>, name: String) {
        assertEquals(src.toList(), result.flatten(), "$name: Source and flatten result not equal!")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEmpty() {
        val separatedList = emptyList<Int>().separate(0).toList()
        assertTrue(separatedList.isEmpty(), "Empty: Test on empty collection failed!")
    }

    @Test
    fun testEasyListInt() {
        val src = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val chunkNum = 3
        val result = src.separate(chunkNum)
        checkSeparation(src, chunkNum, result, "EasyListInt")
    }

    @Test
    fun testRandomListInt() {
        val src = List(200) { rand.nextInt()}
        val chunkNum = rand.nextInt(30)  // 5
        val result = src.separate(chunkNum)
        checkSeparation(src, chunkNum, result, "RandomListInt(count = $chunkNum)")
    }

    @Test
    fun testRandomSetDouble() {
        val src = List(500) { rand.nextDouble() }.toSet()
        val chunkNum = rand.nextInt(30) // 12
        val result = src.separate(chunkNum)
        checkSeparation(src, chunkNum, result, "RandomSetDouble")
    }

    @Test
    fun testRandomListPair() {
        val src = List(500) { rand.nextDouble() }.mapIndexed { index: Int, value: Double -> index to value }
        val chunkNum = rand.nextInt(30) // 12
        val result = src.separate(chunkNum)
        checkSeparation(src, chunkNum, result, "RandomListPair")
    }
}