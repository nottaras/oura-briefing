# MVP Issues

Создай эти issues на GitHub после пуша репо.

---

## Issue #1 — OAuth2: `briefing auth` command
**Labels:** `mvp`, `auth`

Реализовать полный OAuth2 Authorization Code flow:
- Команда `briefing auth` в CLI
- Открыть браузер на Oura authorize URL
- Поднять временный Ktor Netty сервер на localhost:8080
- Перехватить `?code=` из redirect
- Обменять code на access_token + refresh_token через POST
- Сохранить токены в `~/.config/oura-briefing/tokens.json`
- Остановить локальный сервер

**Acceptance:** `briefing auth` завершается успешно, tokens.json создан

---

## Issue #2 — Token refresh
**Labels:** `mvp`, `auth`
**Depends on:** #1

- При старте читать tokens.json
- Проверять `expiresAt` — если протух, тихо рефрешить
- Обновлять tokens.json с новыми значениями
- Если refresh тоже протух — просить запустить `briefing auth`

**Acceptance:** повторный запуск через 24ч работает без браузера

---

## Issue #3 — Oura API: fetch today's data
**Labels:** `mvp`, `oura`
**Depends on:** #2

Реализовать три метода в `OuraClient`:
- `getSleep(date)` → `/v2/usercollection/daily_sleep`
- `getReadiness(date)` → `/v2/usercollection/daily_readiness`
- `getCardiovascular(date)` → `/v2/usercollection/daily_cardiovascular_age`

Параллельный вызов через `async/await` уже в `BriefingService`.

**Acceptance:** `HealthContext` заполнен реальными данными

---

## Issue #4 — Claude API: generate briefing
**Labels:** `mvp`, `claude`
**Depends on:** #3

Реализовать `ClaudeClient.generateBriefing()`:
- POST на `https://api.anthropic.com/v1/messages`
- Заголовки: `x-api-key`, `anthropic-version: 2023-06-01`
- Body: model, max_tokens=400, system prompt из Prompts.kt, user message с данными
- Парсить ответ → вернуть text

**Acceptance:** получаем осмысленный briefing на реальных данных

---

## Issue #5 — `briefing run` CLI command
**Labels:** `mvp`, `cli`
**Depends on:** #4

Реализовать команду `briefing run`:
- Опция `--date` (default: сегодня)
- Показать spinner пока идут запросы (mordant)
- Вывести результат красиво через mordant (panel с заголовком, цветные иконки)
- Обработать ошибки: нет токена → подсказать `briefing auth`

**Acceptance:** `briefing run` выводит briefing в терминале

---

## Issue #6 — `briefing status` command
**Labels:** `nice-to-have`

Показать текущее состояние:
- Авторизован? Токен протухает когда?
- Последний успешный запрос к Oura
- Версия приложения

---

## Issue #7 — SQLite history cache
**Labels:** `phase-2`

Локальный кэш данных по дням:
- Exposed ORM + SQLite
- Сохранять HealthContext после каждого успешного fetch
- `briefing history --days 30` — показать таблицу метрик

---

## Issue #8 — Trend context in briefing
**Labels:** `phase-2`
**Depends on:** #7

Передавать в Claude последние 7 дней как контекст.
Тогда Claude может замечать тренды: "HRV снижается третий день подряд".
