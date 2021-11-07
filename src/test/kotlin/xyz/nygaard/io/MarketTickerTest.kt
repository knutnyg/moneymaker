package xyz.nygaard.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MarketTickerTest {

    @Test
    fun `ask price high spread`() {
        val ticker = MarketTicker(100.0, 200.0)
        assertEquals(200.0, ticker.askPrice())
    }

    @Test
    fun `bid price high spread`() {
        val ticker = MarketTicker(100.0, 200.0)
        assertEquals(100.0, ticker.bidPrice())
    }

    @Test
    fun `ask price low spread`() {
        val ticker = MarketTicker(100.0, 101.0)
        assertEquals(101.2, ticker.askPrice())
    }

    @Test
    fun `bid price low spread`() {
        val ticker = MarketTicker(100.0, 101.0)
        assertEquals(99.79, ticker.bidPrice())
    }
}