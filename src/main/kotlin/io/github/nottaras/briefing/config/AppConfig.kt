package io.github.nottaras.briefing.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val TOKEN_URL = "https://api.ouraring.com/oauth/token"
private val prettyJson = Json { prettyPrint = true }
private val lenientJson = Json { ignoreUnknownKeys = true }

data class AppConfig(
    val oura: OuraConfig,
    val anthropic: AnthropicConfig,
)

data class OuraConfig(
    val clientId: String,
    val clientSecret: String,
)

data class AnthropicConfig(
    val apiKey: String,
    val model: String = "claude-sonnet-4-5-20251001",
)

// Tokens are stored separately from config (never committed to git)
@Serializable
data class OuraTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long, // epoch seconds
)

@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

object ConfigPaths {
    val configDir: Path = Path.of(System.getProperty("user.home"), ".config", "oura-briefing")
    val configFile: Path = configDir.resolve("config.toml")
    val tokensFile: Path = configDir.resolve("tokens.json")

    fun ensureConfigDirExists() = configDir.createDirectories()
}

@OptIn(ExperimentalHoplite::class)
fun loadConfig(): AppConfig {
    val path = ConfigPaths.configFile
    require(path.exists()) {
        "Config not found at $path\nRun: cp config.example.toml ${path}"
    }
    return ConfigLoaderBuilder.default()
        .addFileSource(path.toFile())
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow()
}

suspend fun loadValidTokens(ouraConfig: OuraConfig): OuraTokens {
    require(ConfigPaths.tokensFile.exists()) {
        "Not authenticated. Run `briefing auth` first."
    }
    val tokens = lenientJson.decodeFromString<OuraTokens>(ConfigPaths.tokensFile.readText())
    if (Instant.now().epochSecond >= tokens.expiresAt - 60) {
        return refreshTokens(tokens.refreshToken, ouraConfig)
    }
    return tokens
}

private suspend fun refreshTokens(refreshToken: String, ouraConfig: OuraConfig): OuraTokens {
    val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(lenientJson) }
    }
    return try {
        val response: TokenResponse = http.submitForm(
            url = TOKEN_URL,
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", ouraConfig.clientId)
                append("client_secret", ouraConfig.clientSecret)
            }
        ).body()
        val newTokens = OuraTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresAt = Instant.now().epochSecond + response.expiresIn,
        )
        ConfigPaths.tokensFile.writeText(prettyJson.encodeToString(newTokens))
        newTokens
    } finally {
        http.close()
    }
}
