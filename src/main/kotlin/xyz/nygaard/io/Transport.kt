package xyz.nygaard.io

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

data class OrderRequest(
    val market: String = "BTCNOK",
    val type: String,
    val price: String,
    val amount: String
)

data class OrderResponse(
    val id: Int
)

data class ActiveOrder(
    val id: Int,
    val market: Market,
    val type: OrderType,
    val price: Double,
    val remaining: Double,
    val amount: Double,
    val matched: Double,
    val cancelled: Double,
    val created_at: LocalDateTime
) {
    enum class OrderType { bid, ask }

    fun valid(marketTicker: MarketTicker): Boolean {
        return when (type) {
            OrderType.bid -> this.price <= marketTicker.maxBid()
            OrderType.ask -> this.price >= marketTicker.minAsk()
        }
    }

    fun outOfSync(marketTicker: MarketTicker): Boolean {
        return when (type) {
            OrderType.bid -> this.price < (marketTicker.bid * 0.9995) // TODO: Må ta hensyn til spread
            OrderType.ask -> this.price > (marketTicker.ask * 1.0001) // TODO: Må ta hensyn til spread
        }
    }
}

enum class Market { BTCNOK }

data class MarketTicker(
    val bid: Double,
    val ask: Double,
    val spread: Double = (ask - bid)
) {

    private fun spreadAsPercentage() = BigDecimal(spread / ((ask + bid) / 2) * 100).setScale(2, RoundingMode.HALF_UP)

    internal fun minAsk(scalar: BigDecimal = BID_SCALAR): Double = (BigDecimal(bid) * scalar).setScale(2, RoundingMode.HALF_UP).toDouble()
    internal fun maxBid(scalar: BigDecimal = ASK_SCALAR): Double = (BigDecimal(ask) * scalar).setScale(2, RoundingMode.HALF_UP).toDouble()

    fun askPrice(scalar: BigDecimal = BID_SCALAR) = max(minAsk(scalar), ask)
    fun bidPrice(scalar: BigDecimal = ASK_SCALAR) = min(maxBid(scalar), bid)

    override fun toString(): String {
        return "MarketTick BTCNOK: bid: $bid NOK, ask: $ask NOK, spread: $spread NOK(${spreadAsPercentage()}%)"
    }
}

val ASK_SCALAR = BigDecimal(0.9855)
val BID_SCALAR = BigDecimal(1.015)