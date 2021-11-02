package xyz.nygaard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.Market
import xyz.nygaard.io.MarketTicker
import java.time.LocalDateTime

internal class BidMasterTest {

    @Test
    fun `no current order - low spread`() {
        val tick = MarketTicker(bid = 999.0, ask = 1000.0)
        val activeBids = emptyList<ActiveOrder>()

        val actions = BidMaster(activeBids, tick).execute()
        val req = CreateOrderRequest(
            type = ActiveOrder.OrderType.bid,
            amount = 0.0001,
            price = 989.0,
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
        val tick = MarketTicker(bid = 900.0, ask = 1000.0)
        val activeBids = listOf(activeBid(899.0))

        val actions = BidMaster(activeBids, tick).execute()
        val req = CreateOrderRequest(
            ActiveOrder.OrderType.bid,
            price = 899.0,
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
            ClearOrders(ActiveOrder.OrderType.bid),
            AddBid(req = req),
        )
    }

    @Test
    fun `move bid closer as spread has increased`() {
        val tick = MarketTicker(bid = 513015.3, ask = 518469.83)
        val activeBids = listOf(activeBid(510608.01))

        val actions = BidMaster(activeBids, tick).execute()
        val req = CreateOrderRequest(
            type = ActiveOrder.OrderType.bid,
            amount = 0.0001,
            price = 512766.66,
        )

        assertThat(actions).containsExactly(
            ClearOrders(ActiveOrder.OrderType.bid),
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
        created_at = LocalDateTime.now()
    )
}