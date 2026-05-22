package io.github.nottaras.briefing.db

import io.github.nottaras.briefing.model.BriefingResult
import io.github.nottaras.briefing.model.HealthContext
import io.github.nottaras.briefing.model.ReadinessData
import io.github.nottaras.briefing.model.SleepData
import java.nio.file.Files
import java.time.LocalDate
import org.junit.jupiter.api.AfterEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BriefingRepositoryTest {

    private val tempDir = Files.createTempDirectory("oura-briefing-test")
    private val repo = BriefingRepository(dbPath = tempDir.resolve("history.db"))

    @AfterEach
    fun cleanup() {
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }

    @Test
    fun `save and findByDate round-trip`() {
        val date = LocalDate.of(2026, 5, 21)
        val result = BriefingResult(
            text = "Briefing text",
            context = HealthContext(
                date = date.toString(),
                sleep = SleepData(id = "s", day = date.toString(), score = 82),
                readiness = ReadinessData(id = "r", day = date.toString(), score = 77),
                cardiovascular = null,
            ),
        )

        repo.save(result)

        assertEquals("Briefing text", repo.findByDate(date))
        assertEquals(date, repo.lastCachedDate())
    }

    @Test
    fun `getRecentTrends returns averages from prior days`() {
        val day1 = LocalDate.of(2026, 5, 18)
        val day2 = LocalDate.of(2026, 5, 19)
        repo.save(briefing(day1, sleep = 70, readiness = 60))
        repo.save(briefing(day2, sleep = 80, readiness = 80))

        val trends = repo.getRecentTrends(before = LocalDate.of(2026, 5, 20))

        assertNotNull(trends)
        assertEquals(2, trends!!.days)
        assertEquals(75, trends.avgSleepScore!!.toInt())
        assertEquals(70, trends.avgReadinessScore!!.toInt())
    }

    @Test
    fun `listHistory returns rows newest first`() {
        repo.save(briefing(LocalDate.of(2026, 5, 10), sleep = 60, readiness = 60))
        repo.save(briefing(LocalDate.of(2026, 5, 12), sleep = 90, readiness = 90))

        val rows = repo.listHistory(days = 10)

        assertEquals(2, rows.size)
        assertEquals(LocalDate.of(2026, 5, 12), rows[0].date)
        assertEquals(90, rows[0].sleepScore)
    }

    @Test
    fun `findByDate returns null when missing`() {
        assertNull(repo.findByDate(LocalDate.of(2020, 1, 1)))
    }

    private fun briefing(date: LocalDate, sleep: Int, readiness: Int) = BriefingResult(
        text = "text",
        context = HealthContext(
            date = date.toString(),
            sleep = SleepData(id = "s", day = date.toString(), score = sleep),
            readiness = ReadinessData(id = "r", day = date.toString(), score = readiness),
            cardiovascular = null,
        ),
    )
}
