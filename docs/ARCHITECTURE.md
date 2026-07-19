# Architecture

## Overview

Second Brain System is a local-first, single-user PKM application.
The Go backend serves a REST API on localhost, reading and writing
Markdown files in a configurable vault directory. The Android app
is a Kotlin Jetpack Compose client that communicates via HTTP/JSON.

## Vault Structure

All data lives in a user-configurable directory (`VAULT_PATH`).
Default: `~/second-brain/vault/`.

```
vault/
  notes/          # Note entities
  tasks/          # Task entities
  quick-tasks/    # Quick Task entities
  people/         # Person entities
  archive/        # Archived entities
```

Each entity is a single `.md` file named `{uuid}.md`.

## Entity Schemas (YAML Frontmatter)

### Note

```yaml
---
id: "550e8400-e29b-41d4-a716-446655440000"
type: "note"
title: "My Note"
status: "active"           # active | archived
tags: ["idea", "reference"]
links: ["<entity-uuid>"]
created_at: "2025-01-01T10:00:00Z"
updated_at: "2025-01-01T10:00:00Z"
---
Markdown content here...
```

### Task

```yaml
---
id: "550e8400-e29b-41d4-a716-446655440001"
type: "task"
title: "Write architecture doc"
status: "pending"           # pending | in-progress | completed | expired
icon: "edit-note"           # Material icon name or similar
location: "Home office"
tags: ["documentation"]
links: ["<note-uuid>", "<person-uuid>"]
start_date: "2025-01-01T00:00:00Z"
end_date: "2025-01-10T23:59:59Z"
recurrence:
  type: "weekly"            # daily | weekly | monthly | yearly
  interval: 1               # every N days/weeks/months/years
  days_of_week: [1,3]       # only for weekly: 0=Sun, 1=Mon...
subtasks:
  - id: "sub-uuid-1"
    title: "Research"
    completed: true
  - id: "sub-uuid-2"
    title: "Write"
    completed: false
created_at: "2025-01-01T10:00:00Z"
updated_at: "2025-01-01T10:00:00Z"
---
Optional description in markdown...
```

### Quick Task

```yaml
---
id: "550e8400-e29b-41d4-a716-446655440002"
type: "quick-task"
title: "Buy milk"
status: "pending"           # pending | completed
created_at: "2025-01-01T10:00:00Z"
---
```

Quick Tasks have no markdown body. They are created from the dashboard
with just a title. When marked completed, they are deleted after 5 seconds.

### Person

```yaml
---
id: "550e8400-e29b-41d4-a716-446655440003"
type: "person"
name: "John Doe"
status: "active"            # active | archived
contacts:
  - type: "phone"           # phone | email | social
    value: "+90 555 123 4567"
    label: "Personal"
  - type: "email"
    value: "john@example.com"
    label: "Work"
social_links:
  - platform: "github"
    url: "https://github.com/johndoe"
  - platform: "twitter"
    url: "https://twitter.com/johndoe"
tags: ["friend", "work"]
links: ["<task-uuid>", "<note-uuid>"]
notes: "Met at conference. Interested in PKM systems."
created_at: "2025-01-01T10:00:00Z"
updated_at: "2025-01-01T10:00:00Z"
---
```

## API Design

Base path: `/api/v1`

### Endpoints

| Method | Path | Description |
|---|---|---|
| GET | /notes | List all notes |
| GET | /notes/:id | Get note by ID |
| POST | /notes | Create note |
| PUT | /notes/:id | Update note |
| DELETE | /notes/:id | Delete note |
| GET | /tasks | List all tasks |
| GET | /tasks/:id | Get task by ID |
| POST | /tasks | Create task |
| PUT | /tasks/:id | Update task |
| DELETE | /tasks/:id | Delete task |
| GET | /quick-tasks | List all quick tasks |
| POST | /quick-tasks | Create quick task |
| PUT | /quick-tasks/:id | Mark complete (handles auto-delete) |
| DELETE | /quick-tasks/:id | Delete quick task |
| GET | /people | List all people |
| GET | /people/:id | Get person by ID |
| POST | /people | Create person |
| PUT | /people/:id | Update person |
| DELETE | /people/:id | Delete person |
| GET | /search?q=... | Full-text search across all entities |

### Response Format

All responses follow a consistent envelope:

```json
{
  "data": { ... },
  "error": ""
}
```

Errors include appropriate HTTP status codes:

| Status | Meaning |
|---|---|
| 200 | Success |
| 400 | Bad request (invalid input) |
| 404 | Entity not found |
| 409 | Conflict (e.g. duplicate ID) |
| 500 | Internal server error |

## Recurrence System

When a Task with `recurrence` is marked `completed`, the backend:

1. Creates a new Task with a fresh UUID
2. Copies all fields (title, subtasks, icon, location, etc.)
3. Resets subtask completion statuses to `false`
4. Calculates new start/end dates based on recurrence config
5. Marks the original task as `expired` (not `completed`)
6. Returns the new task ID in the response

Recurrence is evaluated lazily -- on API request, not via cron.
When listing tasks, the backend checks if any recurring tasks
need to spawn new instances based on their end_date.

## File Operations & Concurrency

- All vault file operations use `os.ReadFile` / `os.WriteFile`
- Write operations use a per-file mutex to prevent corruption
- YAML frontmatter is parsed with `gopkg.in/yaml.v3`
- Markdown body is everything after the `---\n` closing frontmatter delimiter
- File writes are atomic: write to a temp file, then rename

## Search

MVP search is simple:
- Backend loads all vault files
- Parses frontmatter and body
- Performs case-insensitive substring match across title, tags, and body
- Returns matching entities with their type and ID

Future: full-text index with bleed and relevance scoring.

## Android Architecture

### MVI Pattern

```
UI (Compose) --events--> ViewModel --state--> UI (Compose)
                   |                          ^
                   v                          |
                Repository --> API --> Backend
```

- Each screen has a ViewModel with `StateFlow<UiState>`
- Events are sealed classes (e.g. `NoteEvent.Save`, `NoteEvent.Delete`)
- Side effects (navigation, toasts) use `SharedFlow<Effect>`

### Dependency Injection

Manual DI via a simple `AppModule` object (no Hilt/Dagger for MVP).
Provides:
- Ktor HttpClient
- API service instances
- Repository instances

### Navigation

Simple sealed class-based navigation with a `NavHost`-like composable.
No third-party navigation library for MVP.

### Networking

- Ktor HttpClient with content negotiation (JSON)
- `kotlinx.serialization` for DTOs
- Base URL: `http://localhost:8080/api/v1` (configurable)
- Connect timeout: 5s, request timeout: 30s

## Future Considerations

- File watching with fsnotify for hot-reload of vault changes
- Git-based sync or WebDAV
- Full text search with bleve
- WikiLink resolution (`[[title]]` -> entity lookup)
- Archived entity restoration from archive folder
- Offline mode with local vault copy
- Backup to GitHub
