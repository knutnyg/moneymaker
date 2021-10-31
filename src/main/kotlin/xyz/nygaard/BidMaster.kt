package xyz.nygaard

import kotlinx.coroutines.runBlocking
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.MarketTicker
import kotlin.math.min

class BidMaster(
    val activeBids: List<ActiveOrder>,
    val marketTicker: MarketTicker,
    private val firiClient: FiriClient
) {
    fun execute(): Unit = runBlocking {
        if (activeBids.hasInvalidOrders(marketTicker)) {
            log.info("Found active bids over threshold: ${marketTicker.maxBid()}")
            firiClient.deleteActiveOrders()
        }
        if (activeBids.hasValidOrders(marketTicker)) {
            if (activeBids.hasAnyOutOfSyncBids(marketTicker)) {
                log.info("We have a valid bid that is out of sync")
                firiClient.deleteActiveOrders()
                val price = min(marketTicker.maxBid(), marketTicker.bid)
                firiClient.placeBid(price)
            } else {
                log.info("We have a valid bid, nothing to do here")
            }
        } else {
            val price = min(marketTicker.maxBid(), marketTicker.bid)
            firiClient.placeBid(price)
        }
    }
}

fun List<ActiveOrder>.hasValidOrders(marketTicker: MarketTicker) = this.any { it.valid(marketTicker) }
fun List<ActiveOrder>.hasAnyOutOfSyncBids(marketTicker: MarketTicker) = this.any { it.outOfSync(marketTicker) }
fun List<ActiveOrder>.hasInvalidOrders(marketTicker: MarketTicker) = this.any { !it.valid(marketTicker) }