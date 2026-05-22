# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build fat JAR
./gradlew shadowJar          # outputs build/libs/briefing.jar

# Run tests
./gradlew test
./gradlew test --tests "BriefingServiceTest"

# Run directly (no JAR)
./gradlew run --args="run"

# Clean
./gradlew clean
```

## Architecture

Personal health briefing tool: fetches Oura Ring data → summarizes via Claude API → outputs an English morning briefing in the terminal.

```
CLI (Clikt)
└── RootCommand
    ├── AuthCommand / RunCommand / StatusCommand / HistoryCommand
    └── BriefingService            ← core logic, client-agnostic
        ├── OuraClient             ← Oura v2 API (throws OuraApiException on failure)
        ├── ClaudeClient           ← Anthropic messages API
        └── BriefingRepository     ← SQLite cache + trend averages
```

**Data flow:** `BriefingService.generateBriefing(date, trends)` launches three parallel Oura API calls (`async`), aggregates into `HealthContext`, formats with `Prompts.userMessage()`, passes to `ClaudeClient`, returns `BriefingResult`. `RunCommand` saves to SQLite and prints via `TerminalUi` (mordant panel + spinner).

**Config** lives at `~/.config/oura-briefing/config.toml` (Hoplite TOML). OAuth tokens in `~/.config/oura-briefing/tokens.json`. Override config dir for tests: `OURA_BRIEFING_CONFIG_DIR`.

**OAuth callback** is an inline `embeddedServer(Netty, port = 8080)` in `AuthCommand` (`OAuthConstants`).

**Token refresh** is in `loadValidTokens()` — refreshes when `expiresAt - now < 60s`; `TokenExpiredException` prompts re-auth.

## CLI Usage

```bash
briefing auth
briefing run
briefing run --date 2026-05-21
briefing run --force
briefing status
briefing history --days 30
```

## Key Notes

- **Package**: `io.github.nottaras.briefing`
- **Oura API base URL**: `https://api.ouraring.com/v2/usercollection`
- **Claude model default**: `claude-sonnet-4-6` (see `config.example.toml`)
- `OuraClient` and `ClaudeClient` use `expectSuccess = false` — check status before parsing body.
- Empty Oura `data` array → `null` (no ring data that day); HTTP errors → `OuraApiException`.
