package io.github.nottaras.briefing.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.nottaras.briefing.client.ClaudeClient
import io.github.nottaras.briefing.client.OuraClient
import io.github.nottaras.briefing.config.loadConfig
import io.github.nottaras.briefing.config.loadValidTokens
import io.github.nottaras.briefing.service.BriefingService
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class RunCommand : CliktCommand(name = "run") {

    private val date by option("--date", help = "Date to generate briefing for (YYYY-MM-DD, default: today)")
        .convert { LocalDate.parse(it) }
        .default(LocalDate.now())

    override fun help(context: Context) = "Generate a morning health briefing"

    override fun run() = runBlocking {
        val config = loadConfig()
        val tokens = loadValidTokens(config.oura)

        val oura = OuraClient(tokens.accessToken)
        val claude = ClaudeClient(config.anthropic.apiKey, config.anthropic.model)
        try {
            val result = BriefingService(oura, claude).generateBriefing(date)
            echo(result.text)
        } finally {
            oura.close()
            claude.close()
        }
    }
}
