package xyz.nygaard

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
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
import xyz.nygaard.db.Database
import java.io.File
import javax.sql.DataSource

val log: Logger = LoggerFactory.getLogger("Lightning Store")

fun main() {
    embeddedServer(Netty, port = 8020, host = "localhost") {

        val environment = Config(
            databaseName = System.getenv("LS_DATABASE_NAME"),
            databaseUsername = System.getenv("LS_DATABASE_USERNAME"),
            databasePassword = System.getenv("LS_DATABASE_PASSWORD"),
            staticResourcesPath = getEnvOrDefault("LS_STATIC_RESOURCES", "src/main/frontend/build"),
        )

        val database = Database(
            "jdbc:postgresql://localhost:5432/${environment.databaseName}",
            environment.databaseUsername,
            environment.databasePassword,
        )
        buildApplication(
            dataSource = database.dataSource,
            staticResourcesPath = environment.staticResourcesPath,
        )
    }.start(wait = true)
}

internal fun Application.buildApplication(
    dataSource: DataSource,
    staticResourcesPath: String,
) {
    installContentNegotiation()
    install(XForwardedHeaderSupport)
    install(CallLogging) {
        level = Level.TRACE
    }
    routing {
        route("/api") {
            registerSelftestApi()
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

fun Route.registerSelftestApi() {
    get("/isAlive") {
        call.respondText("I'm alive! :)")
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
    val databaseName: String,
    val databaseUsername: String,
    val databasePassword: String,
    val staticResourcesPath: String
)

fun getEnvOrDefault(name: String, defaultValue: String): String {
    return System.getenv(name) ?: defaultValue
}