package xyz.nygaard.core.strategy

import xyz.nygaard.core.CreateOrderRequest
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.MarketTicker

interface Strategy {
    fun createAsk(marketTicker: MarketTicker): CreateOrderRequest
    fun createBid(marketTicker: MarketTicker): CreateOrderRequest
    fun allValid(activeAsks: List<ActiveOrder>, marketTicker: MarketTicker): Boolean
}