package xyz.nygaard.io

import xyz.nygaard.io.responses.CurrencyBalance
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.errors.*
import xyz.nygaard.core.AccountBalance
import xyz.nygaard.core.CreateOrderRequest
import xyz.nygaard.io.requests.RequestBase
import xyz.nygaard.io.responses.Currency
import xyz.nygaard.log
import java.security.Key

class FiriClient(
    private val clientId: String,
    private val baseUrl: String = "https://api.firi.com/v2",
    private val clientSecret: Key,
    private val httpclient: HttpClient
) {
    suspend fun getBalance(): AccountBalance = try {
        val currencies: List<CurrencyBalance> = httpclient.signedGet("${baseUrl}/balances").body()
        AccountBalance(currencies = currencies.associateBy { Currency.valueOf(it.currency) })
    } catch (e: Exception) {
        log.info("Failed to fetch balance", e)
        throw RuntimeException("Failed to fetch balance", e)
    }

    suspend fun getActiveOrders(): List<ActiveOrder> =
        try {
            httpclient.signedGet("${baseUrl}/orders/${Market.BTCNOK}").body()
        } catch (e: Exception) {
            log.info("Failed to fetch orders", e)
            throw IOException("Failed to fetch orders", e)
        }


    suspend fun deleteActiveOrders(market: Market = Market.BTCNOK) {
        val res: HttpResponse = httpclient.signedDelete("${baseUrl}/orders/${market}")
        if (res.status.isSuccess()) {
            log.info("Deleted all open orders")
        } else {
            log.error("failed to delete all open orders")
        }
    }

    suspend fun getFilledOrders(): List<ActiveOrder> =
        try {
            httpclient.signedGet("${baseUrl}/orders/${Market.BTCNOK}/history").body()
        } catch (e: Exception) {
            log.info("Failed to fetch orders", e)
            throw IOException("Failed to fetch orders", e)
        }

    suspend fun fetchMarketTicker(): MarketTicker = try {
        httpclient.signedGet("${baseUrl}/markets/${Market.BTCNOK}/ticker").body()
    } catch (e: Exception) {
        log.error("Failed to fetch market", e)
        throw IOException(e)
    }

    suspend fun placeOrder(req: CreateOrderRequest): OrderResponse {
        log.info("${req.market}: placing ${req.type} for ${req.amount} @ ${req.price}")
        return try {
            httpclient.signedPost("${baseUrl}/orders", req.toRequest()).body()
        } catch (e: Exception) {
            log.info("Failed to place order", e)
            throw IOException(e)
        }
    }

    private suspend fun HttpClient.signedGet(urlString: String): HttpResponse =
        signedRequest(HttpMethod.Get, urlString)

    private suspend fun HttpClient.signedPost(
        urlString: String,
        payload: RequestBase,
        contentType: ContentType = ContentType.Application.Json
    ): HttpResponse =
        signedRequest(HttpMethod.Post, urlString, payload = payload) {
            contentType(contentType)
            setBody(payload)
        }

    private suspend fun HttpClient.signedDelete(urlString: String) = signedRequest(HttpMethod.Delete, urlString)

    private suspend fun HttpClient.signedRequest(
        httpMethod: HttpMethod,
        urlString: String,
        payload: RequestBase = RequestBase(),
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return this.request(urlString) {
            method = httpMethod
            header("miraiex-user-clientid", clientId)
            header("miraiex-user-signature", payload.signWith(clientSecret))
            parameter("timestamp", payload.timestamp)
            parameter("validity", payload.validity)
            block()
        }
    }
}
