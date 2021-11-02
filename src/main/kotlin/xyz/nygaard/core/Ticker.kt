package xyz.nygaard.core

import kotlinx.coroutines.runBlocking
import xyz.nygaard.*
import java.util.*

class Ticker(private val firiClient: FiriClient, private val taskMaster: TaskMaster) : TimerTask() {
    override fun run() = runBlocking {
        val marketTicker = firiClient.fetchMarketTicker()
        log.info(marketTicker.toString())

        val activeOrders = firiClient.getActiveOrders()

        val bidActions = BidMaster(activeOrders, marketTicker).execute()
        val askActions = AskMaster(activeOrders, marketTicker).execute()

        taskMaster.run(merge(bidActions, askActions))
    }
}