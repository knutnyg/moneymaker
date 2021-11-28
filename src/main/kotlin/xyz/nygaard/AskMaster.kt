package xyz.nygaard

import kotlinx.coroutines.runBlocking
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.ActiveOrder.OrderType.ask
import xyz.nygaard.core.CreateOrderRequest
import xyz.nygaard.io.MarketTicker
import xyz.nygaard.core.strategy.PriceStrategy
import xyz.nygaard.core.strategy.Strategy

class AskMaster(
    private val activeOrders: List<ActiveOrder>,
    val marketTicker: MarketTicker,
    private val priceStrategy: Strategy = PriceStrategy(),
) {
    fun execute(): List<Action> = runBlocking {
        val actions = mutableListOf<Action>()
        val activeAsks = activeOrders.filter { it.type == ask }

        if (activeAsks.isEmpty()) {
            log.info("No active asks found")
            actions.add(AddAsk(req = priceStrategy.createAsk(marketTicker)))
            return@runBlocking actions
        }

        if(priceStrategy.allValid(activeAsks, marketTicker)) {
            log.info("Keeping ask@${activeAsks.first().price}")
            actions.add(KeepAsk(CreateOrderRequest(ask, activeAsks.first().price)))
        } else {
            log.info("We have an ask we need to move")
            actions.add(ClearOrders())
            actions.add(AddAsk(req = priceStrategy.createAsk(marketTicker)))
        }
        actions
    }
}
