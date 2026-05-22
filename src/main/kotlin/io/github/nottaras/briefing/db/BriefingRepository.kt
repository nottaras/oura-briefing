package io.github.nottaras.briefing.db

import io.github.nottaras.briefing.config.ConfigPaths
import io.github.nottaras.briefing.model.BriefingResult
import io.github.nottaras.briefing.model.TrendContext
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate

object Briefings : Table("briefings") {
    val date = text("date")
    val text = text("text")
    val sleepScore = integer("sleep_score").nullable()
    val readinessScore = integer("readiness_score").nullable()
    val vascularAge = integer("vascular_age").nullable()
    val createdAt = text("created_at")
    override val primaryKey = PrimaryKey(date)
}

data class HistoryRow(
    val date: LocalDate,
    val sleepScore: Int?,
    val readinessScore: Int?,
    val vascularAge: Int?,
)

class BriefingRepository(
    private val dbPath: Path = ConfigPaths.configDir.resolve("history.db"),
) {
    init {
        Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")
        transaction { SchemaUtils.createMissingTablesAndColumns(Briefings) }
    }

    fun findByDate(date: LocalDate): String? = transaction {
        Briefings.selectAll()
            .where { Briefings.date eq date.toString() }
            .firstOrNull()
            ?.get(Briefings.text)
    }

    fun save(result: BriefingResult) = transaction {
        Briefings.upsert {
            it[date] = result.context.date
            it[text] = result.text
            it[sleepScore] = result.context.sleep?.score
            it[readinessScore] = result.context.readiness?.score
            it[vascularAge] = result.context.cardiovascular?.vascularAge
            it[createdAt] = Instant.now().toString()
        }
    }

    fun lastCachedDate(): LocalDate? = transaction {
        Briefings.selectAll()
            .orderBy(Briefings.date, SortOrder.DESC)
            .firstOrNull()
            ?.get(Briefings.date)
            ?.let { LocalDate.parse(it) }
    }

    fun getRecentTrends(before: LocalDate, days: Int = 7): TrendContext? = transaction {
        val rows = Briefings.selectAll()
            .where { Briefings.date less before.toString() }
            .orderBy(Briefings.date, SortOrder.DESC)
            .limit(days)
            .toList()
        if (rows.isEmpty()) return@transaction null
        TrendContext(
            days = rows.size,
            avgSleepScore = rows.mapNotNull { it[Briefings.sleepScore] }.average().takeIf { it.isFinite() },
            avgReadinessScore = rows.mapNotNull { it[Briefings.readinessScore] }.average().takeIf { it.isFinite() },
        )
    }

    fun listHistory(days: Int = 30): List<HistoryRow> = transaction {
        Briefings.selectAll()
            .orderBy(Briefings.date, SortOrder.DESC)
            .limit(days)
            .map { row ->
                HistoryRow(
                    date = LocalDate.parse(row[Briefings.date]),
                    sleepScore = row[Briefings.sleepScore],
                    readinessScore = row[Briefings.readinessScore],
                    vascularAge = row[Briefings.vascularAge],
                )
            }
    }
}
