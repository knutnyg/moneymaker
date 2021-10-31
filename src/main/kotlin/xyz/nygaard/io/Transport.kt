package xyz.nygaard.io

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

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
            OrderType.bid -> this.price < (marketTicker.bid * 0.998)
            OrderType.ask -> this.price > (marketTicker.ask * 1.002)
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

    fun maxBid(): Double = (BigDecimal(ask) * BigDecimal(0.989)).setScale(2, RoundingMode.HALF_UP).toDouble()
    fun minAsk(): Double = (BigDecimal(bid) * BigDecimal(1.011)).setScale(2, RoundingMode.HALF_UP).toDouble()

    override fun toString(): String {
        return "MarketTick BTCNOK: bid: $bid NOK, ask: $ask NOK, spread: $spread NOK(${spreadAsPercentage()}%)"
    }
}