package xyz.nygaard

import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.Market
import xyz.nygaard.io.MarketTicker
import java.time.LocalDateTime

internal class BidMasterTest {

    private val firiClientMock = mockk<FiriClient>(relaxed = true)

    @Test
    fun `no current order - low spread`() {
        val tick = MarketTicker(bid = 999.0, ask = 1000.0)
        val activeBids = emptyList<ActiveOrder>()

        BidMaster(activeBids, tick, firiClientMock).execute()

        coVerify(exactly = 0 ) { firiClientMock.deleteActiveOrders() }
        coVerify(exactly = 1) { firiClientMock.placeBid(989.0) }
    }

    @Test
    fun `no current order - high spread`() {
        val tick = MarketTicker(bid = 900.0, ask = 1000.0)
        val activeBids = emptyList<ActiveOrder>()

        BidMaster(activeBids, tick, firiClientMock).execute()

        coVerify(exactly = 0 ) { firiClientMock.deleteActiveOrders() }
        coVerify(exactly = 1) { firiClientMock.placeBid(900.0) }
    }

    @Test
    fun `active bid within bounds`() {
        val tick = MarketTicker(bid = 900.0, ask = 1000.0)
        val activeBids = listOf(activeBid(899.0))

        BidMaster(activeBids, tick, firiClientMock).execute()

        coVerify(exactly = 0) { firiClientMock.deleteActiveOrders() }
        coVerify(exactly = 0) { firiClientMock.placeBid(90.0) }
    }

    @Test
    fun `active bid that is now too high`() {
        val tick = MarketTicker(bid = 90.0, ask = 110.0)
        val activeBids = listOf(activeBid(109.0))

        BidMaster(activeBids, tick, firiClientMock).execute()

        coVerify(exactly = 1) { firiClientMock.deleteActiveOrders() }
        coVerify(exactly = 1) { firiClientMock.placeBid(90.0) }
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