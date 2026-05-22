# oura-briefing

Personal morning health briefing powered by Oura Ring + Claude.

```
$ briefing run
😴 Ты спал 7ч 20м, score 82 — глубокого сна чуть меньше нормы (1ч 10м).
⚡ Readiness 74, HRV баланс слегка снижен — тело ещё восстанавливается.
🎯 Сегодня лёгкая активность, не форсируй — пробежка или прогулка.
👀 Температура тела +0.3°C — следи за самочувствием в течение дня.
```

## Setup

### 1. Oura OAuth App

Register at [cloud.ouraring.com/oauth/applications](https://cloud.ouraring.com/oauth/applications)  
Set redirect URI: `http://localhost:8080/callback`

### 2. Anthropic API Key

Get at [console.anthropic.com](https://console.anthropic.com)

### 3. Config

```bash
mkdir -p ~/.config/oura-briefing
cp config.example.toml ~/.config/oura-briefing/config.toml
# Edit with your client_id, client_secret, api_key
```

### 4. Auth (first time only)

```bash
./gradlew shadowJar
java -jar build/libs/briefing.jar auth
# Opens browser → login with Oura → done
```

### 5. Run

```bash
java -jar build/libs/briefing.jar run
```

### Alias (optional)

```bash
echo "alias briefing='java -jar ~/tools/briefing.jar'" >> ~/.zshrc
```

## Architecture

```
CLI (clikt)
    └── BriefingService        # client-agnostic core logic
            ├── OuraClient     # Oura API + OAuth2
            ├── ClaudeClient   # Anthropic API
            └── (SQLite)       # local cache — phase 2
```

`BriefingService` returns plain data — CLI, Telegram bot, or any other 
client can consume it without changes to business logic.

## Roadmap

- [x] Project structure
- [ ] OAuth2 flow (`briefing auth`)
- [ ] Oura API — sleep, readiness, HRV
- [ ] Claude API — generate briefing
- [ ] `briefing run` command with mordant output
- [ ] SQLite history cache
- [ ] 30-day trends in briefing
- [ ] Telegram bot client
