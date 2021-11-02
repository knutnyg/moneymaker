package xyz.nygaard

import kotlinx.coroutines.runBlocking
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.ActiveOrder.OrderType
import xyz.nygaard.io.MarketTicker
import kotlin.math.min

class BidMaster(
    val activeOrders: List<ActiveOrder>,
    val marketTicker: MarketTicker,
) {
    fun execute(): List<Action> = runBlocking {
        val actions = mutableListOf<Action>()
        val activeBids = activeOrders.filter { it.type == OrderType.bid }
        if (activeBids.hasInvalidOrders(marketTicker)) {
            log.info("Found active bids over threshold: ${marketTicker.maxBid()}")
            actions.add(ClearOrders(OrderType.bid))
        }

        if (activeBids.hasValidOrders(marketTicker)) {
            if (activeBids.hasAnyOutOfSyncBids(marketTicker)) {
                log.info("We have a valid bid that is out of sync")
                if (!actions.contains(ClearOrders(OrderType.bid))) {
                    actions.add(ClearOrders(OrderType.bid))
                }

                val price = min(marketTicker.maxBid(), marketTicker.bid)
                val req = CreateOrderRequest(
                    type = OrderType.bid,
                    amount = 0.0001,
                    price = price,
                )
                actions.add(AddBid(req = req))
            } else {
                log.info("We have a valid bid, just keep the bid")
                actions.add(KeepBid(CreateOrderRequest(OrderType.bid, activeBids.first().price)))
            }
        } else {
            val price = min(marketTicker.maxBid(), marketTicker.bid)

            val req = CreateOrderRequest(
                type = OrderType.bid,
                amount = 0.0001,
                price = price,
            )
            actions.add(AddBid(req = req))
        }

        return@runBlocking actions
    }
}

fun List<ActiveOrder>.hasValidOrders(marketTicker: MarketTicker) = this.any { it.valid(marketTicker) }
fun List<ActiveOrder>.hasAnyOutOfSyncBids(marketTicker: MarketTicker) = this.any { it.outOfSync(marketTicker) }
fun List<ActiveOrder>.hasInvalidOrders(marketTicker: MarketTicker) = this.any { !it.valid(marketTicker) }