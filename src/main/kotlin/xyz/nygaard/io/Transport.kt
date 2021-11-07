package xyz.nygaard.io

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
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
    val created_at: Instant
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

class PriceStrategy(
    val minAskSpread: Double = 1.012,
    val minBidSpread: Double = 0.988
)

data class MarketTicker(
    val bid: Double,
    val ask: Double,
    val spread: Double = (ask - bid),
    val priceStrategy: PriceStrategy = PriceStrategy()
) {

    private fun spreadAsPercentage() = BigDecimal(spread / ((ask + bid) / 2) * 100).setScale(2, RoundingMode.HALF_UP)

    internal fun minAsk(): Double =
        (bid.toBigDecimal() * priceStrategy.minAskSpread.toBigDecimal()).setScale(2, RoundingMode.HALF_UP).toDouble()

    internal fun maxBid(): Double =
        (ask.toBigDecimal() * (priceStrategy.minBidSpread).toBigDecimal()).setScale(2, RoundingMode.HALF_UP)
            .toDouble()

    fun askPrice() = max(minAsk(), ask)
    fun bidPrice() = min(maxBid(), bid)

    override fun toString(): String {
        return "MarketTick BTCNOK: bid: $bid NOK, ask: $ask NOK, spread: $spread NOK(${spreadAsPercentage()}%)"
    }
}