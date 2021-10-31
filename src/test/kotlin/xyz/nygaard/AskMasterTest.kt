package xyz.nygaard

import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.Market
import xyz.nygaard.io.MarketTicker
import java.time.LocalDateTime

internal class AskMasterTest {

    private val firiClientMock = mockk<FiriClient>(relaxed = true)

    @Test
    fun `no current order - low spread`() {
        val tick = MarketTicker(bid = 999.0, ask = 1000.0)
        val activeAsks = emptyList<ActiveOrder>()

        AskMaster(activeAsks, tick, firiClientMock).execute()

        coVerify(exactly = 0 ) { firiClientMock.deleteActiveOrders() }
        coVerify(exactly = 1) { firiClientMock.placeAsk(1011.99) }
    }

    @Test
    fun `no current order - high spread`() {
        val tick = MarketTicker(bid = 900.0, ask = 1000.0)
        val activeAsks = emptyList<ActiveOrder>()

        AskMaster(activeAsks, tick, firiClientMock).execute()

        coVerify(exactly = 0 ) { firiClientMock.deleteActiveOrders() }
        coVerify(exactly = 1) { firiClientMock.placeAsk(1000.0) }
    }

    @Test
    fun `active ask within bounds`() {
        val tick = MarketTicker(bid = 900.0, ask = 1000.0)
        val activeAsks = listOf(activeAsk(1001.0))

        AskMaster(activeAsks, tick, firiClientMock).execute()

        coVerify(exactly = 0) { firiClientMock.deleteActiveOrders() }
        coVerify(exactly = 0) { firiClientMock.placeAsk(any()) }
    }

    @Test
    fun `active bid that is now too low`() {
        val tick = MarketTicker(bid = 90.0, ask = 110.0)
        val activeAsks = listOf(activeAsk(91.0))

        AskMaster(activeAsks, tick, firiClientMock).execute()

        coVerify(exactly = 1) { firiClientMock.deleteActiveOrders() }
        coVerify(exactly = 1) { firiClientMock.placeAsk(110.0) }
    }

    @Test
    fun `move ask closer as spread has increased`() {
        val tick = MarketTicker(bid = 510000.0, ask = 520000.0)
        val activeAsks = listOf(activeAsk(530000.0))

        AskMaster(activeAsks, tick, firiClientMock).execute()

        coVerify(exactly = 1) { firiClientMock.deleteActiveOrders() }
        coVerify(exactly = 1) { firiClientMock.placeAsk(520000.0) }
    }

    private fun activeAsk(price: Double) = ActiveOrder(
        id = 123,
        market = Market.BTCNOK,
        type = ActiveOrder.OrderType.ask,
        price = price,
        remaining = 1.0,
        amount = 1.0,
        matched = 0.0,
        cancelled = 0.0,
        created_at = LocalDateTime.now()
    )
}