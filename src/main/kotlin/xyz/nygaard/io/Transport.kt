package xyz.nygaard.io

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

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
    val created_at: Instant,
    val priceStrategy: PriceStrategy = PriceStrategy()
) {
    enum class OrderType { bid, ask }

    fun valid(marketTicker: MarketTicker): Boolean {
        return when (type) {
            OrderType.bid -> this.price <= marketTicker.maxBid()
            OrderType.ask -> this.price >= marketTicker.minAsk()
        }
    }

    fun outOfSync(marketTicker: MarketTicker): Boolean {
        return priceStrategy.outOfSync(this, marketTicker)
    }
}

enum class Market { BTCNOK }

class PriceStrategy(
    private val minAskSpread: Double = 1.012,
    private val minBidSpread: Double = 0.988
) {

    init {
        require(minBidSpread <= 1.00) { "require a maximum of 1.00 spread for our bids" }
        require(minAskSpread >= 1.00) { "require a minimum of 1.00 spread for our asks" }
    }

    internal fun minAsk(bid: Double): Double =
        (bid.toBigDecimal() * minAskSpread.toBigDecimal()).setScale(2, RoundingMode.HALF_UP).toDouble()

    internal fun maxBid(ask: Double): Double =
        (ask.toBigDecimal() * (minBidSpread).toBigDecimal()).setScale(2, RoundingMode.HALF_UP).toDouble()

    internal fun outOfSync(activeOrder: ActiveOrder, marketTicker: MarketTicker): Boolean {
        return if (marketTicker.spreadAsPercentage().toDouble() < 1.012) {
            // If spread is very low allow orders as long as they keep the minimum spread
            when (activeOrder.type) {
                ActiveOrder.OrderType.bid -> activeOrder.price > marketTicker.bid || (activeOrder.price < (marketTicker.ask * 0.985) || activeOrder.price > marketTicker.ask * 0.988)
                ActiveOrder.OrderType.ask -> activeOrder.price < marketTicker.ask || (activeOrder.price < (marketTicker.bid * 1.012) ||  activeOrder.price > (marketTicker.bid * 1.015))
            }
        } else {
            // If spread larger always follow bid/ask
            when (activeOrder.type) {
                ActiveOrder.OrderType.bid -> activeOrder.price > marketTicker.bid || activeOrder.price < (marketTicker.bid * 0.997)
                ActiveOrder.OrderType.ask -> activeOrder.price < marketTicker.ask || activeOrder.price > (marketTicker.ask * 1.003)
            }
        }
    }
}

data class MarketTicker(
    val bid: Double,
    val ask: Double,
    val spread: Double = (ask - bid),
    val priceStrategy: PriceStrategy = PriceStrategy()
) {

    internal fun spreadAsPercentage() = BigDecimal(spread / ((ask + bid) / 2) * 100).setScale(2, RoundingMode.HALF_UP)

    internal fun minAsk(): Double = priceStrategy.minAsk(bid)
    internal fun maxBid(): Double = priceStrategy.maxBid(ask)

    fun askPrice() = max(minAsk(), ask)
    fun bidPrice() = min(maxBid(), bid)

    override fun toString(): String {
        return "MarketTick BTCNOK: bid: $bid NOK, ask: $ask NOK, spread: $spread NOK(${spreadAsPercentage()}%)"
    }
}