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
                val marketTickerCall = async {
                    val market = firiClient.fetchMarketTicker()
                    onMarket(market)
                    market
                }
                val activeOrdersCall = async {
                    val orders = firiClient.getActiveOrders()
                    onActiveOrders?.accept(orders)
                    orders
                }
                val balanceCall = async {
                    val balance = firiClient.getBalance()
                    onBalance(balance)
                    balance
                }

                val marketTicker = marketTickerCall.await()
                val activeOrders = activeOrdersCall.await()
                //val balance = balanceCall.await()

                log.info(marketTicker.toString())

                val bidActions = BidMaster(activeOrders, marketTicker).execute()
                val askActions = AskMaster(activeOrders, marketTicker).execute()

                val actions = merge(bidActions, askActions)
                taskMaster.run(actions.runnableActions)

                onActions?.accept(actions.actionLog)
                return@coroutineScope
            }
        } catch (exception: Throwable) {
            log.error("Failed tick: skipping", exception)
        }
    }
}