package xyz.nygaard.io

import round
import xyz.nygaard.core.PriceStrategy
import xyz.nygaard.io.ActiveOrder.OrderType.ask
import xyz.nygaard.io.ActiveOrder.OrderType.bid
import xyz.nygaard.log
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class OrderResponse(
    val id: Int
)

const val DAY = 60 * 60 * 24

data class ActiveOrder(
    val id: Int,
    val market: Market = Market.BTCNOK,
    val type: OrderType,
    val price: Double,
    val remaining: Double,
    val amount: Double,
    val matched: Double,
    val cancelled: Double,
    val created_at: Instant
) {
    enum class OrderType { bid, ask }

    companion object {
        fun List<ActiveOrder>.createReport(cutoff:Instant = Instant.now().minusSeconds(2L * DAY)) {

            val relevantOrders = this.filter { it.created_at > cutoff }.filter { it.amount == 0.0001 }
            val (bids, asks) = relevantOrders.partition { it.type == bid }

            val bidSumPaydNOK = bids.sumOf { it.matched * it.price }.round(2)
            val bidSumBTCBought = bids.sumOf { it.matched }.round(6)
            val askSumPaydBTC = asks.sumOf { it.matched * it.price }.round(2)
            val askSumBTCSold = asks.sumOf { it.matched }.round(6)

            log.info("Last 48h we matched ${bids.size} bids and bought $bidSumBTCBought BTC for $bidSumPaydNOK NOK")
            log.info("Last 48h we matched ${asks.size} asks and sold $askSumBTCSold BTC for $askSumPaydBTC NOK")

            // Prevent dividing by zero
            if (bids.isEmpty() || asks.isEmpty()) return

            val avgBuy = (bids.sumOf { it.price * it.matched } / bids.size).round(2)
            val avgSale = (asks.sumOf { it.price * it.matched } / asks.size).round(2)
            val fees = ((askSumPaydBTC + bidSumPaydNOK) * 0.01).round(2)
            val avgFees = (fees / (bids.size + asks.size)).round(2)

            log.info("$: Avg buy: ${avgBuy}, avg sale: $avgSale, avg fees: $avgFees")
            log.info("$: We are on average earning ${(avgSale - avgBuy - avgFees).round(2)} per trade! 💸")
        }
    }
}

enum class Market { BTCNOK }

data class MarketStrategy (
    val market: MarketTicker,
    val priceStrategy: PriceStrategy = PriceStrategy(),
) {
    override fun toString(): String {
        return "MarketStrategy BTCNOK: bid: ${market.bid} NOK, ask: ${market.ask} NOK, spread: ${market.spread} NOK(${market.spreadAsPercentage()}%)"
    }
//    internal fun spreadAsPercentage() = BigDecimal(spread / ((ask + bid) / 2) * 100).setScale(2, RoundingMode.HALF_UP)
}

data class MarketTicker(
    val bid: Double,
    val ask: Double,
    val spread: Double = (ask - bid),
) {
    fun spreadAsPercentage(): BigDecimal = BigDecimal(spread / ((ask + bid) / 2) * 100).setScale(2, RoundingMode.HALF_UP)

    override fun toString(): String {
        return "MarketTick BTCNOK: bid: $bid NOK, ask: $ask NOK, spread: $spread NOK(${spreadAsPercentage()}%)"
    }
}
