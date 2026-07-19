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
  notes/          # Note entities (.md files named {uuid}.md)
  tasks/          # Task entities
  quick-tasks/    # Quick Task entities
  people/         # Person entities
  archive/        # Archived/deleted entities
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
icon: "edit-note"
location: "Home office"
tags: ["documentation"]
links: ["<note-uuid>"]
start_date: "2025-01-01T00:00:00Z"
end_date: "2025-01-10T23:59:59Z"
recurrence:
  type: "weekly"            # daily | weekly | monthly | yearly
  interval: 1               # every N periods
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

Quick Tasks have no markdown body. When marked completed, they are
deleted after 5 seconds via a background goroutine.

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
links: ["<task-uuid>"]
notes: "Met at conference. Interested in PKM systems."
created_at: "2025-01-01T10:00:00Z"
updated_at: "2025-01-01T10:00:00Z"
---
```

## Backend Architecture

### Packages

**cmd/server/main.go** -- Entry point. Loads config, initializes vault,
creates Echo server, registers all routes.

**internal/config/** -- Reads environment variables (SECOND_BRAIN_VAULT_PATH,
SECOND_BRAIN_PORT) with sensible defaults. Expands ~ in vault path.

**internal/model/** -- Domain models and shared types:
- `common.go` -- BaseEntity, EntityStatus, Recurrence, Subtask, Contact, SocialLink, type constants
- `note.go` -- Note struct
- `task.go` -- Task struct
- `quick_task.go` -- QuickTask struct
- `person.go` -- Person struct
- `response.go` -- DataResponse/ErrorResponse helpers for JSON envelope
- `errors.go` -- ValidationError, NotFoundError, and helpers

**internal/handler/** -- HTTP handlers:
- `note.go` -- CRUD for notes
- `task.go` -- CRUD for tasks with recurrence logic
- `quicktask.go` -- CRUD for quick tasks with auto-delete goroutine
- `person.go` -- CRUD for people
- `search.go` -- Full-text search across all entity types
- `helpers.go` -- Shared helper utilities

**internal/vault/** -- File operations:
- Atomic writes (write to temp file, rename)
- Per-file mutex locking (sync.Map)
- YAML frontmatter parsing with yaml.v3
- List entities by scanning directory for .md files
- Archive deleted entities (move to archive/ dir)
- InitVault creates all required subdirectories

### API Design

Base path: `/api/v1`

Response envelope:
```json
{
  "data": { ... },
  "error": "..."
}
```

Error responses use appropriate HTTP status codes (400, 404, 409, 500)
with descriptive error messages.

### Recurrence Logic

1. When PUT /tasks/:id is called with status=completed and the task
   has recurrence configured:
2. Spawn a new Task with fresh UUID, copy all fields
3. Reset subtask completion statuses
4. Calculate new start_date based on recurrence (add N days/weeks/months)
5. Set new end_date based on new start_date + original duration
6. Mark original task as "expired"
7. Return the new task ID in the response

### Quick Task Auto-Delete

When a Quick Task is marked completed via PUT:
1. The status is updated to "completed"
2. A background goroutine waits 5 seconds
3. The file is deleted

## Android Architecture

### MVI Pattern

```
UI (Compose) --events--> ViewModel --state--> UI (Compose)
                   |                          ^
                   v                          |
                Repository --> API --> Backend
```

Each screen has a ViewModel with `StateFlow<UiState>` and sealed event classes.

### Package Structure

**data/api/** -- Ktor-based API service interface and implementation.
Handles HTTP calls to all backend endpoints.

**data/dto/** -- kotlinx.serialization @Serializable data classes
matching the backend JSON response format, including ApiResponse<T>
envelope.

**data/repository/** -- Repository classes wrapping ApiService with
Kotlin `Result<T>` for success/error handling. Each entity
has its own repository.

**domain/model/** -- Domain model classes (Note, Task, QuickTask, Person,
SearchResult) separate from DTOs.

**di/** -- Manual dependency injection via AppModule providing
HttpClient, ApiService, and Repository instances.

**ui/theme/** -- Material3 theming with custom colors, typography, and shapes.

**ui/navigation/** -- Sealed class Screen routes, NavHost composable,
and a bottom navigation bar with tabs: Dashboard, Notes, Tasks, People.

**ui/dashboard/** -- Dashboard screen showing quick stats (entity counts)
and a quick task creation card with the latest quick tasks listed.

**ui/notes/** -- Note listing (LazyColumn), detail view with markdown
content, and edit screen for title/tags/body.

**ui/tasks/** -- Task listing with status filtering, detail view with
subtask checkboxes, and edit screen for all task fields.

**ui/people/** -- Person listing (alphabetical), detail view with
contacts/social links/notes, and edit screen.

## Concurrency & File Safety

- All vault file operations use per-file mutexes (sync.Map keyed by
  absolute path)
- Writes are atomic: content is written to a temp file first, then
  renamed to the target path
- Read operations acquire the same per-file mutex
- Deleted entities are moved to the archive directory, not permanently
  deleted

## Search

MVP search is a simple case-insensitive substring match:
- Loads all vault files for all entity types
- Parses frontmatter and body
- Checks title, tags, notes field, and body content
- Returns matching entities with their type and ID

## Future Considerations

- File watching with fsnotify for hot-reload of vault changes
- Full-text index with bleve for relevance scoring
- WikiLink resolution ([[title]] -> entity lookup)
- Archived entity restoration from archive folder
- Offline mode with local vault copy
- GitHub/WebDAV sync
