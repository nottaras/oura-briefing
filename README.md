# oura-briefing

Fetches your Oura Ring data each morning and generates a concise health briefing via Claude.

```
$ briefing run
😴 You slept well with a 78/100 score, though deep sleep was slightly below average.
⚡ Readiness at 79/100 — HRV balance (73) is the weak link, suggesting mild fatigue.
🎯 Light movement this morning; your activity balance is strong enough for a harder session later.
👀 Temp deviation +0.20°C is minor — watch for fatigue mid-afternoon.
```

## Setup

**1. Create an Oura OAuth app** at [cloud.ouraring.com/oauth/applications](https://cloud.ouraring.com/oauth/applications) with redirect URI `http://localhost:8080/callback`.

**2. Get an Anthropic API key** at [console.anthropic.com](https://console.anthropic.com).

**3. Configure:**
```bash
cp config.example.toml ~/.config/oura-briefing/config.toml
# fill in client_id, client_secret, api_key
```

**4. Authenticate (once):**
```bash
./gradlew shadowJar
java -jar build/libs/briefing.jar auth
```

**5. Run:**
```bash
java -jar build/libs/briefing.jar run
```

## Commands

| Command | Description |
|---|---|
| `briefing auth` | OAuth2 flow — opens browser, saves tokens |
| `briefing run` | Generate today's briefing (cached after first run) |
| `briefing run --date 2026-05-20` | Briefing for a specific date |
| `briefing run --force` | Regenerate even if cached |
| `briefing status` | Show token validity and last cached date |
| `briefing history --days 30` | Table of cached sleep/readiness scores |
