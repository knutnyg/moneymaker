package xyz.nygaard.core

import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.MarketTicker
import java.math.RoundingMode
import kotlin.math.max

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

    fun askPrice(marketTicker: MarketTicker) = max(minAsk(marketTicker.bid), marketTicker.ask)

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

    fun allValid(activeOrders: List<ActiveOrder>, marketTicker: MarketTicker): Boolean {
        return activeOrders.all { isValid(it, marketTicker)} && activeOrders.none {
            outOfSync(it, marketTicker)
        }
    }

    fun isValid(activeOrder: ActiveOrder, marketTicker: MarketTicker): Boolean {
        return when (activeOrder.type) {
            ActiveOrder.OrderType.bid -> activeOrder.price <= maxBid(marketTicker.ask)
            ActiveOrder.OrderType.ask -> activeOrder.price >= minAsk(marketTicker.bid)
        }
    }
}