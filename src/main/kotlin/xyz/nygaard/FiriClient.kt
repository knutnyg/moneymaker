package xyz.nygaard

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
    enum class Market { BTCNOK }
}

interface IFiriClient {
    suspend fun getActiveOrders(): List<ActiveOrder>
}

class FiriClient(val httpclient: HttpClient, val apiKey: String) : IFiriClient {

    val baseUrl = "https://api.firi.com/v2"

    override suspend fun getActiveOrders(): List<ActiveOrder> {
        val res: HttpResponse = httpclient.get("${baseUrl}/orders/${ActiveOrder.Market.BTCNOK}") {
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

    fun deleteOrders() {

    }
}