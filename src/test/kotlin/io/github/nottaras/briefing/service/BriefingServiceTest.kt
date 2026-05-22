package io.github.nottaras.briefing.service

import io.github.nottaras.briefing.client.ClaudeClient
import io.github.nottaras.briefing.client.OuraClient
import io.github.nottaras.briefing.model.CardiovascularData
import io.github.nottaras.briefing.model.ReadinessData
import io.github.nottaras.briefing.model.SleepData
import io.github.nottaras.briefing.model.TrendContext
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BriefingServiceTest {

    @Test
    fun `generateBriefing aggregates parallel fetches and calls Claude`() = runTest {
        val date = LocalDate.of(2026, 5, 20)
        val sleep = SleepData(id = "1", day = "2026-05-20", score = 80)
        val readiness = ReadinessData(id = "2", day = "2026-05-20", score = 75)
        val cardio = CardiovascularData(id = "3", day = "2026-05-20", vascularAge = 32)
        val trends = TrendContext(days = 3, avgSleepScore = 78.0, avgReadinessScore = 74.0)

        val oura = mockk<OuraClient>()
        coEvery { oura.getSleep(date) } returns sleep
        coEvery { oura.getReadiness(date) } returns readiness
        coEvery { oura.getCardiovascular(date) } returns cardio

        val claude = mockk<ClaudeClient>()
        coEvery { claude.generateBriefing(any(), trends) } returns "😴 Good sleep"

        val result = BriefingService(oura, claude).generateBriefing(date, trends)

        assertEquals("😴 Good sleep", result.text)
        assertEquals("2026-05-20", result.context.date)
        assertEquals(80, result.context.sleep?.score)
        assertEquals(75, result.context.readiness?.score)
        assertEquals(32, result.context.cardiovascular?.vascularAge)
    }
}
