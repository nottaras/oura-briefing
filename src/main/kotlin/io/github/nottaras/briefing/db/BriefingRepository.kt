package io.github.nottaras.briefing.db

import io.github.nottaras.briefing.config.ConfigPaths
import io.github.nottaras.briefing.model.BriefingResult
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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

class BriefingRepository {
    init {
        val dbPath = ConfigPaths.configDir.resolve("history.db")
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
}
