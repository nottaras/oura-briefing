package io.github.nottaras.briefing.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import io.github.nottaras.briefing.config.ConfigPaths
import io.github.nottaras.briefing.config.OuraTokens
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.exists
import kotlin.io.path.readText

private val lenientJson = Json { ignoreUnknownKeys = true }
private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

class StatusCommand : CliktCommand(name = "status") {

    override fun help(context: Context) = "Show authentication and token status"

    override fun run() {
        val configExists = ConfigPaths.configFile.exists()
        echo("Config:  ${if (configExists) "✓ ${ConfigPaths.configFile}" else "✗ not found — run: cp config.example.toml ${ConfigPaths.configFile}"}")

        if (!ConfigPaths.tokensFile.exists()) {
            echo("Tokens:  ✗ not found — run: briefing auth")
            return
        }

        val tokens = runCatching {
            lenientJson.decodeFromString<OuraTokens>(ConfigPaths.tokensFile.readText())
        }.getOrElse {
            echo("Tokens:  ✗ could not parse ${ConfigPaths.tokensFile}")
            return
        }

        val now = Instant.now().epochSecond
        val secsLeft = tokens.expiresAt - now
        val expiryStr = formatter.format(Instant.ofEpochSecond(tokens.expiresAt))

        val tokenStatus = when {
            secsLeft < 0 -> "✗ expired at $expiryStr"
            secsLeft < 60 -> "⚠ expiring at $expiryStr (will refresh on next run)"
            else -> "✓ valid until $expiryStr"
        }
        echo("Tokens:  $tokenStatus")
    }
}
