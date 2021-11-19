package xyz.nygaard.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.nygaard.core.PriceStrategy

internal class PriceStrategyTest {

    private val priceStrategy = PriceStrategy(minAskSpread = 1.5, minBidSpread = 0.75)

    @Test
    fun minAsk() {
        assertEquals(150.0, priceStrategy.minAsk(100.0))
    }

    @Test
    fun maxBid() {
        assertEquals(75.0, priceStrategy.maxBid(100.0))
    }

    @Test
    fun `illegal strategies`() {
        assertThrows<IllegalArgumentException> { PriceStrategy(minAskSpread = 0.78) }
        assertThrows<IllegalArgumentException> { PriceStrategy(minBidSpread = 1.20) }
    }

    @Test
    fun `ask price high spread`() {
        val ticker = MarketTicker(100.0, 200.0)
        assertEquals(200.0, priceStrategy.askPrice(ticker))
    }

    @Test
    fun `ask price low spread`() {
        val ticker = MarketTicker(100.0, 101.0)
        assertEquals(150.0, priceStrategy.askPrice(ticker))
    }

    @Test
    fun `bid price high spread`() {
        val ticker = MarketTicker(100.0, 200.0)
        assertEquals(100.0, priceStrategy.bidPrice(ticker))
    }

    @Test
    fun `bid price low spread`() {
        val ticker = MarketTicker(100.0, 101.0)
        assertEquals(75.75, priceStrategy.bidPrice(ticker))
    }

    @Test
    fun `created ask is not outOfSync`() {
        val master = PriceStrategy(minSpread = 0.013)
        val ticker = MarketTicker(521780.1, 525920.46)
        assertFalse(master.outOfSync(activeOrder(ActiveOrder.OrderType.ask, 528563.24), ticker))
    }

    @Test
    fun `created ask is not outOfSync 2`() {
        val master = PriceStrategy(minSpread = 0.013)
        val ticker = MarketTicker(521326.69, 527304.33)
        assertFalse(master.outOfSync(activeOrder(ActiveOrder.OrderType.ask, 528102.9), ticker))
    }

    @Test
    fun `created bid is not outOfSync 1`() {
        val master = PriceStrategy(minSpread = 0.013)
        val ticker = MarketTicker(521326.69, 527304.33)
        assertFalse(master.outOfSync(activeOrder(ActiveOrder.OrderType.bid, 520449.37), ticker))
    }
}