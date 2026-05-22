package io.github.nottaras.briefing.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

class RootCommand : CliktCommand(name = "briefing") {
    override fun help(context: Context) = "Personal morning health briefing powered by Oura + Claude"

    init {
        subcommands(AuthCommand(), RunCommand())
    }

    override fun run() = Unit
}
