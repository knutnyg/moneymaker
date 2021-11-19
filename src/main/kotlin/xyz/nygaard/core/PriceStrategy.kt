package xyz.nygaard.core

import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.MarketTicker
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min

class PriceStrategy(
    private val minSpread: Double = 0.013,
    private val minAskSpread: Double = 1.0 + minSpread,
    private val minBidSpread: Double = 1.0 - minSpread,
    private val maxBidDrift: Double = 0.999997,
    private val maxAskDrift: Double = 1.000003
) {

    init {
        require(minBidSpread <= 1.00) { "require a maximum of 1.00 spread for our bids" }
        require(maxBidDrift <= 1.00) { "require a maximum of 1.00 for our bid drift" }
        require(minAskSpread >= 1.00) { "require a minimum of 1.00 spread for our asks" }
        require(maxAskDrift >= 1.00) { "require a minimum of 1.00 for our ask drift" }
    }

    internal fun minAsk(bid: Double): Double =
        (bid.toBigDecimal() * minAskSpread.toBigDecimal()).setScale(2, RoundingMode.HALF_UP).toDouble()

    internal fun maxBid(ask: Double): Double =
        (ask.toBigDecimal() * (minBidSpread).toBigDecimal()).setScale(2, RoundingMode.HALF_UP).toDouble()

    fun askPrice(marketTicker: MarketTicker) = max(minAsk(marketTicker.bid), marketTicker.ask)
    fun bidPrice(marketTicker: MarketTicker) = min(maxBid(marketTicker.ask), marketTicker.bid)

    fun createAsk(marketTicker: MarketTicker) = CreateOrderRequest(
        type = ActiveOrder.OrderType.ask,
        price = askPrice(marketTicker),
        amount = 0.0001,
    )

    fun createBid(marketTicker: MarketTicker) = CreateOrderRequest(
        type = ActiveOrder.OrderType.bid,
        price = bidPrice(marketTicker),
        amount = 0.0001,
    )

    internal fun outOfSync(activeOrder: ActiveOrder, marketTicker: MarketTicker): Boolean {
        return if (marketTicker.spreadAsPercentage().toDouble() < (1.0 + minSpread)) {
            // If spread is very low allow orders as long as they keep the minimum spread
            val validBidRange = (marketTicker.ask * (minBidSpread - 0.003)..marketTicker.ask * minBidSpread)
            val validAskRange = (marketTicker.bid * minAskSpread..marketTicker.bid * (minAskSpread + 0.003))
            when (activeOrder.type) {
                ActiveOrder.OrderType.bid -> activeOrder.price > marketTicker.bid || (activeOrder.price !in validBidRange)
                ActiveOrder.OrderType.ask -> activeOrder.price < marketTicker.ask || (activeOrder.price !in validAskRange)
            }
        } else {
            // If spread larger always follow bid/ask
            when (activeOrder.type) {
                ActiveOrder.OrderType.bid -> activeOrder.price > marketTicker.bid || activeOrder.price < (marketTicker.bid * maxBidDrift)
                ActiveOrder.OrderType.ask -> activeOrder.price < marketTicker.ask || activeOrder.price > (marketTicker.ask * maxAskDrift)
            }
        }
    }

    fun allValid(activeOrders: List<ActiveOrder>, marketTicker: MarketTicker): Boolean {
        return activeOrders.all { isValid(it, marketTicker) } && activeOrders.none {
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