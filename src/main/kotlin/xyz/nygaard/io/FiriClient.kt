package xyz.nygaard

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import xyz.nygaard.io.*


class FiriClient(val httpclient: HttpClient, val apiKey: String) {

    private val baseUrl = "https://api.firi.com/v2"

    suspend fun getActiveOrders(): List<ActiveOrder> {
        val res: HttpResponse = httpclient.get("${baseUrl}/orders/${Market.BTCNOK}") {
            header("miraiex-access-key", apiKey)
        }
        return try {
            res.receive()
        } catch (e: Exception) {
            log.info("Failed to fetch orders", e)
            throw RuntimeException()
        }
    }

    suspend fun deleteActiveOrders() {
        val res: HttpResponse = httpclient.delete("${baseUrl}/orders/${Market.BTCNOK}") {
            header("miraiex-access-key", apiKey)
        }
        if (res.status.isSuccess()) {
            log.info("Deleted all open orders")
        } else {
            log.error("failed to delete all open orders")
        }
    }

    suspend fun fetchMarketTicker(): MarketTicker {
        val res: HttpResponse = httpclient.get("${baseUrl}/markets/${Market.BTCNOK}/ticker")
        return try {
            res.receive()
        } catch (e: Exception) {
            log.info("Failed to fetch market", e)
            throw RuntimeException(e)
        }
    }


    suspend fun placeBid(price: Double, amount: Double = 0.0001): OrderResponse {
        log.info("Placing bid for $amount BTCNOK @ $price")

        val res: HttpResponse = httpclient.post("${baseUrl}/orders") {
            contentType(ContentType.Application.Json)
            header("miraiex-access-key", apiKey)
            this.body = OrderRequest(
                type = "bid",
                price = price.toString(),
                amount = amount.toString()
            )
        }
        return try {
            res.receive()
        } catch (e: Exception) {
            log.info("Failed to place order", e)
            throw RuntimeException(e)
        }
    }
}