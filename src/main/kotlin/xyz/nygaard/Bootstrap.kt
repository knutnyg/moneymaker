package xyz.nygaard

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.jackson.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.EmptyContent.headers
import io.ktor.client.utils.EmptyContent.status
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.server.websocket.WebSockets.Plugin
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level
import xyz.nygaard.core.*
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.io.FiriClient
import xyz.nygaard.io.Market
import xyz.nygaard.util.createKey
import java.io.File
import java.io.FileInputStream
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("Moneymaker")

private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

fun <T> toJson(value: T): String = objectMapper.writeValueAsString(value)
fun <T> toJsonBytes(value: T): ByteArray = objectMapper.writeValueAsBytes(value)

fun getRequestId(): String = MDC.get("r-id") ?: generateRequestId()
fun generateRequestId(): String = UUID.randomUUID().toString()

@ExperimentalCoroutinesApi
fun main() {
    val config = setupConfig()
    val httpClient = setupHttpClient(config.noLog)
    val secretKey = createKey(config.clientSecret)

    val firiClient = FiriClient(
        clientId = config.clientId,
        clientSecret = secretKey,
        httpclient = httpClient,
    )

    val active = runBlocking { firiClient.getActiveOrders() }
    val balance = runBlocking { firiClient.getBalance() }
    val now = Instant.now()
    AppState.update {
        it.copy(
            activeTrades = it.activeTrades.copy(
                activeOrders = active,
                lastUpdatedAt = now,
            ),
            accountBalance = it.accountBalance.copy(
                account = balance,
            ),
            lastUpdatedAt = now,
        )
    }

    val reporter = ReportTicker(
        firiClient = firiClient,
        onFilledOrders = { orders: List<ActiveOrder> ->
            val now = Instant.now()
            AppState.update {
                it.copy(filledOrders = it.filledOrders.copy(
                    filledOrders = orders,
                    lastUpdatedAt = now,
                ))
            }
        },
    )
    val ticker = Ticker(
        firiClient,
        taskMaster = TaskMaster(firiClient),
        onActions = { actions ->
            AppState.update {
                it.copy(
                    prevActionSet = it.prevActionSet.copy(
                        actions = actions,
                        lastUpdatedAt = Instant.now(),
                    ),
                )
            }
        },
        onActiveOrders = { activeOrders ->
            AppState.update {
                it.copy(
                    activeTrades = it.activeTrades.copy(
                        activeOrders = activeOrders,
                        lastUpdatedAt = Instant.now(),
                    ),
                )
            }
        },
        onMarket = { mt ->
            AppState.update {
                it.copy(
                    market = MarketState(mapOf(Market.BTCNOK to mt)),
                )
            }
        },
        onBalance = { accountBalance ->
            AppState.update {
                it.copy(
                    accountBalance = it.accountBalance.copy(
                        account = accountBalance,
                    ),
                )
            }
        }
    )

    val timer = Timer("tick")
    timer.scheduleAtFixedRate(ticker, 2000, 5000) // every 5 sec
    timer.scheduleAtFixedRate(reporter, 5000, 300000) // every  5 min

    val server = embeddedServer(Netty, port = config.port, host = config.host) {
        buildApplication(
            staticResourcesPath = config.staticResourcesPath,
            firiClient = firiClient,
        )
    }.start()

    setupTearDownHook(timer, server, firiClient)
}

private fun setupHttpClient(noLog: Boolean) = HttpClient(CIO) {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        jackson {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerModule(JavaTimeModule())
        }
    }
    install("RequestLogging") {
        val startedAtKey = AttributeKey<Long>("started-at")

        if (noLog) {
            return@install
        }
        sendPipeline.intercept(HttpSendPipeline.Monitoring) {
            val start = Instant.now().toEpochMilli()
            context.attributes.put(startedAtKey, start)
            log.info(
                "Request:  ---> [{}] {} {}",
                context.headers.get("X-Request-ID") ?: "NONE",
                context.method.value,
                Url(context.url),
            )
        }
        receivePipeline.intercept(HttpReceivePipeline.After) {
            val req = subject.request;
            val attr = subject.request.attributes;

            val start = attr[startedAtKey]

            val elapsedMs = Instant.now().toEpochMilli() - start
            log.info(
                "Response: <--- [{}] {} {}: {} in {}ms",
                req.headers["X-Request-ID"] ?: "NONE",

                req.method.value,
                req.url,
                subject.status.toString(),
                elapsedMs,
            )
        }
    }
    //install(Logging) {
    //    level = LogLevel.INFO
    //}
    defaultRequest {
        header("X-Request-ID", getRequestId())
    }
}

fun setupConfig(): Config {
    val props = Properties()

    val propertiesFile = File("src/main/resources/config.properties")
    if (propertiesFile.exists()) {
        log.info("loaded config.properties")
        props.load(FileInputStream("src/main/resources/config.properties"))
    }

    return Config(
        staticResourcesPath = getEnvOrDefault("STATIC_FOLDER", "src/main/frontend/build"),
        clientId = props["CLIENT_ID"].toString(),
        clientSecret = props["CLIENT_SECRET"].toString(),
        apiKey = props["API_KEY"].toString(),
        firiBaseUrl = "https://api.firi.com/v2/",
        noLog = true,
        port = props.getOrDefault("MONEYMAKER_PORT", "8020").toString().toInt(),
        host = props.getOrDefault("MONEYMAKER_HOST", "localhost").toString()
    )

}

private fun setupTearDownHook(
    timer: Timer,
    server: NettyApplicationEngine,
    firiClient: FiriClient,
) {
    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("stop timer")
        timer.cancel()
        log.info("stop timer: STOPPED")

        log.info("stop server")
        server.stop(1, 5, TimeUnit.SECONDS)
        log.info("stop server: STOPPED")

        log.info("cleanup active orders")
        runBlocking { firiClient.deleteActiveOrders() }
        log.info("cleanup active orders: STOPPED")
    })
}

@ExperimentalCoroutinesApi
internal fun Application.buildApplication(
    staticResourcesPath: String,
    firiClient: FiriClient,
    httpClient: HttpClient = HttpClient(CIO),
) {
    val log = log

    install(CORS) {
        anyHost()
    }
    install(XForwardedHeaders)
    install(CallLogging) {
        level = Level.TRACE
    }
    install(io.ktor.server.websocket.WebSockets)
    installContentNegotiation()
    routing {
        route("/api") {
            registerSelftestApi(httpClient)
            get("/balance") {
                log.info("looking up balance")
                val balance = firiClient.getBalance()
                call.respond(balance)
            }
            get("/orders/open") {
                log.info("fetching open orders")
                val orders = firiClient.getActiveOrders()
                call.respond(orders)
            }

            get("/orders/delete") {
                firiClient.deleteActiveOrders()
                call.respond("Deleted all orders")
            }

            get("/market") {
                val market = firiClient.fetchMarketTicker()
                call.respond(market)
            }
            get("/app/state") {
                val state = AppState.get()
                call.respond(state)
            }
            webSocket("/app/state/ws") {
                log.info("new ws client")
                val now = AppState.get()

                send(toJson(now))
                val events = callbackFlow {
                    trySend(now)
                    AppState.listen(call) { state: AppState ->
                        trySend(state)
                    }

                    awaitClose {
                        AppState.removeListener(call)
                    }
                }

                try {
                    events.collect {
                        val state: AppState = it
                        withContext(Dispatchers.IO) {
                            log.info("push state for {}", call.request.origin.remoteHost)
                            val data = toJson(state)
                            send(data)
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    log.info("onClose ${closeReason.await()}")
                } catch (e: Exception) {
                    log.warn("error: ", e)
                } finally {
                    log.info("cleanup listener")
                    AppState.removeListener(call)
                }

            }
            get("/app/state/listen") {
                val now = AppState.get()

                val events = callbackFlow {
                    trySend(now)
                    AppState.listen(call) { state: AppState ->
                        trySend(state)
                    }

                    awaitClose {
                        AppState.removeListener(call)
                    }
                }

                try {
                    call.response.headers.append(HttpHeaders.CacheControl, "no-cache, no-transform")
                    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                        events.collect {
                            val state: AppState = it
                            withContext(Dispatchers.IO) {
                                log.info("push state for {}", call.request.origin.remoteHost)
                                val data = toJson(state)
                                write("data: ${data}\n\n")
                                flush()
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.warn("error: ", e)
                } finally {
                    log.info("cleanup listener")
                    AppState.removeListener(call)
                }
            }
        }

        // Serves all static content i.e: example.com/static/css/styles.css
        static("/static") {
            staticRootFolder = File(staticResourcesPath)
            files("static")
        }

        // Serves index.html on example.com
        static("/") {
            default("$staticResourcesPath/index.html")
        }

        // Serves index.html on other paths like: example.com/register
        static("*") {
            default("$staticResourcesPath/index.html")
        }

    }
}

fun Route.registerSelftestApi(httpClient: HttpClient) {
    get("/isAlive") {
        call.respondText("I'm alive! :)")
    }
    get("/health") {
        val state = AppState.get()
        val listeners = AppState.listenersCount()

        call.respond(
            mapOf(
                "state" to state,
                "listeners" to listeners,
            )
        )
    }
    get("/firi") {
        log.info("looking up markets")
        httpClient.use {
            val res: HttpResponse = it.get("https://api.firi.com/v2/markets")
            call.respond(res.bodyAsText())
        }
    }
}

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}

data class Config(
    val staticResourcesPath: String,
    val firiBaseUrl: String,
    val apiKey: String,
    val clientId: String,
    val clientSecret: String,
    val noLog: Boolean = true,
    val port: Int = 8020,
    val host: String = "localhost",
)

fun getEnvOrDefault(name: String, defaultValue: String): String {
    return System.getenv(name) ?: defaultValue
}