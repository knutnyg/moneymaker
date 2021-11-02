package xyz.nygaard

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xyz.nygaard.io.ActiveOrder

internal class TaskMasterKtTest {

    private val clearA = ClearOrders(ActiveOrder.OrderType.ask)
    private val addA = AddAsk(mockk())
    private val keepA = KeepAsk(mockk())

    private val clearB = ClearOrders(ActiveOrder.OrderType.bid)
    private val addB = AddBid(mockk())
    private val keepB = KeepBid(mockk())

    @Test
    fun `clear a + a + empty b`() {
        val a = listOf(clearA, addA)
        val b = emptyList<Action>()
        val res = merge(a, b)
        assertEquals(listOf(clearA, addA), res)
    }

    @Test
    fun `clear a + a + b`() {
        val a = listOf(clearA, addA)
        val b = listOf(addB)
        val res = merge(a, b)
        assertEquals(listOf(clearA, addA, addB), res)
    }

    @Test
    fun `clear a + a + clear b + b`() {
        val a = listOf(clearA, addA)
        val b = listOf(clearB, addB)
        val res = merge(a, b)
        assertEquals(listOf(clearA, addA, addB), res)
    }

    @Test
    fun `keep a + clear b + b`() {
        val a = listOf(keepA)
        val b = listOf(clearB, addB)
        val res = merge(a, b)
        assertEquals(listOf(clearB, keepA, addB), res)
    }

    @Test
    fun `keep b + clear a + a`() {
        val a = listOf(keepB)
        val b = listOf(clearA, addA)
        val res = merge(a, b)
        assertEquals(listOf(clearA, keepB, addA), res)
    }

    @Test
    fun `keep a + keep b`() {
        val a = listOf(keepA)
        val b = listOf(keepB)
        val res = merge(a, b)
        assertEquals(listOf(keepA, keepB), res)
    }
}