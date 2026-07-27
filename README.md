# Second Brain System

[![Go Version](https://img.shields.io/badge/go-1.26.5-blue)](https://go.dev/)
[![Build Status](https://img.shields.io/github/actions/workflow/status/koreicc/garou-second-brain/backend-build.yml?branch=main)](https://github.com/koreicc/garou-second-brain/actions)
[![License](https://img.shields.io/github/license/koreicc/garou-second-brain)](LICENSE)
[![Last Commit](https://img.shields.io/github/last-commit/koreicc/garou-second-brain/main)](https://github.com/koreicc/garou-second-brain/commits/main)

Lightweight, Markdown-first Personal Knowledge Management (PKM) system.
Backend runs on Termux (Go/Echo), frontend is a Kotlin Jetpack Compose Android app.

## Features

- Five entity types: Notes, Tasks, Habits, Quick Tasks, People with full CRUD
- Markdown files with YAML frontmatter -- no SQL databases
- Recurring tasks with daily/weekly/monthly/yearly schedules
- Habit tracking with day-of-week scheduling and daily completion
- Subtasks, priority levels, timezone-aware status computation
- Calendar view with month grid and per-day task lists
- Smart lists (Today/Tomorrow/This Week) on dashboard
- Task templates and occurrence system
- Cross-referencing with `#tags` and `[[wikilinks]]`
- Full-text search across all entities
- Archive and restore for deleted entities
- Material You (Material Design 3) dynamic theming
- Single-user, localhost-only (runs in Termux)

## Quick Install

```sh
curl -fsSL https://raw.githubusercontent.com/koreicc/garou-second-brain/main/install.sh | sh
```

With autostart: `... | SECOND_BRAIN_AUTOSTART=1 sh`
Build from source: `... | SECOND_BRAIN_BUILD_FROM_SOURCE=1 sh`

After install, run `source ~/.bashrc` (or `~/.zshrc`) then `second-brain-server`.

## Architecture

```
Android (Ktor Client) <--> HTTP/JSON <--> Go API (echo) <--> Vault (.md files)
```

- Backend runs locally in Termux (Go HTTP server on localhost)
- Android app connects to localhost API
- Vault is a directory of .md files managed by the backend
- Android does not access the vault directly -- only through the API

## Stack

- **Backend:** Go 1.26.5, echo framework, yaml.v3, google/uuid
- **Frontend:** Kotlin, Jetpack Compose, Material You (MD3), Ktor, kotlinx-serialization
- **Data:** Markdown (.md) with YAML frontmatter
- **Auth:** None (single-user, localhost only)

## Quick Start

### Backend

```sh
cd backend
export SECOND_BRAIN_VAULT_PATH=~/second-brain/vault
export SECOND_BRAIN_PORT=8080
go mod tidy
go run ./cmd/server
```

### Frontend

```sh
cd android
./gradlew assembleDebug
```

### Run Tests

```sh
# Backend
cd backend && go test -race ./...

# Frontend
cd android && ./gradlew test
```

## Update Backend

Download the latest nightly binary and restart:

```sh
curl -fSL -o ~/.local/bin/second-brain-server \
  https://github.com/koreicc/garou-second-brain/releases/download/nightly/second-brain-server-arm64 \
  && chmod +x ~/.local/bin/second-brain-server \
  && pkill second-brain-server 2>/dev/null; \
  nohup second-brain-server > /dev/null 2>&1 &
```

## Build from Source (any branch)

For testing a feature branch on your device:

```sh
curl -fsSL https://raw.githubusercontent.com/koreicc/garou-second-brain/main/install.sh | \
  SECOND_BRAIN_BUILD_FROM_SOURCE=1 SECOND_BRAIN_BRANCH=feat/my-branch sh
```

Or manually from a local clone:

```sh
cd backend && CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o ~/.local/bin/second-brain-server ./cmd/server
pkill second-brain-server 2>/dev/null; nohup second-brain-server > /dev/null 2>&1 &
```

## Entities

| Entity | Description |
|---|---|
| Note | Free-form markdown notes for ideas, references, resources |
| Task | Full-featured tasks with icon, location, subtasks, dates, and optional recurrence (daily/weekly/monthly/yearly) |
| Habit | Recurring habits with day-of-week scheduling, completion tracking per day |
| Quick Task | Minimal tasks created from dashboard -- title only, auto-deletes 5 seconds after completion |
| Person | Contact/OSINT profiles with social links, contact info, notes, and entity relations |

All entities support `#tags` and `[[wikilinks]]` for cross-referencing.

## Design System

The app uses **Material You (Material Design 3)** with:

| Feature | Details |
|---|---|
| Dynamic Colors | Android 12+ uses wallpaper-based dynamic color schemes. Falls back to custom blue-tone palette on older devices. |
| Contrast Awareness | Android 14+ detects system contrast preference and switches to medium-contrast color variants. |
| Custom Shapes | Five M3 shape levels: extraSmall (4dp) through extraLarge (28dp). |
| Complete Typography | All 15 M3 type scale slots with custom sizes and weights. |
| Status Badges | Reusable `StatusBadge` component with color-coded labels (Pending, In Progress, Done). |
| Priority Badges | Five-level priority display (None, Low, Medium, High, Urgent). |
| Relative Time | `formatRelativeTime()` utility for human-readable dates. |
| Accessibility | Descriptive `contentDescription` on all interactive elements. |

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
│       ├── data/             # Ktor client, DTOs, repositories, settings persistence
│       ├── di/               # Manual dependency injection (AppModule)
│       ├── domain/model/     # Domain models
│       └── ui/               # Compose screens with MVI architecture
│           ├── common/       # Shared components (LinkPickerSheet, LinkedEntitiesView)
│           ├── dashboard/    # Daily planner, routines, today's tasks
│           ├── navigation/   # Floating pill bar, FAB menu, NavHost
│           ├── notes/        # Note list, detail, edit screens
│           ├── tasks/        # Task list, detail, edit screens (with subtask toggle)
│           ├── people/       # Person list, detail, edit screens
│           ├── calendar/     # Month grid calendar with per-day task list
│           ├── settings/     # Theme, server config, persistence
│           ├── workspace/    # Unified entity management (Notes/Tasks/People tabs)
│           ├── theme/        # Material You theming (Color.kt, Theme.kt, Type.kt, Shape.kt)
│           └── util/         # Shared utilities (StatusBadge, TimeFormatUtil, LifecycleUtil)
└── docs/
    ├── ARCHITECTURE.md       # Detailed architecture and design decisions
    ├── agent-anti-patterns.md # Go anti-patterns reference
    ├── compose/              # Compose UI anti-patterns reference
    └── MASTER_SPEC.md        # Full feature specification
```

## API Endpoints

All endpoints are under `/api/v1` and return JSON:

```json
{ "data": ..., "error": "..." }
```

### Notes
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/notes | List all notes |
| GET | /api/v1/notes/:id | Get note by ID |
| POST | /api/v1/notes | Create note |
| PUT | /api/v1/notes/:id | Update note |
| DELETE | /api/v1/notes/:id | Delete note (archives to archive/) |

### Tasks
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/tasks | List all tasks |
| GET | /api/v1/tasks/templates | List task templates |
| GET | /api/v1/tasks/by-date | List tasks by date |
| GET | /api/v1/tasks/upcoming | List upcoming tasks |
| GET | /api/v1/tasks/:id | Get task by ID |
| POST | /api/v1/tasks | Create task |
| POST | /api/v1/tasks/batch | Batch task operations |
| PUT | /api/v1/tasks/:id | Update task (completing a recurring task spawns a new instance) |
| DELETE | /api/v1/tasks/:id | Delete task |
| PUT | /api/v1/tasks/occurrence/:parentId/:date | Update task occurrence |

### Habits
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/habits | List all habits |
| GET | /api/v1/habits/today | Get today's habit completions |
| GET | /api/v1/habits/:id | Get habit by ID |
| POST | /api/v1/habits | Create habit |
| PUT | /api/v1/habits/:id | Update habit |
| DELETE | /api/v1/habits/:id | Delete habit |
| POST | /api/v1/habits/:id/complete | Mark habit completed for today |

### Quick Tasks
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/quick-tasks | List all quick tasks |
| POST | /api/v1/quick-tasks | Create quick task |
| PUT | /api/v1/quick-tasks/:id/complete | Mark complete (auto-deletes after 5 seconds) |
| DELETE | /api/v1/quick-tasks/:id | Delete quick task |

### People
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/people | List all people |
| GET | /api/v1/people/:id | Get person by ID |
| POST | /api/v1/people | Create person |
| PUT | /api/v1/people/:id | Update person |
| DELETE | /api/v1/people/:id | Delete person |

### Search
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/search?q=... | Full-text search across all entities |
| GET | /api/v1/wikilink | Resolve wikilinks to entities |

### Archive
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/archive | List archived entities |
| POST | /api/v1/archive/:type/:id/restore | Restore archived entity |

### Entity Resolution
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/entities/by-ids | Resolve multiple entities by IDs |

### Health
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/health | Server health check |

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

## Screens

| Screen | Features |
|---|---|
| Dashboard | Daily planner with greeting, routine checklist, today's tasks, smart lists, overdue section, quick task/note creation |
| Workspace | Unified entity management (Notes/Tasks/People tabs) with global search |
| Calendar | Month grid with dot indicators, day selection, per-day task listing |
| Settings | Theme config (Material You, dark mode, palette), server URL, persistence |
| Detail/Edit | Full CRUD for all entities with linked entity support and wikilink navigation |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow and coding standards.

## License

[MIT](LICENSE)
