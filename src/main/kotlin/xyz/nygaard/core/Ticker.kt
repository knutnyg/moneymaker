package xyz.nygaard.core

import io.ktor.utils.io.errors.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import xyz.nygaard.*
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.FiriClient
import java.util.*
import java.util.function.Consumer

class Ticker(
    private val firiClient: FiriClient,
    private val taskMaster: TaskMaster,
    private val onActiveOrders: Consumer<List<ActiveOrder>>?,
    private val onActions: Consumer<List<Action>>?,
) : TimerTask() {
    override fun run() = runBlocking {
        try {
            coroutineScope {
                val marketTickerCall = async { firiClient.fetchMarketTicker() }
                val activeOrdersCall = async { firiClient.getActiveOrders() }

                val marketTicker = marketTickerCall.await()
                val activeOrders = activeOrdersCall.await()

                log.info(marketTicker.toString())
                onActiveOrders?.accept(activeOrders)

                val bidActions = BidMaster(activeOrders, marketTicker).execute()
                val askActions = AskMaster(activeOrders, marketTicker).execute()

                val actions = merge(bidActions, askActions)
                taskMaster.run(actions)

                onActions?.accept(actions)
                return@coroutineScope
            }
        } catch (exception: IOException) {
            log.error("Failed IO. Skipping this tick")
        }
    }
}