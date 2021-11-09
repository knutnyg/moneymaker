package xyz.nygaard.io

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import xyz.nygaard.log
import xyz.nygaard.util.createSignature
import java.math.BigDecimal
import java.time.Instant


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
    private val clientId: String,
    private val clientSecret: String,
    private val clientApiKey: String,
    private val baseUrl: String = "https://api.firi.com/v2",
    private val httpclient: HttpClient
) {
    suspend fun getBalance(): AccountBalance {
        val res: HttpResponse = httpclient.signedGet("${baseUrl}/balances")

        return try {
            val currencies: List<CurrencyBalance> = res.receive()
            AccountBalance(currencies = currencies.associateBy { Currency.valueOf(it.currency) })
        } catch (e: Exception) {
            log.info("Failed to fetch orders", e)
            throw RuntimeException()
        }
    }

    suspend fun getActiveOrders(): List<ActiveOrder> {
        val res: HttpResponse = httpclient.signedGet("${baseUrl}/orders/${Market.BTCNOK}")

        return try {
            res.receive()
        } catch (e: Exception) {
            log.info("Failed to fetch orders", e)
            throw RuntimeException()
        }
    }

    suspend fun deleteActiveOrders() {
        val res: HttpResponse = httpclient.signedDelete("${baseUrl}/orders/${Market.BTCNOK}")
        if (res.status.isSuccess()) {
            log.info("Deleted all open orders")
        } else {
            log.error("failed to delete all open orders")
        }
    }

    suspend fun fetchMarketTicker(): MarketTicker {
        val res: HttpResponse = httpclient.signedGet("${baseUrl}/markets/${Market.BTCNOK}/ticker")
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
        val res: HttpResponse = httpclient.signedPost("${baseUrl}/orders") {
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

        val res: HttpResponse = httpclient.signedPost("${baseUrl}/orders") {
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

    private suspend fun HttpClient.signedGet(urlString: String): HttpResponse = signedRequest(HttpMethod.Get, urlString)

    // TODO: This should use signed request but that doesn't work for some reason...
    private suspend fun HttpClient.signedPost(urlString: String, block: HttpRequestBuilder.() -> Unit): HttpResponse =
        this.post(urlString) {
            header("miraiex-access-key", clientApiKey)
            block()
        }

    private suspend fun HttpClient.signedDelete(urlString: String) = signedRequest(HttpMethod.Delete, urlString)

    private suspend fun HttpClient.signedRequest(
        httpMethod: HttpMethod,
        urlString: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        val timestamp = Instant.now().toEpochMilli() / 1000
        val validity = "2000"
        return this.request(urlString) {
            method = httpMethod
            header("miraiex-user-clientid", clientId)
            header("miraiex-user-signature", createSignature(clientSecret, timestamp.toString(), validity))
            parameter("timestamp", timestamp)
            parameter("validity", validity)
            block()
        }
    }


}