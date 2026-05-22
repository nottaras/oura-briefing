package io.github.nottaras.briefing.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import io.github.nottaras.briefing.config.ConfigPaths
import io.github.nottaras.briefing.config.OAuthConstants
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
import java.net.BindException
import java.net.URI
import java.time.Instant
import kotlin.io.path.writeText

private val prettyJson = Json { prettyPrint = true }
private val lenientJson = Json { ignoreUnknownKeys = true }

class AuthCommand : CliktCommand(name = "auth") {

    override fun help(context: Context) = "Authenticate with Oura Ring"

    override fun run() = runBlocking {
        val config = loadConfig()
        val codeDeferred = CompletableDeferred<String>()

        val server = try {
            embeddedServer(Netty, port = OAuthConstants.CALLBACK_PORT) {
                routing {
                    get("/callback") {
                        suspend fun fail(message: String, status: HttpStatusCode = HttpStatusCode.BadRequest) {
                            call.respondText(message, status = status)
                            codeDeferred.completeExceptionally(RuntimeException(message))
                        }

                        val oauthError = call.request.queryParameters["error"]
                        if (oauthError != null) {
                            val description = call.request.queryParameters["error_description"] ?: oauthError
                            fail("Authorization failed: $description")
                            return@get
                        }

                        val code = call.request.queryParameters["code"]
                        if (code == null) {
                            fail("Missing authorization code.")
                            return@get
                        }
                        call.respondText("Authenticated! You can close this tab.")
                        codeDeferred.complete(code)
                    }
                }
            }.start(wait = false)
        } catch (e: BindException) {
            throw RuntimeException(
                "Port ${OAuthConstants.CALLBACK_PORT} is already in use. " +
                    "Stop the other process or change the redirect URI in your Oura OAuth app.",
                e,
            )
        }

        try {
            openBrowser(buildAuthorizeUrl(config.oura.clientId))
            echo("Waiting for Oura authorization...")

            val code = try {
                codeDeferred.await()
            } catch (e: Exception) {
                throw RuntimeException(e.message ?: "Oura authorization failed", e)
            }

            val tokens = exchangeCode(code, config.oura.clientId, config.oura.clientSecret)

            ConfigPaths.ensureConfigDirExists()
            ConfigPaths.tokensFile.writeText(prettyJson.encodeToString(tokens))

            echo("✓ Authenticated")
        } finally {
            server.stop(gracePeriodMillis = 500, timeoutMillis = 1000)
        }
    }

    private suspend fun exchangeCode(code: String, clientId: String, clientSecret: String): OuraTokens {
        val http = HttpClient(CIO) {
            install(ContentNegotiation) { json(lenientJson) }
            expectSuccess = false
        }
        return try {
            val httpResponse = http.submitForm(
                url = OAuthConstants.TOKEN_URL,
                formParameters = parameters {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("redirect_uri", OAuthConstants.REDIRECT_URI)
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
        "${OAuthConstants.AUTHORIZE_URL}?response_type=code&client_id=$clientId" +
            "&redirect_uri=${OAuthConstants.REDIRECT_URI}&scope=${OAuthConstants.SCOPES}"

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
