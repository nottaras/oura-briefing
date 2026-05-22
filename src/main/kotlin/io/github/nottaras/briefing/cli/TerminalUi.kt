package io.github.nottaras.briefing.cli

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Panel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDate

object TerminalUi {
    private val terminal = Terminal()
    private val spinnerFrames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")

    fun printBriefing(text: String, date: LocalDate, cached: Boolean = false) {
        val title = buildString {
            append("Morning Briefing — $date")
            if (cached) append(" (cached)")
        }
        terminal.println(
            Panel(
                content = text,
                title = title,
                expand = true,
            )
        )
    }

    fun <T> withSpinnerBlocking(label: String, block: suspend () -> T): T = runBlocking {
        val spinner = Thread {
            var frame = 0
            while (!Thread.currentThread().isInterrupted) {
                terminal.print("\r$label ${spinnerFrames[frame++ % spinnerFrames.size]}")
                Thread.sleep(80)
            }
        }
        spinner.start()
        try {
            withContext(Dispatchers.Default) { block() }
        } finally {
            spinner.interrupt()
            spinner.join()
            terminal.println("\r$label ✓")
        }
    }
}
