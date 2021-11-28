package xyz.nygaard

import kotlinx.coroutines.runBlocking
import xyz.nygaard.core.AccountBalance
import xyz.nygaard.core.CreateOrderRequest
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.ActiveOrder.OrderType
import xyz.nygaard.io.MarketTicker
import xyz.nygaard.core.strategy.BalancedStrategy
import xyz.nygaard.core.strategy.Strategy
import xyz.nygaard.io.responses.Currency

class BalanceMaster(
    private val account: AccountBalance,
    private val marketTicker: MarketTicker,
) {
    fun execute(): List<Action> {
        val btc = account.currencies[Currency.BTC]

        return listOf()
    }
}

class BidMaster(
    private val activeOrders: List<ActiveOrder>,
    val marketTicker: MarketTicker,
    private val balancedStrategy: Strategy = BalancedStrategy(),
) {
    fun execute(): List<Action> = runBlocking {
        val actions = mutableListOf<Action>()
        val activeBids = activeOrders.filter { it.type == OrderType.bid }

        if (activeBids.isEmpty()) {
            log.info("No active bids found")
            actions.add(AddBid(req = balancedStrategy.createBid(marketTicker)))
            return@runBlocking actions
        }

        if(balancedStrategy.allValid(activeBids, marketTicker)) {
            log.info("Keeping bid@${activeBids.first().price}")
            actions.add(KeepBid(CreateOrderRequest(OrderType.bid, activeBids.first().price)))
        } else {
            log.info("We have an bid we need to move")
            actions.add(ClearOrders())
            actions.add(AddBid(req = balancedStrategy.createBid(marketTicker)))
        }
        actions
    }
}
