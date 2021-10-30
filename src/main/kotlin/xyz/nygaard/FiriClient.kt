package xyz.nygaard

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.LocalDateTime

data class ActiveOrder(
    val id: Int,
    val market: Market,
    val type: OrderType,
    val price: Double,
    val remaining: Double,
    val amount: Double,
    val matched: Double,
    val cancelled: Double,
    val created_at: LocalDateTime
) {
    enum class OrderType { bid, ask }

}

enum class Market { BTCNOK }

data class MarketTicker(
    val bid: Double,
    val ask: Double,
    val spread: Double
)

interface IFiriClient {
    suspend fun getActiveOrders(): List<ActiveOrder>
    suspend fun deleteActiveOrders()
    suspend fun marketTicker(): MarketTicker
}

class FiriClient(val httpclient: HttpClient, val apiKey: String) : IFiriClient {

    val baseUrl = "https://api.firi.com/v2"

    override suspend fun getActiveOrders(): List<ActiveOrder> {
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

    fun getAllFilledOrders() {

    }

    override suspend fun deleteActiveOrders() {
        val res: HttpResponse = httpclient.delete("${baseUrl}/orders/${Market.BTCNOK}") {
            header("miraiex-access-key", apiKey)
        }
        if (res.status.isSuccess()) {
            log.info("deleted all open orders")
        } else {
            log.error("failed to delete all open orders")
        }
    }

    override suspend fun marketTicker(): MarketTicker {
        val res: HttpResponse = httpclient.get("${baseUrl}/markets/${Market.BTCNOK}/ticker")
        return try {
            res.receive()
        } catch (e: Exception) {
            log.info("Failed to fetch market", e)
            throw RuntimeException(e)
        }
    }
}