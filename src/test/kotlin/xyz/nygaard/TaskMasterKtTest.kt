package xyz.nygaard

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

fun mergeActions(a: List<Action>, b: List<Action>): List<Action> {
    return merge(a, b).runnableActions
}

internal class TaskMasterKtTest {

    private val clear = ClearOrders()
    private val addA = AddAsk(mockk())
    private val keepA = KeepAsk(mockk())

    private val addB = AddBid(mockk())
    private val keepB = KeepBid(mockk())

    @Test
    fun `clear a + a + empty b`() {
        val a = listOf(clear, addA)
        val b = emptyList<Action>()
        val res = mergeActions(a, b)
        assertEquals(listOf(clear, addA), res)
    }

    @Test
    fun `clear a + a + b`() {
        val a = listOf(clear, addA)
        val b = listOf(addB)
        val res = mergeActions(a, b)
        assertEquals(listOf(clear, addA, addB), res)
    }

    @Test
    fun `clear a + a + clear b + b`() {
        val a = listOf(clear, addA)
        val b = listOf(clear, addB)
        val res = mergeActions(a, b)
        assertEquals(listOf(clear, addA, addB), res)
    }

    @Test
    fun `keep a + clear b + b`() {
        val a = listOf(keepA)
        val b = listOf(clear, addB)
        val res = mergeActions(a, b)
        assertEquals(listOf(clear, keepA, addB), res)
    }

    @Test
    fun `keep b + clear a + a`() {
        val a = listOf(keepB)
        val b = listOf(clear, addA)
        val res = mergeActions(a, b)
        assertEquals(listOf(clear, keepB, addA), res)
    }

    @Test
    fun `keep a + keep b`() {
        val a = listOf(keepA)
        val b = listOf(keepB)
        val res = mergeActions(a, b)
        assertTrue(res.isEmpty())
    }

    @Test
    fun `keep a`() {
        val a = listOf(keepA)
        val b = emptyList<Action>()
        val res = mergeActions(a, b)
        assertTrue(res.isEmpty())
    }

    @Test
    fun `keep b`() {
        val a = emptyList<Action>()
        val b = listOf(keepB)
        val res = mergeActions(a, b)
        assertTrue(res.isEmpty())
    }

    @Test
    fun `keep a + b` (){
        val a = listOf(keepA)
        val b = listOf(addB)
        val res = mergeActions(a, b)
        assertEquals(listOf(addB), res)
    }

}