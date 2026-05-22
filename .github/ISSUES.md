# MVP Issues — Status

## Done

### #1 — OAuth2: `briefing auth`
- `AuthCommand`: browser, Netty callback on `:8080`, token exchange, `tokens.json`
- OAuth denial (`?error=`) and bind failures handled; server always stopped in `finally`

### #2 — Token refresh
- `loadValidTokens()` with 60s skew; `TokenExpiredException` on revoked refresh

### #3 — Oura API
- `getSleep` / `getReadiness` / `getCardiovascular`; errors surface as `OuraApiException`

### #4 — Claude API
- Full messages API; first `text` content block; `Prompts` system + user message

### #5 — `briefing run`
- `--date`, `--force`, mordant spinner + panel, `UsageError` for auth/API failures

### #6 — `briefing status`
- Config, token expiry, last cached date

### #7 — SQLite history
- `BriefingRepository`, `history.db`, `briefing history --days N`

### #8 — Trend context
- 7-day averages from cache passed into Claude prompt

## Backlog (not MVP)

- Telegram bot client
- Flyway migrations instead of `createMissingTablesAndColumns`
- Configurable OAuth callback port (must match Oura app redirect URI)
