package xyz.nygaard.io

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xyz.nygaard.core.PriceStrategy
import java.time.Instant

internal class ActiveOrderTest {

    private val priceStrategy = PriceStrategy(
        maxAskDrift = 1.003,
        maxBidDrift = 0.997
    )

    @Test
    fun `ask order equal to ticker ask`() {
        assertFalse(
            activeOrder(ActiveOrder.OrderType.ask, 1000.0).outOfSync(
                MarketTicker(700.0, 1000.0),
                priceStrategy
            )
        )
    }

    @Test
    fun `bid order equal to ticker bid`() {
        assertFalse(activeOrder(ActiveOrder.OrderType.bid, 700.0).outOfSync(MarketTicker(700.0, 1000.0), priceStrategy))
    }

    @Test
    fun `ask just below ticker ask`() {
        assertTrue(activeOrder(ActiveOrder.OrderType.ask, 999.0).outOfSync(MarketTicker(700.0, 1000.0), priceStrategy))
    }

    @Test
    fun `ask just above ticker ask`() {
        assertFalse(
            activeOrder(ActiveOrder.OrderType.ask, 1001.0).outOfSync(
                MarketTicker(700.0, 1000.0),
                priceStrategy
            )
        )
    }

    @Test
    fun `ask far above ticker bid`() {
        assertTrue(activeOrder(ActiveOrder.OrderType.ask, 1100.0).outOfSync(MarketTicker(700.0, 1000.0), priceStrategy))
    }

    @Test
    fun `bid just above ticker bid`() {
        assertTrue(activeOrder(ActiveOrder.OrderType.bid, 701.0).outOfSync(MarketTicker(700.0, 1000.0), priceStrategy))
    }

    @Test
    fun `bid just below ticker bid`() {
        assertFalse(activeOrder(ActiveOrder.OrderType.bid, 699.0).outOfSync(MarketTicker(700.0, 1000.0), priceStrategy))
    }

    @Test
    fun `bid far below ticker bid`() {
        assertTrue(activeOrder(ActiveOrder.OrderType.bid, 600.0).outOfSync(MarketTicker(700.0, 1000.0), priceStrategy))
    }

    @Test
    fun `bid far below ticker bid with low spread`() {
        assertFalse(
            activeOrder(ActiveOrder.OrderType.bid, 529000.0).outOfSync(
                MarketTicker(
                    535000.0,
                    536000.0
                )
            )
        ) // 0.5% spre,priceStrategyad
    }

    @Test
    fun `ask far above ticker ask with low spread`() {
        assertFalse(
            activeOrder(ActiveOrder.OrderType.ask, 542000.0).outOfSync(
                MarketTicker(
                    535000.0,
                    536000.0
                )
            )
        ) // 0.5% spre,priceStrategyad
    }

    @Test
    fun `bid far below low spread`() {
        assertTrue(activeOrder(ActiveOrder.OrderType.bid, 850.0).outOfSync(MarketTicker(990.0, 1000.0), priceStrategy))
    }

    @Test
    fun `bid close to bid high spread under limit`() {
        assertFalse(
            activeOrder(ActiveOrder.OrderType.bid, 529000.0).outOfSync(
                MarketTicker(530000.0, 540000.0),
                priceStrategy
            )
        )
    }

    @Test
    fun `bid close to bid high spread over limit`() {
        assertTrue(
            activeOrder(ActiveOrder.OrderType.bid, 528000.0).outOfSync(
                MarketTicker(530000.0, 540000.0),
                priceStrategy
            )
        )
    }

    @Test
    fun `ask close to ask high spread under limit`() {
        assertFalse(
            activeOrder(ActiveOrder.OrderType.ask, 540500.0).outOfSync(
                MarketTicker(530000.0, 540000.0),
                priceStrategy
            )
        )
    }

    @Test
    fun `ask close to ask high spread over limit`() {
        assertTrue(
            activeOrder(ActiveOrder.OrderType.ask, 542000.0).outOfSync(
                MarketTicker(530000.0, 540000.0),
                priceStrategy
            )
        )
    }

    private fun activeOrder(type: ActiveOrder.OrderType, price: Double) =
        ActiveOrder(1, Market.BTCNOK, type, price, 1.0, 1.0, 1.0, 0.0, Instant.now())
}