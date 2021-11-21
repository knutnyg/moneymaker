package xyz.nygaard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.core.CreateOrderRequest
import xyz.nygaard.core.PriceStrategy
import xyz.nygaard.io.Market
import xyz.nygaard.io.MarketTicker
import java.time.Instant

internal class BidMasterTest {

    @Test
    fun `no current order - low spread`() {
        val tick = MarketTicker(bid = 999.0, ask = 1000.0)
        val activeBids = emptyList<ActiveOrder>()

        val actions = BidMaster(activeBids, tick).execute()
        val req = CreateOrderRequest(
            type = ActiveOrder.OrderType.bid,
            amount = 0.0001,
            price = PriceStrategy().bidPrice(tick),
        )
        assertThat(actions).containsExactly(
            AddBid(req = req),
        )
    }

    @Test
    fun `no current order - high spread`() {
        val tick = MarketTicker(bid = 900.0, ask = 1000.0)
        val activeBids = emptyList<ActiveOrder>()

        val actions = BidMaster(activeBids, tick).execute()

        val req = CreateOrderRequest(
            type = ActiveOrder.OrderType.bid,
            amount = 0.0001,
            price = 900.0,
        )
        assertThat(actions).containsExactly(
            AddBid(req = req),
        )
    }

    @Test
    fun `active bid within bounds`() {
        val tick = MarketTicker(bid = 500000.0, ask = 550000.0)
        val activeBids = listOf(activeBid(499999.0))

        val actions = BidMaster(activeBids, tick).execute()
        val req = CreateOrderRequest(
            ActiveOrder.OrderType.bid,
            price = 499999.0,
            amount = 0.0001,
        )
        assertThat(actions).containsExactly(
            KeepBid(req = req),
        )
    }

    @Test
    fun `active bid that is now too high`() {
        val tick = MarketTicker(bid = 90.0, ask = 110.0)
        val activeBids = listOf(activeBid(109.0))

        val actions = BidMaster(activeBids, tick).execute()

        val req = CreateOrderRequest(
            type = ActiveOrder.OrderType.bid,
            amount = 0.0001,
            price = 90.0,
        )

        assertThat(actions).containsExactly(
            ClearOrders(),
            AddBid(req = req),
        )
    }

    @Test
    fun `move bid closer`() {
        val tick = MarketTicker(bid = 990.0, ask = 1000.0)
        val activeBids = listOf(activeBid(850.0))

        val actions = BidMaster(activeBids, tick).execute()
        val req = CreateOrderRequest(
            type = ActiveOrder.OrderType.bid,
            amount = 0.0001,
            price = 987.0,
        )

        assertThat(actions).containsExactly(
            ClearOrders(),
            AddBid(req = req),
        )
    }

    private fun activeBid(price: Double) = ActiveOrder(
        id = 123,
        market = Market.BTCNOK,
        type = ActiveOrder.OrderType.bid,
        price = price,
        remaining = 1.0,
        amount = 1.0,
        matched = 0.0,
        cancelled = 0.0,
        created_at = Instant.now()
    )
}