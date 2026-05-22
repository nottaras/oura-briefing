package io.github.nottaras.briefing.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class RootCommand : CliktCommand(name = "briefing") {
    override fun help(context: Context) = "Personal morning health briefing powered by Oura + Claude"
    override fun run() = Unit
}

// Register subcommands here as they are implemented:
// RootCommand().subcommands(AuthCommand(), RunCommand(), StatusCommand())
