package xyz.nygaard

import kotlinx.coroutines.runBlocking
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.ActiveOrder.OrderType.ask
import xyz.nygaard.io.MarketTicker
import kotlin.math.max

class AskMaster(
    val activeAsks: List<ActiveOrder>,
    val marketTicker: MarketTicker
) {
    fun execute(): List<Action> = runBlocking {
        val actions = mutableListOf<Action>()

        if (activeAsks.hasInvalidOrders(marketTicker)) {
            log.info("Found active asks under threshold: ${marketTicker.minAsk()}")
            actions.add(ClearOrders(ask))
        }
        if (activeAsks.hasValidOrders(marketTicker)) {
            if (activeAsks.hasAnyOutOfSyncBids(marketTicker)) {
                log.info("We have a valid bid that is out of sync")
                actions.add(ClearOrders(ask))

                val price = max(marketTicker.minAsk(), marketTicker.ask)
                val req = CreateOrderRequest(
                    type = ask,
                    price = price,
                    amount = 0.0001,
                )
                actions.add(AddAsk(req = req))
            } else {
                log.info("We have a valid bid, nothing to do here")
            }
        } else {
            val price = max(marketTicker.minAsk(), marketTicker.ask)
            val req = CreateOrderRequest(
                type = ask,
                price = price,
                amount = 0.0001,
            )
            actions.add(AddAsk(req = req))
        }
        return@runBlocking actions
    }
}
