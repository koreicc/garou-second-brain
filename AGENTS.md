# AGENTS.md -- AI Agent Configuration

This file governs AI coding agent behavior for the Second Brain System project.
Read it at session start. For project overview, architecture, and API docs, see
`README.md` and `docs/ARCHITECTURE.md`.

---

## Workflow

1. **Discover** -- use `ls`, `rg`, `find`, and `read` to understand existing code.
   Do not guess file contents.
2. **Branch** -- never work on main. Use `feat/[name]` or `fix/[name]`.
3. **Implement** -- backend first (YAML schema + Go structs), then frontend sync
   (Kotlin data classes to match).
4. **Validate** -- backend: `gofmt -w .` then `go build ./...`. Frontend:
   `./gradlew ktlintCheck` then `./gradlew assembleDebug`. Do NOT commit if
   validation fails.
5. **Anti-patterns** -- load `docs/agent-anti-patterns.md` before writing Go code.
   For Compose code, load the Compose anti-patterns reference. Audit your output
   against the WRONG/RIGHT tables.
6. **Commit** -- use semantic commit messages (see below). Push and open a PR.

---

## Testing on Device

After pushing a branch, test the backend on Termux:

- **Option A (manual build from local clone):**
  ```sh
  cd backend && CGO_ENABLED=0 GOOS=linux GOARCH=arm64 \
    go build -o ~/.local/bin/second-brain-server ./cmd/server
  pkill second-brain-server 2>/dev/null; \
    nohup second-brain-server > /dev/null 2>&1 &
  ```

- **Option B (CI artifact):** Go to Actions > Backend Build > Run workflow >
  check "Build binary" > download the artifact from the run.

- **Option C (install script):**
  ```sh
  curl -fsSL https://raw.githubusercontent.com/koreicc/garou-second-brain/main/install.sh | \
    SECOND_BRAIN_BUILD_FROM_SOURCE=1 SECOND_BRAIN_BRANCH=feat/my-branch sh
  ```

---

## Coding Standards

### Go
- This project uses Go 1.26.5. Do not downgrade or question this version.
- Use standard library where possible.
- Handle file locking to prevent vault corruption.
- Use `err` for error variable names (idiomatic Go).
- Prefer `var` zero-initialization over `make` unless capacity is known.

### Kotlin
- Use `StateFlow` for UI state.
- Use `CoroutineScope` for async operations.
- Match backend JSON structures exactly in data classes.
- Use `@Serializable` from kotlinx.serialization.

### Markdown / YAML
- All notes must have valid YAML frontmatter.
- Required frontmatter fields: `id`, `type`, `status`, `updated_at`.
- Use kebab-case for YAML keys.

---

## Error Handling

- Go: `fmt.Errorf("context: %w", err)` -- always wrap with context.
- Go: Use `errors.Is` / `errors.As` for error inspection.
- Kotlin: `Result<T>` or sealed class `NetworkResult<T>` for API calls.
- HTTP: `{ "error": "human-readable message" }` in JSON.

---

## Forbidden Practices

- NEVER use SQL databases.
- NEVER commit build artifacts (`/build`, `.gradle/`, `bin/`).
- NEVER push directly to main.
- NEVER overwrite a file without reading it first.
- NEVER leave TODO or FIXME comments -- implement or file an issue.
- NEVER commit secrets, hardcoded tokens, or absolute paths.
- NEVER use emojis in any project file, comment, commit, or documentation.
- NEVER ship Go code without `go vet ./...` and `go test -race ./...`.
- NEVER ship Compose code without loading the anti-patterns reference first.

---

## Commit Messages

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
refactor(ui): extract NoteCard component from NoteScreen
```

---

## Anti-Patterns Reference

`docs/agent-anti-patterns.md` covers 10 categories of AI-agent mistakes in Go:
error handling, goroutine concurrency, context propagation, HTTP handlers,
fsnotify file watching, file locking, Markdown/YAML parsing, API design,
testing, and interface design. Each includes WRONG/RIGHT code examples.

For Jetpack Compose anti-patterns, load the Compose skill reference before
generating any UI code.

---

## Task System Invariants (mandatory)
1. Three entity types: standalone task, template (isTemplate=true), virtual occurrence
   (<parentId>_<YYYY-MM-DD>). Templates never appear in task views; occurrences are
   never written to tasks/.
2. Completing/skipping an occurrence writes an override JSON only — the template is
   never mutated by occurrence actions.
3. effectiveStatus is computed server-side. Manual completed/expired always wins.
   Past-due and not completed -> expired. NEVER auto-complete an unfinished task.
4. dateMode="" tasks are fully manual and never overdue.
5. Default sort: timeBucket ASC -> priorityWeight DESC -> dueDate ASC -> updatedAt DESC,
   via server-issued sortKey. Priority never promotes across time buckets.
6. Recurrence: weekly interval anchored to the start week; monthly day-of-month clamps
   to the last valid day; yearly Feb 29 -> Feb 28 in non-leap years.
7. Full semantics: docs/MASTER_SPEC.md (Part A). Deviations require explicit user approval.

## UI Implementation Policy (mandatory)
1. Reuse before custom. Precedence: Material 3 / M3 Expressive built-ins -> approved
   libraries (docs/MASTER_SPEC.md C3) -> official samples (Now in Android, Jetlagged,
   Reply, Jetnews) -> custom, documented in the commit message.
2. Research first with current (2026) data; prefer sources updated within 12 months
   (developer.android.com, m3.material.io, active GitHub repos).
3. Every screen follows docs/MASTER_SPEC.md Part B: progressive disclosure, segmented
   controls for mode selection, inline subtask quick-add, sheets for secondary
   settings, sticky save, max one scroll to any core action. No control-panel forms.
4. Feedback on every interaction: M3 Expressive springs, haptics, edge-to-edge with
   correct insets, predictive back.