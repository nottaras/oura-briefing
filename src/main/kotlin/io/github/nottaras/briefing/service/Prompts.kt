package io.github.nottaras.briefing.service

import io.github.nottaras.briefing.model.HealthContext
import io.github.nottaras.briefing.model.TrendContext
import kotlin.math.roundToInt

object Prompts {

    val system = """
        You are a personal health coach. Every morning you receive biometric data
        from the user's Oura Ring and produce a concise morning briefing in English.

        Format your response exactly like this:
        😴 [One sentence on sleep quality — reference actual numbers]
        ⚡ [One sentence on readiness and HRV]
        🎯 [One concrete recommendation for today based on the data]
        👀 [One thing to watch or be aware of today]

        Rules:
        - Be direct, not generic. Always reference the actual numbers.
        - If trend data is provided, compare today's scores to the recent average and note whether things are improving, declining, or stable.
        - If a score is missing, acknowledge it gracefully.
        - No fluff, no motivational filler. Just facts and one clear action.
        - Keep the total response under 150 words.
    """.trimIndent()

    fun userMessage(context: HealthContext, trends: TrendContext? = null): String = buildString {
        appendLine("Date: ${context.date}")
        appendLine()

        context.sleep?.let { s ->
            appendLine("SLEEP:")
            s.score?.let { appendLine("  Score: $it/100") }
            s.totalSleepDuration?.let { appendLine("  Total: ${it.toHoursMinutes()}") }
            s.deepSleepDuration?.let { appendLine("  Deep: ${it.toHoursMinutes()}") }
            s.remSleepDuration?.let { appendLine("  REM: ${it.toHoursMinutes()}") }
            s.efficiency?.let { appendLine("  Efficiency: $it%") }
            s.latency?.let { appendLine("  Latency: ${it / 60} min") }
        } ?: appendLine("SLEEP: no data")

        appendLine()

        context.readiness?.let { r ->
            appendLine("READINESS:")
            r.score?.let { appendLine("  Score: $it/100") }
            r.restingHeartRate?.let { appendLine("  Resting HR: $it bpm") }
            r.temperatureDeviation?.let { appendLine("  Temp deviation: ${String.format("%.2f", it)}°C") }
            r.contributors?.let { c ->
                appendLine("  Contributors:")
                c.hrvBalance?.let { appendLine("    HRV balance: $it") }
                c.sleepBalance?.let { appendLine("    Sleep balance: $it") }
                c.activityBalance?.let { appendLine("    Activity balance: $it") }
                c.bodyTemperature?.let { appendLine("    Body temp: $it") }
            }
        } ?: appendLine("READINESS: no data")

        appendLine()

        context.cardiovascular?.let { c ->
            appendLine("CARDIOVASCULAR:")
            c.vascularAge?.let { appendLine("  Vascular age: $it") }
        } ?: appendLine("CARDIOVASCULAR: no data")

        trends?.let { t ->
            appendLine()
            appendLine("TREND (last ${t.days} days):")
            t.avgSleepScore?.let { appendLine("  Avg sleep score: ${it.roundToInt()}/100") }
            t.avgReadinessScore?.let { appendLine("  Avg readiness score: ${it.roundToInt()}/100") }
        }
    }

    private fun Int.toHoursMinutes(): String {
        val h = this / 3600
        val m = (this % 3600) / 60
        return "${h}h ${m}m"
    }
}
