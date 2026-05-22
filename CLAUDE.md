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

**OAuth callback** is handled by a temporary Ktor server on `localhost:8080` (see `Routing.kt` and `application.yaml`).

## What's Implemented vs. Stubbed

**Working:**
- `BriefingService` — parallel fetching and aggregation logic
- `Prompts` — system prompt and `userMessage()` formatter (output is in Russian, emoji-prefixed sections)
- All data models in `HealthModels.kt`
- Config loading (`AppConfig`, `ConfigPaths`)

**Stubbed (return null/TODO):**
- `OuraClient.getSleep/getReadiness/getCardiovascular` — the generic `get<T>()` helper is ready; endpoint strings just need filling in
- `ClaudeClient.generateBriefing` — DTOs are defined; actual HTTP POST needs implementing
- `RootCommand` — no subcommands registered yet (planned: `auth`, `run`, `status`)
- `Routing.kt` — `/callback` route for OAuth code capture not yet added

See `.githbub/ISSUES.md` for the prioritized implementation plan (#1–#8).

## Key Notes

- **Package**: `io.github.nottaras.briefing` throughout — all source files, `build.gradle.kts` (`group` and `mainClass`), and `application.yaml`.
- **Oura API base URL**: `https://api.ouraring.com/v2/usercollection`
- **Claude model default**: `claude-sonnet-4-5-20251001` (configurable in `config.toml`)
- `OuraClient` expects a valid bearer token; token refresh logic goes in the client before the generic `get<T>()` call.
