package xyz.nygaard.io

import xyz.nygaard.core.PriceStrategy
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
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
    val created_at: Instant
) {
    enum class OrderType { bid, ask }

    @Deprecated("use PriceStrategy", ReplaceWith("priceStrategy.isValid(this, marketTicker)"), )
    fun valid(marketTicker: MarketTicker, priceStrategy: PriceStrategy = PriceStrategy()): Boolean = priceStrategy.isValid(this, marketTicker)

    @Deprecated("use PriceStrategy", ReplaceWith("priceStrategy.outOfSync(this, marketTicker)"))
    fun outOfSync(marketTicker: MarketTicker, priceStrategy: PriceStrategy = PriceStrategy()) = priceStrategy.outOfSync(this, marketTicker)
}

enum class Market { BTCNOK }

data class MarketTicker(
    val bid: Double,
    val ask: Double,
    val spread: Double = (ask - bid),
    val priceStrategy: PriceStrategy = PriceStrategy()
) {

    internal fun spreadAsPercentage() = BigDecimal(spread / ((ask + bid) / 2) * 100).setScale(2, RoundingMode.HALF_UP)

    override fun toString(): String {
        return "MarketTick BTCNOK: bid: $bid NOK, ask: $ask NOK, spread: $spread NOK(${spreadAsPercentage()}%)"
    }
}