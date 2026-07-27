# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

A single primary user (the developer) using the app as a personal
knowledge and task management system, daily, on their own Android phone.
No multi-user, no shared workspace, no team collaboration. Every screen is
designed for one person who is both the operator and the administrator.

## Product Purpose

Second Brain is a local-first personal knowledge management (PKM) and task
system that runs entirely on the user's own device. The Go backend executes
in Termux on the phone; the Android app talks to it over localhost HTTP. The
product exists so that one person can capture, organize, and act on notes,
tasks, habits, and contacts without depending on any cloud service, sync
infrastructure, or external server.

Success means the user can:
- Capture a thought, task, or contact in seconds from the dashboard.
- Trust that tasks won't silently disappear (overdue work is surfaced, never
  auto-completed).
- See exactly what matters today (today's tasks, routines, overdue work) and
  nothing that doesn't.
- Keep all data as plain Markdown files the user owns and can read without
  the app.

## Positioning

The entire system runs on-device. No cloud, no sync, no server elsewhere.
The vault is a directory of Markdown files the user can open in any text
editor. A neighboring PKM (Obsidian, Notion, Logseq) or task app (Google
Tasks, TickTick) cannot truthfully claim that the full stack -- server and
client -- lives on the user's own phone and never transmits data off it.

## Operating Context

- **Runtime:** Go HTTP server (Echo) on localhost, inside Termux, on the
  user's Android phone. The server is started manually or via autostart and
  runs as a background process.
- **Client:** Kotlin Jetpack Compose Android app connecting to
  `localhost:<port>`. No remote backend.
- **Data store:** A vault directory of `.md` files with YAML frontmatter,
  organized by entity type (`notes/`, `tasks/`, `quick-tasks/`, `people/`,
  `archive/`). No SQL database. Atomic writes and per-file mutex locking
  prevent corruption.
- **Auth:** None. Single-user, localhost-only.
- **Daily workflow:** Open app -> dashboard (today's tasks, routines, smart
  lists, overdue section, quick capture). Drill into workspace for notes /
  tasks / people management. Calendar for the month view. Settings for
  theme and server config.
- **Constraint:** The backend is evaluated on every API request (no cron);
  recurrence and status computation happen at request time.

## Capabilities and Constraints

### Entities (five types)
- **Note** -- free-form Markdown for ideas, references, resources.
- **Task** -- full-featured: icon, location, subtasks, priority, dates,
  optional recurrence (daily / weekly / monthly / yearly). Three storage
  forms: standalone task, template (`isTemplate=true`), virtual occurrence
  (`<parentId>_<YYYY-MM-DD>`).
- **Habit** -- recurring habits with day-of-week scheduling and daily
  completion tracking.
- **Quick Task** -- minimal tasks (title only) created from the dashboard;
  auto-deletes 5 seconds after completion.
- **Person** -- contact / OSINT profiles with social links, contact info,
  notes, and entity relations.

All entities support `#tags` and `[[wikilinks]]` for cross-referencing.

### Task system invariants (binding)
1. Templates never appear in task views; occurrences are never written to
   `tasks/`.
2. Occurrence actions write override JSON only; the template is immutable
   to them.
3. `effectiveStatus` is computed server-side; manual completed/expired
   always wins; past-due and not completed -> expired. Never auto-complete
   an unfinished task.
4. `dateMode=""` tasks are fully manual and never overdue.
5. Default sort: `timeBucket ASC -> priorityWeight DESC -> dueDate ASC ->
   updatedAt DESC`, via server-issued `sortKey`. Priority never promotes
   across time buckets.
6. Recurrence: weekly interval anchored to the start week; monthly
   day-of-month clamps to the last valid day; yearly Feb 29 -> Feb 28 in
   non-leap years.
7. Full semantics: `docs/MASTER_SPEC.md` (Part A). Deviations require
   explicit user approval.

### Technical constraints
- Go 1.26.5 (do not downgrade).
- No SQL databases, ever.
- No cloud, no sync, no external server.
- Single-user, localhost-only.
- All status / sorting / recurrence computation is server-side; the client
  never recomputes.

## Brand Commitments

- **Name:** "Second Brain" (fixed).
- **Voice / personality:** Open -- no binding voice or personality
  constraints established yet.
- **Visual identity:** Material You (Material Design 3) with dynamic color
  is the committed design system. M3 Expressive is approved for motion and
  component behavior. No custom brand palette or typography has been
  pinned beyond the M3 system.

## Evidence on Hand

- `README.md` -- full feature list, API surface, architecture, stack.
- `docs/MASTER_SPEC.md` -- binding task system semantics and UI redesign
  directive (Parts A, B, C). This is the source of truth for task behavior
  and UI patterns.
- `docs/ARCHITECTURE.md` -- detailed architecture and design decisions.
- `DEVELOPMENT_STATUS.md` -- completed features and roadmap.
- `AGENTS.md` -- coding standards, task system invariants, UI
  implementation policy, commit conventions.
- `docs/agent-anti-patterns.md` -- Go anti-patterns reference.
- `docs/compose/` -- Jetpack Compose UI anti-patterns reference.
- No customer testimonials, case studies, press, or marketing imagery
  exist. Future work must not fabricate any.

## Product Principles

1. **Local-first is non-negotiable.** The full stack runs on the user's
   device. Any feature that requires data to leave the phone is out of
   scope unless explicitly approved.
2. **Markdown is the source of truth.** Every entity is a plain `.md` file
   the user can read and edit without the app. The app is a fast,
   structured view over those files, not a proprietary store.
3. **Trust over convenience.** Unfinished work is surfaced as overdue, never
   silently completed. The system tells the truth about what was done and
   what was missed.
4. **Capture speed above all.** The dashboard exists to turn a thought into
   a record in seconds. Friction in capture is a regression.
5. **Reuse before custom.** Material 3 / M3 Expressive built-ins and approved
   libraries come before custom components. Custom work must be justified
   and documented.

## Accessibility & Inclusion

No product-specific accessibility requirement beyond standard Android and
Material 3 defaults (TalkBack, dynamic font size, contrast, touch target
sizes). Follow platform accessibility conventions; do not regress them.
