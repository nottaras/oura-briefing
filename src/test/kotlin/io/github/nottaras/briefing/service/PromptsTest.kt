package io.github.nottaras.briefing.service

import io.github.nottaras.briefing.model.HealthContext
import io.github.nottaras.briefing.model.TrendContext
import kotlin.test.Test
import kotlin.test.assertTrue

class PromptsTest {

    @Test
    fun `userMessage includes trend section when trends provided`() {
        val message = Prompts.userMessage(
            HealthContext(date = "2026-05-22", sleep = null, readiness = null, cardiovascular = null),
            TrendContext(days = 5, avgSleepScore = 81.4, avgReadinessScore = 72.6),
        )

        assertTrue(message.contains("TREND (last 5 days):"))
        assertTrue(message.contains("Avg sleep score: 81/100"))
        assertTrue(message.contains("Avg readiness score: 73/100"))
    }
}
