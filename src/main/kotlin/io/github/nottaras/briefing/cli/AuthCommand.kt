package io.github.nottaras.briefing.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import io.github.nottaras.briefing.config.ConfigPaths
import io.github.nottaras.briefing.config.OuraTokens
import io.github.nottaras.briefing.config.TokenResponse
import io.github.nottaras.briefing.config.loadConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.net.URI
import java.time.Instant
import kotlin.io.path.writeText

private val prettyJson = Json { prettyPrint = true }
private val lenientJson = Json { ignoreUnknownKeys = true }

private const val REDIRECT_URI = "http://localhost:8080/callback"
private const val AUTHORIZE_URL = "https://cloud.ouraring.com/oauth/authorize"
private const val TOKEN_URL = "https://api.ouraring.com/oauth/token"
private const val SCOPES = "daily"

class AuthCommand : CliktCommand(name = "auth") {

    override fun help(context: Context) = "Authenticate with Oura Ring"

    override fun run() = runBlocking {
        val config = loadConfig()
        val codeDeferred = CompletableDeferred<String>()

        val server = embeddedServer(Netty, port = 8080) {
            routing {
                get("/callback") {
                    val code = call.request.queryParameters["code"]
                    if (code == null) {
                        call.respondText("Missing authorization code.", status = HttpStatusCode.BadRequest)
                        return@get
                    }
                    // Respond to the browser first, then signal — avoids shutting down
                    // Netty mid-response when the main coroutine unblocks
                    call.respondText("Authenticated! You can close this tab.")
                    codeDeferred.complete(code)
                }
            }
        }.start(wait = false)

        openBrowser(buildAuthorizeUrl(config.oura.clientId))
        echo("Waiting for Oura authorization...")

        val code = codeDeferred.await()

        val tokens = exchangeCode(code, config.oura.clientId, config.oura.clientSecret)
        server.stop(gracePeriodMillis = 500, timeoutMillis = 1000)

        ConfigPaths.ensureConfigDirExists()
        ConfigPaths.tokensFile.writeText(prettyJson.encodeToString(tokens))

        echo("✓ Authenticated")
    }

    private suspend fun exchangeCode(code: String, clientId: String, clientSecret: String): OuraTokens {
        val http = HttpClient(CIO) {
            install(ContentNegotiation) { json(lenientJson) }
            expectSuccess = false
        }
        return try {
            val httpResponse = http.submitForm(
                url = TOKEN_URL,
                formParameters = parameters {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("redirect_uri", REDIRECT_URI)
                }
            )
            if (!httpResponse.status.isSuccess()) {
                error("Token exchange failed (${httpResponse.status}): ${httpResponse.bodyAsText()}")
            }
            val response: TokenResponse = httpResponse.body()
            OuraTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                expiresAt = Instant.now().epochSecond + response.expiresIn,
            )
        } finally {
            http.close()
        }
    }

    private fun buildAuthorizeUrl(clientId: String) =
        "$AUTHORIZE_URL?response_type=code&client_id=$clientId" +
            "&redirect_uri=$REDIRECT_URI&scope=$SCOPES"

    private fun openBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                return
            }
        } catch (_: Exception) {}
        echo("Open this URL in your browser:\n$url")
    }
}
