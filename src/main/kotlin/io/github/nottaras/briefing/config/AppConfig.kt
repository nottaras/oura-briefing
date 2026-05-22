package io.github.nottaras.briefing.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

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

object ConfigPaths {
    val configDir: Path = Path.of(System.getProperty("user.home"), ".config", "oura-briefing")
    val configFile: Path = configDir.resolve("config.toml")
    val tokensFile: Path = configDir.resolve("tokens.json")

    fun ensureConfigDirExists() = configDir.createDirectories()
}

fun loadConfig(): AppConfig {
    val path = ConfigPaths.configFile
    require(path.exists()) {
        "Config not found at $path\nRun: cp config.example.toml ${path}"
    }
    return ConfigLoaderBuilder.default()
        .addFileSource(path.toFile())
        .build()
        .loadConfigOrThrow()
}
