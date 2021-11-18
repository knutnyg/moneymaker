package xyz.nygaard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.ActiveOrder.OrderType.ask
import xyz.nygaard.core.CreateOrderRequest
import xyz.nygaard.io.Market
import xyz.nygaard.io.MarketTicker
import xyz.nygaard.core.PriceStrategy
import java.time.Instant

internal class AskMasterTest {

    @Test
    fun `no current order - low spread`() {
        val tick = MarketTicker(bid = 999.0, ask = 1000.0)
        val activeAsks = emptyList<ActiveOrder>()

        val req = CreateOrderRequest(
            ask,
            price = PriceStrategy().askPrice(tick),
            amount = 0.0001,
        )

        val actions = AskMaster(activeAsks, tick).execute()
        assertThat(actions).containsExactly(
            AddAsk(req = req)
        )
    }

    @Test
    fun `no current order - high spread`() {
        val tick = MarketTicker(bid = 900.0, ask = 1000.0)
        val activeAsks = emptyList<ActiveOrder>()

        val actions = AskMaster(activeAsks, tick).execute()
        val req = CreateOrderRequest(
            ask,
            price = 1000.0,
            amount = 0.0001,
        )
        assertThat(actions).containsExactly(
            AddAsk(req = req)
        )
    }

    @Test
    fun `active ask within bounds`() {
        val tick = MarketTicker(bid = 500000.0, ask = 550000.0)
        val activeAsks = listOf(activeAsk(550001.0))

        val actions = AskMaster(activeAsks, tick).execute()
        val req = CreateOrderRequest(
            ask,
            price = 550001.0,
            amount = 0.0001,
        )
        assertThat(actions).containsExactly(
            KeepAsk(req = req),
        )
    }

    @Test
    fun `active bid that is now too low`() {
        val tick = MarketTicker(bid = 90.0, ask = 110.0)
        val activeAsks = listOf(activeAsk(91.0))

        val actions = AskMaster(activeAsks, tick).execute()
        val req = CreateOrderRequest(
            ask,
            price = 110.0,
            amount = 0.0001,
        )
        assertThat(actions).containsExactly(
            ClearOrders,
            AddAsk(req = req),
        )

    }

    @Test
    fun `move ask closer as spread has increased`() {
        val tick = MarketTicker(bid = 510000.0, ask = 520000.0)
        val activeAsks = listOf(activeAsk(530000.0))

        val actions = AskMaster(activeAsks, tick).execute()
        val req = CreateOrderRequest(
            ask,
            price = 520000.0,
            amount = 0.0001,
        )

        assertThat(actions).containsExactly(
            ClearOrders,
            AddAsk(req = req),
        )
    }

    private fun activeAsk(price: Double) = ActiveOrder(
        id = 123,
        market = Market.BTCNOK,
        type = ask,
        price = price,
        remaining = 1.0,
        amount = 1.0,
        matched = 0.0,
        cancelled = 0.0,
        created_at = Instant.now()
    )
}