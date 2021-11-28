package xyz.nygaard.core

import io.ktor.utils.io.errors.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import xyz.nygaard.*
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.FiriClient
import xyz.nygaard.io.MarketTicker
import java.util.*
import java.util.function.Consumer

class Ticker(
    private val firiClient: FiriClient,
    private val taskMaster: TaskMaster,
    private val onActiveOrders: Consumer<List<ActiveOrder>>?,
    private val onActions: Consumer<List<Action>>?,
    private val onMarket: (mt: MarketTicker) -> Unit,
    private val onBalance: (a: AccountBalance) -> Unit,
) : TimerTask() {
    override fun run() = runBlocking {
        try {
            coroutineScope {
                val marketTickerCall = async { firiClient.fetchMarketTicker() }
                val activeOrdersCall = async { firiClient.getActiveOrders() }
                val balanceCall = async { firiClient.getBalance() }

                val marketTicker = marketTickerCall.await()
                val activeOrders = activeOrdersCall.await()
                val balance = balanceCall.await()

                log.info(marketTicker.toString())
                onActiveOrders?.accept(activeOrders)
                onMarket(marketTicker)
                onBalance(balance)

                val bidActions = BidMaster(activeOrders, marketTicker).execute()
                val askActions = AskMaster(activeOrders, marketTicker).execute()

                val actions = merge(bidActions, askActions)
                taskMaster.run(actions.runnableActions)

                onActions?.accept(actions.actionLog)
                return@coroutineScope
            }
        } catch (exception: Throwable) {
            log.error("Failed IO. Skipping this tick")
        }
    }
}