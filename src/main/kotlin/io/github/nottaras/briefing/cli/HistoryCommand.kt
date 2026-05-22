package io.github.nottaras.briefing.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import io.github.nottaras.briefing.db.BriefingRepository

class HistoryCommand : CliktCommand(name = "history") {

    private val days by option("--days", help = "Number of recent days to show (default: 30)")
        .convert { it.toInt() }
        .default(30)

    override fun help(context: Context) = "Show cached briefing metrics"

    override fun run() {
        val rows = BriefingRepository().listHistory(days)
        if (rows.isEmpty()) {
            echo("No cached briefings yet. Run `briefing run` first.")
            return
        }

        val t = Terminal()
        t.println(
            table {
                header {
                    row("Date", "Sleep", "Readiness", "Vascular age")
                }
                body {
                    rows.forEach { entry ->
                        row(
                            entry.date.toString(),
                            entry.sleepScore?.toString() ?: "—",
                            entry.readinessScore?.toString() ?: "—",
                            entry.vascularAge?.toString() ?: "—",
                        )
                    }
                }
            }
        )
    }
}
