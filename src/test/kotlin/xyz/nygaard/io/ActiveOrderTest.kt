package xyz.nygaard.io

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

internal class ActiveOrderTest {

    @Test
    fun `ask order equal to ticker ask`() {
        assertFalse(activeOrder(ActiveOrder.OrderType.ask, 1000.0).outOfSync(MarketTicker(700.0, 1000.0)))
    }

    @Test
    fun `bid order equal to ticker bid`() {
        assertFalse(activeOrder(ActiveOrder.OrderType.bid, 700.0).outOfSync(MarketTicker(700.0, 1000.0)))
    }

    @Test
    fun `ask just below ticker ask`() {
        assertTrue(activeOrder(ActiveOrder.OrderType.ask, 999.0).outOfSync(MarketTicker(700.0, 1000.0)))
    }

    @Test
    fun `ask just above ticker ask`() {
        assertFalse(activeOrder(ActiveOrder.OrderType.ask, 1001.0).outOfSync(MarketTicker(700.0, 1000.0)))
    }

    @Test
    fun `ask far above ticker bid`() {
        assertTrue(activeOrder(ActiveOrder.OrderType.ask, 1100.0).outOfSync(MarketTicker(700.0, 1000.0)))
    }

    @Test
    fun `bid just above ticker bid`() {
        assertTrue(activeOrder(ActiveOrder.OrderType.bid, 701.0).outOfSync(MarketTicker(700.0, 1000.0)))
    }

    @Test
    fun `bid just below ticker bid`() {
        assertFalse(activeOrder(ActiveOrder.OrderType.bid, 699.0).outOfSync(MarketTicker(700.0, 1000.0)))
    }

    @Test
    fun `bid far below ticker bid`() {
        assertTrue(activeOrder(ActiveOrder.OrderType.bid, 600.0).outOfSync(MarketTicker(700.0, 1000.0)))
    }

    private fun activeOrder(type: ActiveOrder.OrderType, price: Double) =
        ActiveOrder(1, Market.BTCNOK, type, price, 1.0, 1.0, 1.0, 0.0, Instant.now())
}