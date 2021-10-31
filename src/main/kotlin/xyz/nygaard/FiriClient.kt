package xyz.nygaard

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.math.BigDecimal
import java.math.RoundingMode
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

    fun valid(marketTicker: MarketTicker): Boolean {
        return when (type) {
            OrderType.bid -> this.price <= marketTicker.maxBid()
            OrderType.ask -> this.price >= marketTicker.minAsk()
        }
    }

    fun outOfSync(marketTicker: MarketTicker): Boolean {
        return when(type) {
            OrderType.bid -> this.price < (marketTicker.bid * 0.95)
            OrderType.ask -> this.price > (marketTicker.ask * 1.05)
        }
    }
}

enum class Market { BTCNOK }

data class MarketTicker(
    val bid: Double,
    val ask: Double,
    val spread: Double
) {

    private fun spreadAsPercentage() = BigDecimal( spread / ((ask + bid) / 2 ) * 100 ).setScale(2, RoundingMode.HALF_UP)

    fun maxBid(): Double = (BigDecimal(ask) * BigDecimal(0.989)).setScale(2, RoundingMode.HALF_UP).toDouble()
    fun minAsk(): Double = (BigDecimal(bid) * BigDecimal(1.011)).setScale(2, RoundingMode.HALF_UP).toDouble()

    override fun toString(): String {
        return "MarketTick BTCNOK: bid: $bid NOK, ask: $ask NOK, spread: $spread NOK(${spreadAsPercentage()}%)"
    }
}

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
            log.info("Deleted all open orders")
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

    data class OrderRequest(
        val market: String = "BTCNOK",
        val type: String,
        val price: String,
        val amount: String
    )

    data class OrderResponse(
        val id: Int
    )

    suspend fun placeBid(price: Double, amount: Double = 0.0001, dryRun: Boolean = true): OrderResponse {
        log.info("Placing bid for $amount BTCNOK @ $price")

        if (dryRun) return OrderResponse(123)
            .also { log.info("Skipped placing order due to dry run") }

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