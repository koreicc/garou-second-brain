# Second Brain System

Lightweight, Markdown-first Personal Knowledge Management (PKM) system.
Backend runs on Termux (Go), frontend is a Kotlin Jetpack Compose Android app.

## Quick Install (Termux on Android)

One-line install. Clones the repo, installs Go + git, creates the vault, and
downloads dependencies:

```bash
curl -fsSL https://raw.githubusercontent.com/koreicc/garou-second-brain/main/install.sh | bash
```

To also start the server automatically after install, set `SECOND_BRAIN_AUTOSTART=1`:

```bash
curl -fsSL https://raw.githubusercontent.com/koreicc/garou-second-brain/main/install.sh | SECOND_BRAIN_AUTOSTART=1 bash
```

The installer script (`install.sh`) is documented at the repo root. It is
Termux-only -- it verifies it is running inside Termux before doing anything.

## Philosophy

Markdown-First. All data is human-readable .md files with YAML frontmatter.
No SQL databases. No proprietary formats. Your data is your own.

## Project Status

Full MVP implementation is complete. All four entity types (Notes, Tasks,
Quick Tasks, People) are fully implemented with CRUD operations, search,
recurring tasks, and a complete Android UI.

## Entities

| Entity | Description |
|---|---|
| Note | Free-form markdown notes for ideas, references, resources |
| Task | Full-featured tasks with icon, location, subtasks, dates, and optional recurrence (daily/weekly/monthly) |
| Quick Task | Minimal tasks created from dashboard -- title only, auto-deletes 5 seconds after completion |
| Person | Contact/OSINT profiles with social links, contact info, notes, and entity relations |

All entities support `#tags` and `[[wikilinks]]` for cross-referencing.

## Architecture

```
Android (Ktor Client) <--> HTTP/JSON <--> Go API (echo) <--> Vault (.md files)
```

- Backend runs locally in Termux (Go HTTP server on localhost)
- Android app connects to localhost API
- Vault is a directory of .md files managed by the backend
- Android does not access the vault directly -- only through the API

## Stack

- **Backend:** Go, echo framework, yaml.v3, google/uuid
- **Frontend:** Kotlin, Jetpack Compose, Material You (Material Design 3), MVI Architecture, Ktor, kotlinx-serialization
- **Data:** Markdown (.md) with YAML frontmatter
- **Auth:** None (single-user, localhost only)

## Design System

The app uses **Material You (Material Design 3)** with:

| Feature | Details |
|---|---|
| **Dynamic Colors** | Android 12+ uses wallpaper-based dynamic color schemes via `dynamicLightColorScheme` / `dynamicDarkColorScheme`. Falls back to custom blue-tone palette on older devices. |
| **Contrast Awareness** | Android 14+ detects system contrast preference (`UiModeManager.contrast`) and switches to medium-contrast color variants automatically. |
| **Custom Shapes** | Five Material 3 shape levels: extraSmall (4dp), small (8dp), medium/12dp, large/16dp, extraLarge/28dp. |
| **Complete Typography** | All 15 Material 3 type scale slots filled with custom sizes and weights (bodyLarge: 17sp, bodyMedium: 15sp). |
| **Surface Container Colors** | Full surface container family (surfaceDim through surfaceContainerHighest) for proper Material 3 elevation rendering. |
| **Status Badges** | Reusable `StatusBadge` component with color-coded labels (Pending, In Progress, Done). |
| **Relative Time** | `formatRelativeTime()` utility for human-readable dates ("2 minutes ago", "1 hour ago"). |
| **Accessibility** | Descriptive `contentDescription` on all interactive elements. |

## Project Structure

```
/
├── AGENTS.md                 # AI agent configuration
├── README.md                 # This file
├── backend/                  # Go API server
│   ├── cmd/server/main.go    # Entry point
│   └── internal/
│       ├── config/           # Environment config
│       ├── handler/          # HTTP handlers for all entities + search
│       ├── model/            # Domain models, common types, errors, response helpers
│       └── vault/            # File operations, atomic writes, per-file mutex locking
├── android/                  # Kotlin Jetpack Compose app
│   └── app/src/main/java/com/secondbrain/
│       ├── data/             # Ktor client, DTOs, repositories
│       ├── di/               # Manual dependency injection
│       ├── domain/model/     # Domain models
│       └── ui/               # Compose screens with MVI architecture
│           ├── dashboard/    # Dashboard with quick stats, recent items, quick task creation
│           ├── navigation/   # Sealed Screen routes, NavHost, bottom nav bar
│           ├── notes/        # Note list, detail, edit screens
│           ├── tasks/        # Task list, detail, edit screens (with subtask toggle)
│           ├── people/       # Person list, detail, edit screens
│           ├── theme/        # Material You theming (Color.kt, Theme.kt, Type.kt, Shape.kt)
│           └── util/         # Shared utilities (StatusBadge, TimeFormatUtil, LifecycleUtil)
└── docs/
    ├── ARCHITECTURE.md       # Detailed architecture and design decisions
    └── agent-anti-patterns.md # Go anti-patterns reference
```

## API Endpoints

All endpoints are under `/api/v1` and return JSON in the format:
```json
{ "data": ..., "error": "..." }
```

### Notes
- `GET /api/v1/notes` -- List all notes
- `GET /api/v1/notes/:id` -- Get note by ID
- `POST /api/v1/notes` -- Create note
- `PUT /api/v1/notes/:id` -- Update note
- `DELETE /api/v1/notes/:id` -- Delete note (archives to archive/)

### Tasks
- `GET /api/v1/tasks` -- List all tasks
- `GET /api/v1/tasks/:id` -- Get task by ID
- `POST /api/v1/tasks` -- Create task
- `PUT /api/v1/tasks/:id` -- Update task (completing a recurring task spawns a new instance)
- `DELETE /api/v1/tasks/:id` -- Delete task

### Quick Tasks
- `GET /api/v1/quick-tasks` -- List all quick tasks
- `POST /api/v1/quick-tasks` -- Create quick task
- `PUT /api/v1/quick-tasks/:id` -- Mark complete (auto-deletes after 5 seconds)
- `DELETE /api/v1/quick-tasks/:id` -- Delete quick task

### People
- `GET /api/v1/people` -- List all people
- `GET /api/v1/people/:id` -- Get person by ID
- `POST /api/v1/people` -- Create person
- `PUT /api/v1/people/:id` -- Update person
- `DELETE /api/v1/people/:id` -- Delete person

### Search
- `GET /api/v1/search?q=...` -- Full-text search across all entities

## Quick Start

### Backend (manual)
```bash
cd backend
export SECOND_BRAIN_VAULT_PATH=~/second-brain/vault
export SECOND_BRAIN_PORT=8080
go mod tidy
go run ./cmd/server
```

### Frontend
```bash
cd android
./gradlew assembleDebug
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| SECOND_BRAIN_VAULT_PATH | ~/second-brain/vault | Directory for vault .md files |
| SECOND_BRAIN_PORT | 8080 | API server port |

## Vault Structure

```
vault/
  notes/          # Note entity .md files
  tasks/          # Task entity .md files
  quick-tasks/    # Quick Task entity .md files
  people/         # Person entity .md files
  archive/        # Deleted entities (moved here, not permanently deleted)
```

## Recurrence System

When a Task with recurrence is marked completed, the backend:
1. Creates a new Task with a fresh UUID
2. Copies all fields (title, subtasks, icon, location, etc.)
3. Resets subtask completion statuses to false
4. Calculates new start/end dates based on recurrence config
5. Marks the original task as expired (not completed)
6. Returns the new task ID in the response

Recurrence is evaluated on API request (no cron needed).

## Testing (Backend)

```bash
cd backend && go test ./... -race
```

## Screens

| Screen | Features |
|---|---|
| **Dashboard** | Quick stats (notes/tasks count with icons), quick task creation, recent notes, active tasks, pull-to-refresh |
| **Notes** | List with relative timestamps, detail view with markdown body, edit/create screen, delete with confirmation |
| **Tasks** | List with status badges and relative timestamps, detail with interactive subtask toggles, edit with date pickers and recurrence settings |
| **People** | List with contact preview and relative timestamps, detail with contact/social link cards, edit with dynamic form fields |
| **Search** | Cross-entity full-text search with type-based result icons |

## License

MIT
