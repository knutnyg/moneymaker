package xyz.nygaard

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.MDC
import xyz.nygaard.io.*
import java.math.BigDecimal
import java.util.*


data class CreateOrderRequest(
    val type: ActiveOrder.OrderType,
    val price: Double,
    val amount: Double = 0.0001,
    val market: String = "BTCNOK",
)

data class AccountBalance(val currencies: Map<Currency, CurrencyBalance>)

data class CurrencyBalance(
    val currency: String,
    val balance: BigDecimal,
    val hold: BigDecimal,
    val available: BigDecimal,
)

enum class Currency { ADA, BTC, DAI, ETH, LTC, NOK, XRP, }

class FiriClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.firi.com/v2",
    httpclient: HttpClient
) {
    private val client: HttpClient
    init {
        client = httpclient.config {
            defaultRequest {
                header("miraiex-access-key", apiKey)
            }
        }
    }

    suspend fun getBalance(): AccountBalance {
        val res: HttpResponse = client.get("${baseUrl}/balances")

        return try {
            val currencies: List<CurrencyBalance> = res.receive()
            AccountBalance(currencies = currencies.associateBy { Currency.valueOf(it.currency) })
        } catch (e: Exception) {
            log.info("Failed to fetch orders", e)
            throw RuntimeException()
        }
    }

    suspend fun getActiveOrders(): List<ActiveOrder> {
        val res: HttpResponse = client.get("${baseUrl}/orders/${Market.BTCNOK}")

        return try {
            res.receive()
        } catch (e: Exception) {
            log.info("Failed to fetch orders", e)
            throw RuntimeException()
        }
    }

    suspend fun deleteActiveOrders() {
        val res: HttpResponse = client.delete("${baseUrl}/orders/${Market.BTCNOK}")
        if (res.status.isSuccess()) {
            log.info("Deleted all open orders")
        } else {
            log.error("failed to delete all open orders")
        }
    }

    suspend fun fetchMarketTicker(): MarketTicker {
        val res: HttpResponse = client.get("${baseUrl}/markets/${Market.BTCNOK}/ticker")
        return try {
            res.receive()
        } catch (e: Exception) {
            log.info("Failed to fetch market", e)
            throw RuntimeException(e)
        }
    }

    suspend fun placeOrder(req: CreateOrderRequest): OrderResponse {
        log.info("${req.market}: placing ${req.type} for ${req.amount} @ ${req.price}")

        val price = req.price
        val amount = req.amount
        val type = req.type
        val res: HttpResponse = client.post("${baseUrl}/orders") {
            contentType(ContentType.Application.Json)
            this.body = OrderRequest(
                market = req.market,
                type = type.name.lowercase(),
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

    suspend fun placeAsk(price: Double, amount: Double = 0.0001): OrderResponse {
        log.info("Placing ask for $amount BTCNOK @ $price")

        val res: HttpResponse = client.post("${baseUrl}/orders") {
            contentType(ContentType.Application.Json)
            this.body = OrderRequest(
                type = "ask",
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