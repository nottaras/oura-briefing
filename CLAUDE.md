# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build fat JAR
./gradlew shadowJar          # outputs build/libs/briefing.jar

# Run tests
./gradlew test
./gradlew test --tests "ServerTest"   # single test class

# Run directly (no JAR)
./gradlew run --args="<subcommand>"

# Clean
./gradlew clean
```

## Architecture

Personal health briefing tool: fetches Oura Ring data → summarizes via Claude API → outputs a Russian-language morning briefing.

```
CLI (Clikt)
└── RootCommand
    └── BriefingService            ← core logic, client-agnostic
        ├── OuraClient             ← OAuth2 + Oura v2 API
        └── ClaudeClient           ← Anthropic messages API
```

**Data flow:** `BriefingService.generateBriefing(date)` launches three parallel Oura API calls (`async`), aggregates results into `HealthContext`, formats with `Prompts.userMessage()`, passes to `ClaudeClient`, and returns `BriefingResult`.

**Config** lives at `~/.config/oura-briefing/config.toml` (Hoplite TOML). OAuth tokens are persisted to `~/.config/oura-briefing/tokens.json` as `OuraTokens`. See `config.example.toml` for the required fields.

**OAuth callback** is handled by an inline `embeddedServer(Netty, port = 8080)` inside `AuthCommand` — no separate `Routing.kt` or `application.yaml`.

**Token refresh** is in `AppConfig.loadValidTokens()` — refreshes automatically if `expiresAt - now < 60s`.

## CLI Usage

```bash
briefing auth   # OAuth2 browser flow, saves tokens.json
briefing run    # generate today's briefing
briefing run --date 2026-05-21
```

## Key Notes

- **Package**: `io.github.nottaras.briefing` throughout — all source files, `build.gradle.kts` (`group` and `mainClass`).
- **Oura API base URL**: `https://api.ouraring.com/v2/usercollection`
- **Claude model**: `claude-sonnet-4-6` (configurable in `config.toml`, see `config.example.toml`)
- Both `OuraClient` and `ClaudeClient` use `expectSuccess = false` — check status before deserializing body.
- `BriefingResult.text` holds the final string (not `.briefing`).

See `.githbub/ISSUES.md` for planned features (#6–#8): SQLite history, 30-day trends, Telegram bot.
