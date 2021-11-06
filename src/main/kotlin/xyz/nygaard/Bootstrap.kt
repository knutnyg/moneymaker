package xyz.nygaard

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
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
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level
import xyz.nygaard.core.Ticker
import xyz.nygaard.io.ActiveOrder
import xyz.nygaard.util.createSignature
import java.io.File
import java.io.FileInputStream
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

val log: Logger = LoggerFactory.getLogger("Moneymaker")
val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

data class ActiveTradesState(
    val activeOrders: List<ActiveOrder>,
)

data class AppState(
    val activeTrades: ActiveTradesState,
    val prevActionSet: List<Action>,
    val lastUpdatedAt: Instant,
) {
    companion object {
        private val listeners: ConcurrentHashMap<ApplicationCall, (AppState) -> Unit> = ConcurrentHashMap()

        private val appState: AtomicReference<AppState> = AtomicReference(
            AppState(
                activeTrades = ActiveTradesState(activeOrders = listOf()),
                prevActionSet = listOf(),
                lastUpdatedAt = Instant.now(),
            )
        )

        internal fun notify(state: AppState) {
            listeners.forEach { (_, u) -> u(state) }
        }

        fun listen(o: ApplicationCall, l: (AppState) -> Unit) {
            listeners.putIfAbsent(o, l)
        }

        fun removeListener(o: ApplicationCall) {
            listeners.remove(o)
        }

        fun listenersCount() = listeners.size

        fun update(f: (AppState) -> AppState): AppState = appState.updateAndGet {
            val next = f(it).copy(
                lastUpdatedAt = Instant.now(),
            )
            notify(next)
            next
        }

        fun get(): AppState {
            return appState.get().copy()
        }
    }
}

fun getRequestId(): String = MDC.get("r-id") ?: generateRequestId()
fun generateRequestId(): String = UUID.randomUUID().toString()

fun main() {
    val props = Properties()

    val propertiesFile = File("src/main/resources/config.properties")
    if (propertiesFile.exists()) {
        log.info("loaded config.properties")
        props.load(FileInputStream("src/main/resources/config.properties"))
    }

    val environment = Config(
        staticResourcesPath = getEnvOrDefault("STATIC_FOLDER", "src/main/frontend/build"),
        clientId = props["CLIENT_ID"].toString(),
        clientSecret = props["CLIENT_SECRET"].toString(),
        apiKey = props["API_KEY"].toString(),
        firiBaseUrl = "https://api.firi.com/v2/"
    )

    val noLog = true
    val httpClient = HttpClient(CIO) {
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

    val firiClient = FiriClient(apiKey = environment.apiKey, httpclient = httpClient)

    val active = runBlocking { firiClient.getActiveOrders() }
    AppState.update {
        it.copy(
            activeTrades = it.activeTrades.copy(
                activeOrders = active,
            ),
            lastUpdatedAt = Instant.now(),
        )
    }

    val ticker = Ticker(
        firiClient,
        taskMaster = TaskMaster(firiClient),
        onActions = { actions ->
            AppState.update {
                it.copy(
                    prevActionSet = actions,
                    lastUpdatedAt = Instant.now(),
                )
            }
        },
        onActiveOrders = { activeOrders ->
            AppState.update {
                it.copy(
                    activeTrades = it.activeTrades.copy(activeOrders = activeOrders),
                    lastUpdatedAt = Instant.now(),
                )
            }
        },
    )

    Timer("tick")
        .scheduleAtFixedRate(ticker, 2000, 5000)

    embeddedServer(Netty, port = 8020, host = "localhost") {
        buildApplication(
            staticResourcesPath = environment.staticResourcesPath,
            firiClient = firiClient,
            config = environment
        )
    }.start(wait = true)
}

internal fun Application.buildApplication(
    staticResourcesPath: String,
    firiClient: FiriClient,
    config: Config,
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
                                val data = objectMapper.writeValueAsString(state)
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

        call.respond(mapOf(
            "state" to state,
            "listeners" to listeners,
        ))
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
)

fun getEnvOrDefault(name: String, defaultValue: String): String {
    return System.getenv(name) ?: defaultValue
}