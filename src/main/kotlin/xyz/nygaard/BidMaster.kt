package xyz.nygaard

import kotlinx.coroutines.runBlocking
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.MarketTicker
import java.lang.Double

class BidMaster(
    val activeBids: List<ActiveOrder>,
    val marketTicker: MarketTicker,
    val firiClient: FiriClient
) {
    fun execute() = runBlocking {
        if (activeBids.hasInvalidOrders(marketTicker)) {
            log.info("Found active bids over threshold: ${marketTicker.maxBid()}")
            firiClient.deleteActiveOrders()
        }
        // Check if we should move bid
        if (activeBids.hasValidOrders(marketTicker)) {
            if (activeBids.hasAnyoutOfSyncBids(marketTicker)) {
                log.info("We have a valid bid that is out of sync")
                firiClient.deleteActiveOrders()
            } else {
                log.info("We have a valid bid, nothing to do here")
            }
        } else {
            val price = Double.min(marketTicker.maxBid(), marketTicker.bid)
            val response = firiClient.placeBid(price)
            log.info("Placed 1 bid @$price")
        }
    }
}

fun List<ActiveOrder>.hasValidOrders(marketTicker: MarketTicker) = this.any { it.valid(marketTicker) }
fun List<ActiveOrder>.hasAnyoutOfSyncBids(marketTicker: MarketTicker) = this.any { it.outOfSync(marketTicker) }
fun List<ActiveOrder>.hasInvalidOrders(marketTicker: MarketTicker) = this.any { !it.valid(marketTicker) }