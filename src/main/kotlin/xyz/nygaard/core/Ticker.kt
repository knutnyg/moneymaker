package xyz.nygaard.core

import kotlinx.coroutines.runBlocking
import xyz.nygaard.BidMaster
import xyz.nygaard.FiriClient
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.log
import java.util.*

class Ticker(private val firiClient: FiriClient) : TimerTask() {
    override fun run() = runBlocking {
        val marketTicker = firiClient.fetchMarketTicker()
        log.info(marketTicker.toString())

        val activeBids = firiClient.getActiveOrders()
            .filter { it.type == ActiveOrder.OrderType.bid }

        BidMaster(activeBids, marketTicker, firiClient).execute()
    }
}