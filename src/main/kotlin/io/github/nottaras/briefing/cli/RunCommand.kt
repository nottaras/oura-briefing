package io.github.nottaras.briefing.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.nottaras.briefing.client.ClaudeClient
import io.github.nottaras.briefing.client.OuraApiException
import io.github.nottaras.briefing.client.OuraClient
import io.github.nottaras.briefing.config.TokenExpiredException
import io.github.nottaras.briefing.config.loadConfig
import io.github.nottaras.briefing.config.loadValidTokens
import io.github.nottaras.briefing.db.BriefingRepository
import io.github.nottaras.briefing.service.BriefingService
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class RunCommand : CliktCommand(name = "run") {

    private val date by option("--date", help = "Date to generate briefing for (YYYY-MM-DD, default: today)")
        .convert { LocalDate.parse(it) }
        .default(LocalDate.now())

    private val force by option("--force", help = "Regenerate even if a cached briefing exists")
        .flag()

    override fun help(context: Context) = "Generate a morning health briefing"

    override fun run() = runBlocking {
        val repo = BriefingRepository()

        if (!force) {
            repo.findByDate(date)?.let { cached ->
                TerminalUi.printBriefing(cached, date, cached = true)
                return@runBlocking
            }
        }

        val trends = repo.getRecentTrends(before = date)

        try {
            val config = loadConfig()
            val tokens = loadValidTokens(config.oura)
            val oura = OuraClient(tokens.accessToken)
            val claude = ClaudeClient(config.anthropic.apiKey, config.anthropic.model)
            try {
                val result = TerminalUi.withSpinnerBlocking("Fetching Oura data & generating briefing…") {
                    BriefingService(oura, claude).generateBriefing(date, trends)
                }
                repo.save(result)
                TerminalUi.printBriefing(result.text, date)
            } finally {
                oura.close()
                claude.close()
            }
        } catch (e: TokenExpiredException) {
            throw UsageError(e.message ?: "Oura session expired")
        } catch (e: OuraApiException) {
            throw UsageError(e.message ?: "Oura API error")
        }
    }
}
