package xyz.nygaard

import kotlinx.coroutines.runBlocking
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.MarketTicker
import kotlin.math.max

class AskMaster(
    val activeAsks: List<ActiveOrder>,
    val marketTicker: MarketTicker,
    private val firiClient: FiriClient
) {
    fun execute(): Unit = runBlocking {
        if (activeAsks.hasInvalidOrders(marketTicker)) {
            log.info("Found active asks under threshold: ${marketTicker.minAsk()}")
            firiClient.deleteActiveOrders()
        }
        if (activeAsks.hasValidOrders(marketTicker)) {
            if (activeAsks.hasAnyOutOfSyncBids(marketTicker)) {
                log.info("We have a valid bid that is out of sync")
                firiClient.deleteActiveOrders()
                val price = max(marketTicker.minAsk(), marketTicker.ask)
                firiClient.placeAsk(price)
            } else {
                log.info("We have a valid bid, nothing to do here")
            }
        } else {
            val price = max(marketTicker.minAsk(), marketTicker.ask)
            firiClient.placeAsk(price)
        }
    }
}
