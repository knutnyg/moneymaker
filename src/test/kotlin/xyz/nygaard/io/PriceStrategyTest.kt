package xyz.nygaard.io

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.nygaard.core.strategy.PriceStrategy
import java.time.Instant

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

    private val driftPriceStrategy = PriceStrategy(
        maxAskDrift = 1.003,
        maxBidDrift = 0.997
    )

    @Test
    fun `ask order equal to ticker ask`() {
        assertFalse(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.ask, 1000.0),
                MarketTicker(700.0, 1000.0)
            )
        )
    }

    @Test
    fun `bid order equal to ticker bid`() {
        assertFalse(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.bid, 700.0),
                MarketTicker(700.0, 1000.0)
            )
        )
    }

    @Test
    fun `ask just below ticker ask`() {
        Assertions.assertTrue(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.ask, 999.0),
                MarketTicker(700.0, 1000.0)
            )
        )
    }

    @Test
    fun `ask just above ticker ask`() {
        assertFalse(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.ask, 1001.0),
                MarketTicker(700.0, 1000.0)
            )
        )
    }

    @Test
    fun `ask far above ticker bid`() {
        Assertions.assertTrue(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.ask, 1100.0),
                MarketTicker(700.0, 1000.0)
            )
        )
    }

    @Test
    fun `bid just above ticker bid`() {
        Assertions.assertTrue(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.bid, 701.0),
                MarketTicker(700.0, 1000.0)
            )
        )
    }

    @Test
    fun `bid just below ticker bid`() {
        assertFalse(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.bid, 699.0),
                MarketTicker(700.0, 1000.0)
            )
        )
    }

    @Test
    fun `bid far below ticker bid`() {
        Assertions.assertTrue(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.bid, 600.0),
                MarketTicker(700.0, 1000.0)
            )
        )
    }

    @Test
    fun `bid far below ticker bid with low spread`() {
        assertFalse(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.bid, 529000.0),
                MarketTicker(
                    535000.0,
                    536000.0
                )
            )
        )
    }

    @Test
    fun `ask far above ticker ask with low spread`() {
        assertFalse(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.ask, 542000.0),
                MarketTicker(
                    535000.0,
                    536000.0
                )
            )
        )
    }

    @Test
    fun `bid far below low spread`() {
        Assertions.assertTrue(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.bid, 850.0),
                MarketTicker(990.0, 1000.0)
            )
        )
    }

    @Test
    fun `bid close to bid high spread under limit`() {
        assertFalse(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.bid, 529000.0),
                MarketTicker(530000.0, 540000.0)
            )
        )
    }

    @Test
    fun `bid close to bid high spread over limit`() {
        Assertions.assertTrue(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.bid, 528000.0),
                MarketTicker(530000.0, 540000.0)
            )
        )
    }

    @Test
    fun `ask close to ask high spread under limit`() {
        assertFalse(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.ask, 540500.0),
                MarketTicker(530000.0, 540000.0)
            )
        )
    }

    @Test
    fun `ask close to ask high spread over limit`() {
        Assertions.assertTrue(
            driftPriceStrategy.outOfSync(
                activeOrder(ActiveOrder.OrderType.ask, 542000.0),
                MarketTicker(530000.0, 540000.0)
            )
        )
    }
}

fun activeOrder(type: ActiveOrder.OrderType, price: Double) =
    ActiveOrder(1, Market.BTCNOK, type, price, 1.0, 1.0, 1.0, 0.0, Instant.now())