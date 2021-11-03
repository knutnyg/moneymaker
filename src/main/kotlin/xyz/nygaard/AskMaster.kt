package xyz.nygaard

import kotlinx.coroutines.runBlocking
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.ActiveOrder.OrderType.ask
import xyz.nygaard.io.MarketTicker
import kotlin.math.max

class AskMaster(
    private val activeOrders: List<ActiveOrder>,
    val marketTicker: MarketTicker
) {
    fun execute(): List<Action> = runBlocking {
        val actions = mutableListOf<Action>()
        val activeAsks = activeOrders.filter { it.type == ask }

        if (activeAsks.hasInvalidOrders(marketTicker)) {
            log.info("Found active asks under threshold: ${marketTicker.minAsk()}")
            actions.add(ClearOrders)
        }
        if (activeAsks.hasValidOrders(marketTicker)) {
            if (activeAsks.hasAnyOutOfSyncBids(marketTicker)) {
                log.info("We have a valid ask that is out of sync")
                actions.add(ClearOrders)

                val req = CreateOrderRequest(
                    type = ask,
                    price = marketTicker.askPrice(),
                    amount = 0.0001,
                )
                actions.add(AddAsk(req = req))
            } else {
                log.info("Keeping ask@${activeAsks.first().price}")
                actions.add(KeepAsk(CreateOrderRequest(ask, activeAsks.first().price)))
            }
        } else {
            val req = CreateOrderRequest(
                type = ask,
                price = marketTicker.askPrice(),
                amount = 0.0001,
            )
            actions.add(AddAsk(req = req))
        }
        actions
    }
}
