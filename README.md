# Second Brain System

Lightweight, Markdown-first Personal Knowledge Management (PKM) system.
Backend runs on Termux (Go), frontend is a Kotlin Jetpack Compose Android app.

## Philosophy

Markdown-First. All data is human-readable .md files with YAML frontmatter.
No SQL databases. No proprietary formats. Your data is your own.

## Entities

| Entity | Description |
|---|---|
| Note | Free-form markdown notes for ideas, references, resources |
| Task | Full-featured tasks with icon, location, description, subtasks, dates, and optional recurrence |
| Quick Task | Minimal tasks created from dashboard -- title only, auto-deletes 5 seconds after completion |
| Person | Contact/OSINT profiles with social links, contact info, notes, and entity relations |

All entities support `#tags` and `[[wikilinks]]` for cross-referencing.

## Architecture Overview

```
Android (Ktor Client) <--> HTTP/JSON <--> Go API (echo) <--> Vault (.md files)
```

- Backend runs locally in Termux (Go HTTP server on localhost)
- Android app connects to localhost API
- Vault is a directory of .md files managed by the backend
- Android does not access the vault directly -- only through the API

## MVP Feature Order

1. Notes (CRUD + markdown content)
2. Tasks (full features: subtasks, recurrence, dates, icon, location)
3. Quick Tasks (dashboard creation, auto-delete on completion)
4. People (profiles, contacts, social links, relations)

## Stack

- **Backend:** Go, echo framework, yaml.v3, fsnotify
- **Frontend:** Kotlin, Jetpack Compose, Material3, MVI, Ktor
- **Data:** Markdown (.md) with YAML frontmatter
- **Auth:** None (single-user, localhost only)

## Getting Started

```bash
# Backend
cd backend
export SECOND_BRAIN_VAULT_PATH=~/vault
export SECOND_BRAIN_PORT=8080
go run ./cmd/server

# Frontend (separate terminal)
cd android
./gradlew assembleDebug
```

## License

MIT
