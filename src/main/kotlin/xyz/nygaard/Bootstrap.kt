package xyz.nygaard

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level
import xyz.nygaard.core.AppState
import xyz.nygaard.core.MarketState
import xyz.nygaard.core.ReportTicker
import xyz.nygaard.core.Ticker
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
    AppState.update {
        it.copy(
            activeTrades = it.activeTrades.copy(
                activeOrders = active,
            ),
            lastUpdatedAt = Instant.now(),
        )
    }

    val reporter = ReportTicker(
        firiClient = firiClient,
        onFilledOrders = { orders: List<ActiveOrder> ->
            val now = Instant.now()
            AppState.update {
                it.copy(filledOrders = it.filledOrders.copy(
                    lastUpdatedAt = now,
                    filledOrders = orders,
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
                    prevActionSet = actions,
                )
            }
        },
        onActiveOrders = { activeOrders ->
            AppState.update {
                it.copy(
                    activeTrades = it.activeTrades.copy(activeOrders = activeOrders),
                )
            }
        },
        onMarket = { mt ->
            AppState.update {
                it.copy(
                    market = MarketState(mapOf(Market.BTCNOK to mt)),
                )
            }
        }
    )

    val timer = Timer("tick")
    timer.scheduleAtFixedRate(ticker, 2000, 5000) // every 5 sec
    timer.scheduleAtFixedRate(reporter, 5000, 300000) // every  5 min

    val server = embeddedServer(Netty, port = config.port, host = "localhost") {
        buildApplication(
            staticResourcesPath = config.staticResourcesPath,
            firiClient = firiClient,
        )
    }.start()

    setupTearDownHook(timer, server, firiClient)
}

private fun setupHttpClient(noLog: Boolean) = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = JacksonSerializer() {
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
            val start = context.attributes[startedAtKey]
            val elapsedMs = Instant.now().toEpochMilli() - start
            log.info(
                "Response: <--- [{}] {} {}: {} in {}ms",
                context.request.headers["X-Request-ID"] ?: "NONE",
                context.request.method.value,
                context.request.url,
                context.response.status.toString(),
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
        port = getEnvOrDefault("MONEYMAKER_PORT", "8020").toInt()
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
    installContentNegotiation()
    install(CORS) {
        anyHost()
    }
    install(XForwardedHeaderSupport)
    install(CallLogging) {
        level = Level.TRACE
    }
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
            get("/app/state/listen") {
                val now = AppState.get()

                val events = callbackFlow<AppState> {
                    trySend(now)
                    AppState.listen(call) { state: AppState ->
                        trySend(state)
                    }

                    awaitClose {
                        AppState.removeListener(call)
                    }
                }

                try {
                    call.response.cacheControl(CacheControl.NoCache(null))
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
            call.respond(res.readText())
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
)

fun getEnvOrDefault(name: String, defaultValue: String): String {
    return System.getenv(name) ?: defaultValue
}