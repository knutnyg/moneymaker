package xyz.nygaard.core

import kotlinx.coroutines.runBlocking
import xyz.nygaard.BidMaster
import xyz.nygaard.FiriClient
import xyz.nygaard.TaskMaster
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.log
import java.util.*

class Ticker(private val firiClient: FiriClient, private val taskMaster: TaskMaster) : TimerTask() {
    override fun run() = runBlocking {
        val marketTicker = firiClient.fetchMarketTicker()
        log.info(marketTicker.toString())

        val activeBids = firiClient.getActiveOrders()
            .filter { it.type == ActiveOrder.OrderType.bid }

        val actions = BidMaster(activeBids, marketTicker).execute()
        taskMaster.run(actions)
    }
}