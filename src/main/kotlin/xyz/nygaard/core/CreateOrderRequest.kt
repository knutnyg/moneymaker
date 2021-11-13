package xyz.nygaard.core

import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.requests.OrderRequest

data class CreateOrderRequest(
    val type: ActiveOrder.OrderType,
    val price: Double,
    val amount: Double = 0.0001,
    val market: String = "BTCNOK"
) {
    fun toRequest(): OrderRequest {
        return OrderRequest(
            market = market,
            price = price.toString(),
            amount = amount.toString(),
            type = type.toString()
        )
    }
}