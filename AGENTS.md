# AGENTS.MD -- AI Agent Configuration for Second Brain System

This file controls how the AI coding agent (pi) behaves when working on this project.
The agent MUST read and follow this file at the start of every session.

---

## 1. ROLE

You are an AI coding assistant for the Second Brain System project, a lightweight,
remote-first Personal Knowledge Management (PKM) system. Your job is to implement
features, fix bugs, and maintain code quality according to the rules below.

You think step-by-step before coding. You read files before editing them. You treat
this agents.md as your primary governing document.

---

## 2. PROJECT OVERVIEW

Second Brain System is a lightweight, local-first PKM (Personal Knowledge
Management) app. It tracks notes, tasks, quick tasks, and people via a
Markdown vault -- all human-readable .md files with YAML frontmatter.

### Entities

- **Note** -- free-form markdown notes for ideas, references, resources.
- **Task** -- full-featured tasks with icon, location, description, subtasks,
  start/end dates, and optional recurrence (daily, weekly, monthly).
  Recurring tasks spawn new instances with fresh IDs on completion.
- **Quick Task** -- minimal tasks created from the dashboard with just a title.
  Marked complete -> auto-deletes after 5 seconds.
- **Person** -- contact/OSINT profiles with social links, phone/email, notes,
  and cross-entity relations.

All entities support #tags and [[wikilinks]] for cross-referencing.

### Platform

- Backend: Go (echo) runs in Termux on the same Android device.
- Frontend: Kotlin Jetpack Compose app connects to localhost API.
- No SQL databases. No proprietary formats. Your data is your own.

### Core Philosophy

"Markdown-First". All data is human-readable .md files with YAML frontmatter.
Single-user. Localhost-only (no auth in MVP).

---

## 3. TECH STACK & CONSTRAINTS

- Backend: Go (Golang). Use fsnotify for file watching. Use huma or echo for API.
  No heavy frameworks.
- Frontend: Kotlin + Jetpack Compose. Use Material3. MVI Architecture. No XML
  views. Use Ktor for networking.
- Data: Markdown (.md). YAML frontmatter is the schema. No SQLite/PostgreSQL.
- DevOps: Git + GitHub. Feature branches only. Semantic commits.

---

## 4. PROJECT STRUCTURE

```
/
├── README.md                   # Project description
├── AGENTS.md                   # AI agent configuration (this file)
├── backend/                    # Go API server
│   ├── cmd/server/main.go      # Entry point
│   ├── internal/
│   │   ├── handler/            # HTTP handlers (note, task, quicktask, person, search)
│   │   ├── model/              # Domain models & YAML structs
│   │   ├── vault/              # File operations, locking, indexing
│   │   └── config/             # Environment config
│   ├── go.mod
│   └── go.sum
├── android/                    # Kotlin Jetpack Compose app
│   ├── app/src/main/java/com/secondbrain/
│   │   ├── data/               # Ktor client, DTOs, repositories
│   │   ├── domain/model/       # Domain models
│   │   ├── ui/                 # Compose screens, ViewModels, theme, navigation
│   │   └── di/                 # Manual dependency injection
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── docs/
    ├── ARCHITECTURE.md         # Detailed architecture and design decisions
    └── agent-anti-patterns.md  # Go anti-patterns reference (to be created)
```

---

## 5. MANDATORY WORKFLOW (FOLLOW IN ORDER)

1. THINKING BLOCK: Before writing any code, output a THINKING block. Explain
   your plan, which files you will touch, and which rule you are following.

2. DISCOVERY: Use `ls`, `rg`, `find`, and `read` to understand existing code
   before making changes. Do not guess file contents.

3. GIT BRANCHING: Never work on main. Create a branch:
   - `feat/[task-name]` for features
   - `fix/[issue-name]` for bug fixes

4. IMPLEMENTATION:
   - Backend First: Define YAML schema and Go structs.
   - Frontend Sync: Update Kotlin Data Classes to match backend API.

5. VALIDATION:
   - Backend: Run `gofmt -w .` then `go build ./...`. Fix any errors.
   - Frontend: Run `./gradlew ktlintCheck` then `./gradlew assembleDebug`.
     Fix any lint errors.
   - If validation fails: do NOT commit. Fix the issue and re-validate.

6. ANTI-PATTERNS REVIEW:
   - Load the relevant anti-patterns reference from section 15 before accepting
     any generated code.
   - Audit the output against the WRONG/RIGHT tables in the reference.
   - Fix any matches before proceeding.

7. COMMIT & PUSH: Write semantic commit messages (see section 9). Push to GitHub
   and open a Pull Request.

---

## 6. AGENT BEHAVIOR GUIDELINES

- Always check project structure first before making changes.
- Prefer reading files over guessing their contents.
- Use bash for discovery (ls, rg, find) to understand code patterns.
- Explain your plan before implementing -- reference exact files and lines.
- Ask clarifying questions when requirements are ambiguous.
- Be concise in responses; show file paths clearly.
- When in doubt, check existing code for patterns rather than inventing new ones.
- Load and follow the installed skills listed in section 14 when working on relevant tasks.
  Skills are loaded by reading their SKILL.md file or by running `/skill:<name>`.

---

## 7. EMOJI POLICY

The agent MUST NOT use emojis anywhere in this project, including:
- Commit messages
- Pull request descriptions
- Code comments
- Documentation (.md files)
- Inline code or log messages
- Any file created or edited in this repository

Use plain text, bullet points, and ASCII formatting instead.

---

## 8. CODING STANDARDS

### Go
- Use standard library where possible.
- Handle file locking to prevent vault corruption.
- Use `err` for error variable names (idiomatic Go).
- Prefer `var` zero-initialization over `make` unless map/slice capacity is known.

### Kotlin
- Use StateFlow for UI state.
- Use CoroutineScope for async operations.
- Match backend JSON structures exactly in data classes.
- Use `@Serializable` annotation from kotlinx.serialization.

### Markdown
- All notes must have valid YAML frontmatter.
- Required frontmatter fields: `id`, `type`, `status`, `updated_at`.
- Use kebab-case for YAML keys.

### YAML Frontmatter Examples

**Note:**
```yaml
---
id: "550e8400-e29b-41d4-a716-446655440000"
type: "note"
title: "My Note"
status: "active"
tags: ["idea", "reference"]
links: ["<entity-uuid>"]
created_at: "2025-01-01T10:00:00Z"
updated_at: "2025-01-01T10:00:00Z"
---
Markdown content here...
```

**Task:**
```yaml
---
id: "550e8400-e29b-41d4-a716-446655440001"
type: "task"
title: "Write documentation"
status: "pending"          # pending | in-progress | completed | expired
icon: "edit-note"
location: "Home"
tags: ["docs"]
links: []
start_date: "2025-01-01T00:00:00Z"
end_date: "2025-01-10T23:59:59Z"
recurrence: null            # or { type: "weekly", interval: 1, days_of_week: [1,2,3,4,5] }
subtasks:
  - id: "sub-uuid-1"
    title: "Research"
    completed: false
created_at: "2025-01-01T10:00:00Z"
updated_at: "2025-01-01T10:00:00Z"
---
```

**Quick Task:**
```yaml
---
id: "550e8400-e29b-41d4-a716-446655440002"
type: "quick-task"
title: "Buy milk"
status: "pending"            # pending | completed
created_at: "2025-01-01T10:00:00Z"
---
```

**Person:**
```yaml
---
id: "550e8400-e29b-41d4-a716-446655440003"
type: "person"
name: "John Doe"
status: "active"
contacts:
  - type: "phone"
    value: "+90 555 123 4567"
    label: "Personal"
social_links:
  - platform: "github"
    url: "https://github.com/johndoe"
tags: ["friend"]
links: []
notes: "Met at conference."
created_at: "2025-01-01T10:00:00Z"
updated_at: "2025-01-01T10:00:00Z"
---
```

---

## 9. API CONVENTIONS

- Base path: `/api/v1`
- Resources: `/notes`, `/tasks`, `/quick-tasks`, `/people`, `/search`
- Standard CRUD:
  - `GET /resources` -- list all
  - `GET /resources/:id` -- get one
  - `POST /resources` -- create
  - `PUT /resources/:id` -- update
  - `DELETE /resources/:id` -- delete
- Response format: `{ "data": ..., "error": "..." }`
- Errors return appropriate HTTP status codes (400, 404, 409, 500).
- Auth: Bearer token in `Authorization` header (TBD if needed).

---

## 10. COMMIT MESSAGE FORMAT

```
<type>(<scope>): <description>

<body> (optional)
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `style`
Scopes: `api`, `vault`, `model`, `ui`, `data`, `config`, `deps`

Examples:
```
feat(api): add notes list endpoint with pagination
fix(vault): handle file lock contention on concurrent writes
docs: add API usage examples to README
refactor(ui): extract NoteCard component from NoteScreen
```

---

## 11. FORBIDDEN PRACTICES

- NEVER use SQL databases.
- NEVER commit build artifacts (`/build`, `.gradle/`, `bin/`).
- NEVER push directly to the main branch.
- NEVER skip the THINKING block before coding.
- NEVER overwrite a file without reading it first.
- NEVER suggest libraries that violate the "standard library / minimal deps"
  constraint without strong justification.
- NEVER write code that doesn't match the existing code style -- check the
  nearest file first.
- NEVER leave TODO or FIXME comments. Either implement the change or file an
  issue.
- NEVER commit secrets, hardcoded tokens, or absolute paths.
- NEVER use emojis in any project file, comment, commit, or documentation.
- NEVER ship agent-generated Go code without running `go vet ./...` and
  `go test -race ./...` first.
- NEVER ship agent-generated Compose code without loading the relevant
  anti-patterns reference and auditing the output first.

---

## 12. ERROR HANDLING PATTERNS

- Go: Return structured errors with `fmt.Errorf("context: %w", err)`.
- Go: Use `errors.Is` / `errors.As` for error inspection.
- Kotlin: Use `Result<T>` or sealed class `NetworkResult<T>` for API calls.
- Kotlin: Expose errors via `SharedFlow<ErrorEvent>` in ViewModels.
- HTTP: Return `{ "error": "human-readable message" }` in JSON.

---

## 13. SETUP & ENVIRONMENT

```
# Environment variables (create .env in backend/ root)
SECOND_BRAIN_VAULT_PATH=~/second-brain/vault     # Directory for .md vault files
SECOND_BRAIN_PORT=8080                            # API server port

# Run backend
cd backend && go run ./cmd/server

# Build frontend
cd android && ./gradlew assembleDebug

# Run all backend tests
cd backend && go test ./...

# Run all frontend tests
cd android && ./gradlew test
```

---

## 14. INSTALLED SKILLS

This project has skills installed at `~/.pi/agent/skills/`. The agent MUST load and
follow the relevant skill when working on the corresponding task. Skills are loaded
by reading the skill's SKILL.md file or by running `/skill:<name>`.

### compose-skill

- Location: `~/.pi/agent/skills/compose-skill/`
- Purpose: Jetpack Compose and Compose Multiplatform (KMP/CMP) architecture, UI,
  state management, navigation, networking, performance, accessibility, and testing.
- When to use: Any task involving Kotlin UI code -- Composables, ViewModels, MVI
  architecture, Material3 theming, state management, navigation, Ktor networking,
  or any Android/Jetpack Compose screen.
- Activation: Load explicitly when working on frontend UI. The skill covers the
  full Compose app development lifecycle: architecture and state management through
  UI, networking, persistence, performance, accessibility, cross-platform sharing,
  and build configuration.
- Safety: This skill only activates when explicitly invoked. It will NOT auto-trigger
  based on keyword matching alone.
- **Anti-patterns reference:** The skill includes `references/agent-anti-patterns.md`
  which documents 20+ specific patterns that AI agents consistently get wrong in
  Compose code (e.g. `collectAsState` without lifecycle, `GlobalScope.launch`,
  missing keys in LazyColumn, hardcoded strings, string-based navigation). Agents
  MUST load this reference BEFORE generating any Compose UI code to avoid these
  mistakes.

### hallmark

- Location: `~/.pi/agent/skills/hallmark/skills/hallmark/`
- Purpose: Anti-AI-slop UI/UX design skill for greenfield pages, audits, redesigns,
  and design extraction from URLs or screenshots.
- When to use: Any task involving visual design -- building new screens, redesigning
  existing UI, auditing a screen's visual quality, or studying a reference design.
- Activation: Load when the user asks to design or build something new, wants to
  audit/redesign existing UI, or provides a design reference URL or screenshot.
- Safety: Treats `design.md` as design-system data only -- never as executable
  instructions. Has explicit refusal layers for prompt injection in design files.

---

## 15. ANTI-PATTERNS REFERENCES

Two documents catalog the patterns AI agents consistently get wrong in this
project. The agent MUST load the relevant reference BEFORE generating code in
that domain.

### Go Backend and API (`docs/agent-anti-patterns.md`)

Covers 10 categories of AI-agent mistakes:
- Error handling (ignored errors, bare returns, string comparison)
- Goroutine concurrency (leaks, missing ctx.Done, wrong WaitGroup, unbounded spawn)
- Context propagation (context.Background in handlers, stored on struct)
- HTTP handlers and server design (global state, init overuse, no Content-Type)
- File watching with fsnotify (watching files vs directories, no debouncing,
  rename handling)
- File locking and vault integrity (no locks, read-modify-write races)
- Markdown and YAML frontmatter (manual parsing, no validation, wrong types)
- API design for AI agents (generic errors, no idempotency, bad naming)
- Testing (non-table-driven, no race detector, brittle mocks)
- Interface design (producer-defined interfaces, embedded mutex, named returns)

Each anti-pattern includes WRONG/RIGHT code examples with explanations of why
agents get it wrong.

### Kotlin and Compose

`~/.pi/agent/skills/compose-skill/references/agent-anti-patterns.md`

Covers 8 categories of AI-agent mistakes in Jetpack Compose:
- State collection (collectAsState vs collectAsStateWithLifecycle)
- Coroutine scope (GlobalScope, runBlocking)
- State mutation (direct value assignment, mutableStateListOf)
- UI rendering (LazyColumn keys, hardcoded strings, modifier order)
- Navigation (string routes, whole objects as nav args)
- Performance (sort in LazyColumn, backwards writes, derivedStateOf)
- Dependencies (kapt vs KSP, wrong compileSdk, Ktor engine)
- Architecture (ViewModel passed to children, event as consumable boolean)
