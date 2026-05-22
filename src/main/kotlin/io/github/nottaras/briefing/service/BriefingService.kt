package io.github.nottaras.briefing.service

import io.github.nottaras.briefing.client.ClaudeClient
import io.github.nottaras.briefing.client.OuraClient
import io.github.nottaras.briefing.model.BriefingResult
import io.github.nottaras.briefing.model.HealthContext
import io.github.nottaras.briefing.model.TrendContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

class BriefingService(
    private val oura: OuraClient,
    private val claude: ClaudeClient,
) {
    // Fetch all health data in parallel, then ask Claude to analyze
    // This is the core method — works the same for CLI, Telegram, or any other client
    suspend fun generateBriefing(date: LocalDate = LocalDate.now(), trends: TrendContext? = null): BriefingResult = coroutineScope {
        // Parallel fetch — all three requests fire at once
        val sleepDeferred = async { oura.getSleep(date) }
        val readinessDeferred = async { oura.getReadiness(date) }
        val cardiovascularDeferred = async { oura.getCardiovascular(date) }

        val context = HealthContext(
            date = date.toString(),
            sleep = sleepDeferred.await(),
            readiness = readinessDeferred.await(),
            cardiovascular = cardiovascularDeferred.await(),
        )

        val text = claude.generateBriefing(context, trends)
        BriefingResult(text = text, context = context)
    }
}
