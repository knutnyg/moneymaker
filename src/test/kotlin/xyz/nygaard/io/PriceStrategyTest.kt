package xyz.nygaard.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
}