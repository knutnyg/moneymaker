package xyz.nygaard

import kotlinx.coroutines.runBlocking
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.ActiveOrder.OrderType.ask
import xyz.nygaard.core.CreateOrderRequest
import xyz.nygaard.io.MarketTicker
import xyz.nygaard.core.PriceStrategy

class AskMaster(
    private val activeOrders: List<ActiveOrder>,
    val marketTicker: MarketTicker,
    private val priceStrategy: PriceStrategy = PriceStrategy(),
) {
    fun execute(): List<Action> = runBlocking {
        val actions = mutableListOf<Action>()
        val activeAsks = activeOrders.filter { it.type == ask }

        if (activeAsks.isEmpty()) {
            log.info("No active asks found")
            val req = CreateOrderRequest(
                type = ask,
                price = priceStrategy.askPrice(marketTicker),
                amount = 0.0001,
            )
            actions.add(AddAsk(req = req))
            return@runBlocking actions
        }

        if(priceStrategy.allValid(activeAsks, marketTicker)) {
            log.info("Keeping ask@${activeAsks.first().price}")
            actions.add(KeepAsk(CreateOrderRequest(ask, activeAsks.first().price)))
        } else {
            log.info("We have an ask we need to move")
            actions.add(ClearOrders)

            val req = CreateOrderRequest(
                type = ask,
                price = priceStrategy.askPrice(marketTicker),
                amount = 0.0001,
            )
            actions.add(AddAsk(req = req))
        }
        actions
    }
}
