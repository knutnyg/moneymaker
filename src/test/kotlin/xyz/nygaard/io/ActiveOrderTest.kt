package xyz.nygaard.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

internal class ActiveOrderTest {

    val marketBigSpread = MarketTicker(700.0, 1000.0)
    val marketSmallSpread = MarketTicker(10000.0, 10001.0)
    val activeBid1000 = ActiveOrder(1, Market.BTCNOK, ActiveOrder.OrderType.bid, 1000.0, 1.0, 1.0, 1.0, 0.0, Instant.now())

    @Test
    fun `price ask with large spread`() {
        assertEquals(1000.0, marketBigSpread.askPrice(BID_SCALAR))
    }

    @Test
    fun `price ask with small spread`() {
        assertEquals(11000.0, marketSmallSpread.askPrice(BID_SCALAR))
    }

    @Test
    fun `price bid with large spread`() {
        assertEquals(700.0, marketBigSpread.bidPrice(ASK_SCALAR))
    }

    @Test
    fun `price bid with small spread`() {
        assertEquals(9000.9, marketSmallSpread.bidPrice(ASK_SCALAR))
    }

    private val ASK_SCALAR = BigDecimal(0.9)
    private val BID_SCALAR = BigDecimal(1.1)
}

