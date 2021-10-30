package xyz.nygaard

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import xyz.nygaard.util.createSignature
import java.io.File
import java.io.FileInputStream
import java.util.*

val log: Logger = LoggerFactory.getLogger("Lightning Store")

fun main() {
    embeddedServer(Netty, port = 8020, host = "localhost") {

        val props = Properties()

        val propertiesFile = File("src/resources/config.properties")
        if (propertiesFile.exists()) {
            log.info("loaded config.properties")
            props.load(FileInputStream("src/resources/config.properties"))
        }


        val environment = Config(
            staticResourcesPath = getEnvOrDefault("STATIC_FOLDER", "src/main/frontend/build"),
            clientId = props["CLIENT_ID"].toString(),
            clientSecret = props["CLIENT_SECRET"].toString(),
            apiKey = props["API_KEY"].toString(),
            firiBaseUrl = "https://api.firi.com/v2/"
        )

        buildApplication(
            staticResourcesPath = environment.staticResourcesPath,
            config = environment
        )
    }.start(wait = true)
}

internal fun Application.buildApplication(
    staticResourcesPath: String,
    config: Config,
    httpClient: HttpClient = HttpClient(CIO)
) {
    installContentNegotiation()
    install(XForwardedHeaderSupport)
    install(CallLogging) {
        level = Level.TRACE
    }
    routing {
        route("/api") {
            registerSelftestApi(httpClient)
            get("/balance") {
                log.info("looking up balance")
                httpClient.use {
                    val res: HttpResponse = it.get("${config.firiBaseUrl}/balances") {
                        header("miraiex-access-key", config.apiKey)
                        header("firi-user-clientid", config.clientId)
                        header("firi-user-signature", createSignature(config.clientSecret))
                    }
                    call.respond(res.readText())
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
    val clientSecret: String
)

fun getEnvOrDefault(name: String, defaultValue: String): String {
    return System.getenv(name) ?: defaultValue
}