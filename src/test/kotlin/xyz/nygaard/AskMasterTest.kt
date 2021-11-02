package xyz.nygaard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.ActiveOrder.OrderType.ask
import xyz.nygaard.io.Market
import xyz.nygaard.io.MarketTicker
import java.time.LocalDateTime

internal class AskMasterTest {

    @Test
    fun `no current order - low spread`() {
        val tick = MarketTicker(bid = 999.0, ask = 1000.0)
        val activeAsks = emptyList<ActiveOrder>()

        val req = CreateOrderRequest(
            ask,
            price = 1011.99,
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
        val tick = MarketTicker(bid = 900.0, ask = 1000.0)
        val activeAsks = listOf(activeAsk(1001.0))

        val actions = AskMaster(activeAsks, tick).execute()
        val req = CreateOrderRequest(
            ask,
            price = 1001.0,
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
            ClearOrders(orderType = ask),
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
            ClearOrders(orderType = ask),
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
        created_at = LocalDateTime.now()
    )
}