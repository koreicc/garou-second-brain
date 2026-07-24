# SECOND BRAIN — MASTER SPEC
### Task System Semantics · UI Redesign · Agent Directives

**Date:** July 25, 2026 · **Status:** Binding directive for the coding agent · **Scope:** Full overhaul — Go backend + Android (Jetpack Compose) app

---

## 0. MANDATE & DIAGNOSIS

The architecture is sound: YAML-backed Markdown tasks, virtual recurrence occurrences, per-occurrence JSON overrides, server-computed `effectiveStatus`, atomic writes, archive-on-delete, per-file locking. **Keep all of it.** What is broken is the *semantics* and the *UI*:

1. `dateMode="range"` doubles as both span-task and recurrence envelope → conceptual ambiguity.
2. Status is half-manual, half-automatic, and **overdue tasks auto-complete** → unfinished work silently disappears.
3. Priority vs. time ordering is undefined → sorting feels random.
4. Edit screens are control panels → adding a subtask takes 2–3 full scrolls.

This document is the fix. **Part A** rewrites system semantics. **Part B** rewrites the UI. **Part C** binds how the agent works. Where this document conflicts with the old `tasks_system_explanation.txt`, **this document wins**.

### The constitution — this system has exactly three kinds of things

```
┌─────────────────┐   generates (virtual, never stored)   ┌──────────────────┐
│    TEMPLATE     │ ────────────────────────────────────▶ │   OCCURRENCE     │
│ isTemplate=true │                                       │ id: <pid>_<date> │
│ tasks/<id>.md   │   per-instance edits merge on top     │ overrides/*.json │
└─────────────────┘ ◀ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ └──────────────────┘

┌─────────────────┐
│ STANDALONE TASK │   one .md file, one task, no parent
│ tasks/<id>.md   │
└─────────────────┘
```

---

# PART A — TASK SYSTEM SEMANTICS

## A1. Entity rules (non-negotiable)

| Entity | Storage | Visible in |
|---|---|---|
| Standalone task | `tasks/<id>.md` | Task views |
| Template | `tasks/<id>.md`, `isTemplate=true` | **Repeating screen only** |
| Occurrence | Virtual — computed, `id = <parentId>_<YYYY-MM-DD>` | Task views |
| Override | `overrides/<parentId>_<date>.json` | Merged into its occurrence |

- Templates **never** appear in normal task lists, Today, Upcoming, or search results of tasks.
- Occurrences are **never** written to `tasks/`.
- Completing/skipping an occurrence writes an override **only**. The template is never mutated by occurrence actions.

## A2. Date modes

| `dateMode` | Meaning | Automatic status behavior |
|---|---|---|
| `""` | Anytime task | **Fully manual.** Never overdue. Sorted in the Anytime bucket. |
| `due_date` | Deadline on a day | Before → `pending` · due day → `in-progress` · after, not completed → **`expired`** |
| `range` | Spans start→end (inclusive) | Before start → `pending` · during → `in-progress` · after end, not completed → **`expired`** |

**Critical rule for templates:** when `isTemplate=true`, `startDate`/`endDate` are the **recurrence window** (the envelope in which occurrences are generated), not a task span. The UI must label them "Repeat between … and …", never "task duration".

**Breaking change:** the old behavior "after the due day ends → completed" is **removed**. An unfinished task past its date becomes `expired` (overdue). This is the single most important fix in this spec.

## A3. Time modes (boundary precision)

| `timeMode` | Fields | Boundaries |
|---|---|---|
| `""` | — | Day-level: due day 00:00 → 23:59:59 |
| `due_time` | `dueTime` | `in-progress` from start of due day; **`expired` at `dueTime`** if not completed |
| `start_end` | `startTime`,`endTime` | Exact start/end instants |
| `start_duration` | `startTime`,`durationMinutes` | end = start + duration |

## A4. Status model

Persisted enum is **unchanged** (`pending`, `in-progress`, `completed`, `expired`) — no schema migration needed. What changes is the semantics:

- `expired` now means **overdue / missed / skipped** — it can be set manually by the user *or* computed automatically.
- `effectiveStatus` remains server-computed, JSON-only.

**Single source of truth — one function, used everywhere:**

```
ComputeEffectiveStatus(task, now):
    if task.status in {completed, expired}:  return task.status   # manual override always wins
    if task.dateMode == "":                  return task.status   # no date → fully manual
    (start, end) = boundaries(task)                               # per A3, in user's local time
    if now <  start: return "pending"
    if now <= end:   return "in-progress"
    return "expired"                                              # CHANGED: was "completed"
~~~

For occurrences: `dueDate = occurrenceDate`; a past, incomplete occurrence is `expired` — the UI labels it **"Missed"** (or "Skipped" when the user set it manually).

## A5. Recurrence rules

Creation is unchanged: `dateMode="range"` + `recurrence` ⇒ `isTemplate=true`. `endDate` remains required (indefinite recurrence is documented future work; system returns empty when unset — do not change silently).

**Generation rules (harden these):**

| Type | Rule |
|---|---|
| daily | Every `interval` days from `startDate`, within the window |
| weekly | Match `daysOfWeek`, **interval anchored to the start week**: a day qualifies when `floor(daysSinceStart / 7) % interval == 0`. Empty `daysOfWeek` → every `7 × interval` days from start |
| monthly | Same day-of-month as start; **clamp to last valid day** (Jan 31 → Feb 28/29 → Mar 31 → Apr 30) |
| yearly | Same month/day; **Feb 29 → Feb 28** in non-leap years |

**Timezone:** all-day boundaries use the user's local date via the existing `X-Timezone-Offset` header. Future improvement: accept an IANA timezone header for DST correctness — plan for it, don't implement now.

**Override & edit policy:**

| Action | Effect |
|---|---|
| Complete/skip one occurrence | Write `overrides/<pid>_<date>.json` (`Status`, `Title`, `Body`, `Subtasks`). Template untouched. |
| Edit a past or single future occurrence | Override only (existing dialog flow — keep it) |
| Edit the template | Affects **future** occurrences; existing overrides preserved; completed occurrences never retroactively mutated |
| Delete the template | Archive the template **and** its override files (archive-on-delete pattern already exists — extend it) |

## A6. Priority × time — the sorting model

**Golden rule: time decides the bucket, priority sorts inside the bucket. Priority never promotes a task across buckets.** An urgent no-date task does not jump into Today.

| `timeBucket` | Meaning |
|---|---|
| 0 | Overdue (`expired`, not completed) |
| 1 | Today (due today / active range / today's occurrence) |
| 2 | Tomorrow |
| 3 | Next 7 days |
| 4 | Later |
| 5 | Anytime (`dateMode=""`) |
| 6 | Completed |

Priority weights: `urgent=4, high=3, medium=2, low=1, none=0`.

**Default ordering (server-side):**

```sql
ORDER BY timeBucket ASC, priorityWeight DESC, dueDate ASC NULLS LAST, updatedAt DESC
```

**Response enrichment — new fields on every task/occurrence:**

```json
{
  "effectiveStatus": "expired",
  "isOverdue": true,
  "isToday": false,
  "timeBucket": 0,
  "priorityWeight": 3,
  "sortKey": "0|3|2026-07-20"
}
```

The client sorts by `sortKey` only. Manual sort options (title, created…) remain available as a user toggle inside views, never the default.

**Worked example** — `A: overdue/low · B: today/urgent · C: today/low · D: tomorrow/urgent · E: no-date/urgent` → order is **A, B, C, D, E**. To surface E, the user uses the **"Move to Today"** action, which sets `dueDate = today`.

## A7. API surface

Existing endpoints stay (see old spec §6). **Add:**

~~~
GET  /tasks/today                                  # buckets 0+1: overdue, due today, active ranges, today's occurrences
GET  /tasks/overdue                                # bucket 0
GET  /tasks/anytime                                # dateMode = "" only
POST /tasks/occurrence/:parentId/:date/complete    # writes override
POST /tasks/occurrence/:parentId/:date/skip        # writes override, status=expired
```

`GET /tasks/upcoming`, `/tasks/by-date`, `/tasks/templates`, batch, and occurrence-PUT remain as documented. All list responses carry the A6 enrichment fields.

## A8. Invariants (the ten commandments)

1. Templates never appear in task views. Occurrences never touch disk.
2. Occurrence id is deterministic: `<parentId>_<YYYY-MM-DD>`.
3. Occurrence actions write overrides only; the template is immutable to them.
4. Template edits affect the future; past overrides survive.
5. Manual `completed`/`expired` always beats time computation.
6. Past-due and not completed → `expired`. **Never auto-complete.**
7. `dateMode=""` tasks are fully manual and never overdue.
8. `effectiveStatus`, `timeBucket`, `sortKey` are computed server-side; the client never recomputes.
9. Priority sorts within a bucket, never across buckets.
10. All boundaries use the user's local time via the timezone header.

## A9. Behavior delta vs. current system (migration notes)

| Current | New |
|---|---|
| Past due day → `completed` | → `expired` |
| Past range end → `completed` | → `expired` |
| Past incomplete occurrence → ambiguous | → `expired` ("Missed") |
| Templates may surface in task list | Repeating screen only |
| Client-chosen sort field as default | `sortKey` default; manual sort is a toggle |
| "Date range" label for templates | "Repeat between …" |

No schema changes. Tasks already persisted as `completed` stay `completed`.

## A10. Required tests (table-driven, before any UI work ships)

- **Daily:** interval 1 & 3; window boundary inclusion (is `endDate` inclusive? decide once, test it).
- **Weekly:** interval 1 with `daysOfWeek=[1,3,5]`; **interval 2 anchoring** (weeks 0, 2, 4… only); empty `daysOfWeek`; start day not in `daysOfWeek`.
- **Monthly:** start Jan 31 → Feb 28/29 → Mar 31; interval 2.
- **Yearly:** Feb 29 → Feb 28 non-leap.
- **Status:** future → pending; today → in-progress; past incomplete → expired; past completed → completed; manual expired respected.
- **Overrides:** complete one occurrence → siblings unaffected; template title edit → future occurrences change, overridden one keeps its title; completed occurrence survives template subtask edit.
- **Sorting:** the A6 worked example; no-date urgent stays in bucket 5.

---

# PART B — UI REDESIGN

## B1. Principles (non-negotiable)

1. **Progressive disclosure** — the form reshapes to the selection; irrelevant fields vanish.
2. **Core actions inline and immediate** — subtask quick-add sits under the subtask list; zero extra scrolls.
3. **Segmented controls replace mode buttons** — any 2–5 exclusive choices = `SegmentedButton` row, never large cards.
4. **Secondary settings go to a `ModalBottomSheet`** — tags, links, location, icon gallery.
5. **Feedback on every interaction** — M3 Expressive springs, haptics, shared-element transitions.
6. **Reuse before custom** — see Part C.

The foundation stays: Material You dynamic color, bottom bar, existing base components.

## B2. Screen-by-screen

### B2.1 Task Edit — highest priority

Vertical structure, exact order:

| # | Element | Implementation |
|---|---|---|
| 1 | Top bar: back · title · overflow (delete, duplicate) | `TopAppBar` |
| 2 | Title row: icon button + borderless display-size field | No outlines |
| 3 | Status: Pending / In progress / Done | 3-segment `SingleChoiceSegmentedButtonRow` |
| 4 | Priority: one chip ("⚑ High") → small sheet | Replaces the 5-chip row |
| 5 | Date mode: None / Due date / Range | 3-segment row (icons) |
| 6 | Conditional date area — animated expand/collapse | See below |
| 7 | **Subtasks:** header `n/m` · list with drag-reorder + swipe-delete · **inline quick-add row** | Always visible once reached |
| 8 | Notes: collapsed borderless multiline ("Add notes…") | Expands on focus |
| 9 | "More details" chip → sheet (location, tags, links) | `ModalBottomSheet` |
| 10 | **Sticky save bar** above nav bar | Never scroll to save |

Conditional date area:
- `None` → collapses to nothing.
- `Due date` → date chip + ghost "+ Time" chip.
- `Range` → two date chips, then a nested **"Repeat" card**: frequency segmented (Daily/Weekly/Monthly), interval stepper, day-of-week row (weekly only). The card exists *only* in Range mode — this is what resolves the range/recurrence ambiguity in the UI (mirrors A2). For templates, the date chips are labeled "Repeat between".

```kotlin
SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    DateMode.entries.forEachIndexed { i, mode ->
        SegmentedButton(
            selected = uiState.dateMode == mode,
            onClick = { viewModel.setDateMode(mode) },
            shape = SegmentedButtonDefaults.itemShape(i, DateMode.entries.size),
            icon = { SegmentedButtonDefaults.Icon(active = uiState.dateMode == mode) },
        ) { Text(mode.label) }
    }
}
```

Occurrence edit flow (this-occurrence-only vs. template dialog) stays — it's correct.

### B2.2 Task List

- **Grouped by `timeBucket`** with sticky headers: Overdue · Today · Tomorrow · This week · Later · Anytime · Completed (collapsed). Sections come straight from the A6 enrichment — the client does not compute them.
- Card: expressive colored left edge by `effectiveStatus` · icon in expressive shape · title · small priority flag (not a badge) · M3 Expressive `ProgressIndicator` for subtasks · date line. Occurrences labeled "Missed" when `expired`.
- **Swipe right = complete** (strikethrough animation + haptic + undo snackbar). **Swipe left:** occurrences → skip; standalone → snooze/reschedule sheet.
- `PullToRefreshBox`, scrolling filter chips, expanded FAB. Templates excluded (A1).

### B2.3 Task Detail

Hero header (large icon in expressive shape, display title, status + priority chips, quick-action row: Complete · Edit · Reschedule · Delete) → animated subtask checklist → Markdown body (`[[wikilinks]]` tappable) → linked entities as horizontal snap cards. List → detail icon morph via `SharedTransitionLayout`.

### B2.4 Repeating screen (new)

Template list only. Tapping a template shows its pattern summary ("Every Mon, Wed, Fri · until Aug 30") and a preview of upcoming occurrences. Pause/archive lives here.

### B2.5 Notes & People — same language

Note edit = document style (borderless title, inline body, property chips in a sheet — study Jetnews). Person detail = avatar hero, contact rows, linked entities as card sections. All three entity types share one design system: same headers, sheets, motion.

### B2.6 Shell

Bottom bar via `NavigationSuiteScaffold` (adaptive rail on larger screens). Edge-to-edge mandatory (`enableEdgeToEdge`, correct `WindowInsets`). Predictive back animated.

## B3. Pattern replacement map

| Current | Replace with |
|---|---|
| 3 large date-mode buttons | `SingleChoiceSegmentedButtonRow` |
| Status chip row | 3-segment segmented control |
| 5 priority chips always visible | One chip → small sheet |
| Recurrence wall in the form | Nested "Repeat" card, Range mode only |
| Subtasks 3 scrolls deep | Section 7 + inline quick-add + drag reorder |
| Tags/links/location inline | "More details" sheet |
| Save at end of scroll | Sticky bottom save bar |
| Flat task list | `timeBucket`-grouped sections |
| Delete-only list action | Swipe right complete / swipe left skip-snooze |

## B4. UI ↔ system integration (what consumes what)

| UI element | Backend field |
|---|---|
| List section headers & order | `timeBucket` / `sortKey` (A6) |
| Card left-edge color | `effectiveStatus` (A4) |
| "Missed" / "Overdue" labels | `expired` + occurrence vs. standalone |
| "Repeat" card visibility | `dateMode=="range" && isTemplate` (A2) |
| Anytime section | `dateMode==""` |
| "Move to Today" action | sets `dueDate = today` |
| Skip swipe on occurrence | `POST .../skip` → override `status=expired` (A7) |

---

# PART C — AGENT DIRECTIVES

## C1. Execution order

| Phase | Work | Gate |
|---|---|---|
| 0 | Save this doc as `docs/MASTER_SPEC.md`; append the AGENTS.md block (C4) | Block present verbatim |
| 1 | Backend semantics: A4 status change, A6 enrichment + `sortKey`, A7 endpoints | A10 status & sorting tests green |
| 2 | Recurrence hardening: A5 anchoring + clamping | A10 recurrence tests green |
| 3 | UI: Task Edit per B2.1 | Acceptance criteria 1–5 |
| 4 | UI: List, Detail, Repeating per B2.2–B2.4 | Acceptance criteria 6–8 |
| 5 | Notes/People consistency (B2.5), shell polish (B2.6), optional Glance task widget | Full checklist |

## C2. Research protocol (execute before building any screen)

Trusted sources, in order: **developer.android.com** (Compose docs, BOM mapping, release notes) · **m3.material.io** · **github.com/android/compose-samples** (Now in Android, Jetlagged, Reply, Jetnews) · GitHub topics `jetpack-compose` sorted by recent activity · **mobbin.com** (Things, TickTick, Notion, Google Tasks — patterns only) · ProAndroidDev / official Android blog (2025–2026).

Required searches (current, 2026 data): `material3-expressive compose stable components 2026` · `compose BOM latest mapping` · `compose SegmentedButton single choice` · `sh.calvin.reorderable LazyColumn drag` · `compose swipe actions list item 2026` · `multiplatform markdown renderer compose` · `compose rich text editor notes` · `NavigationSuiteScaffold adaptive` · `SharedTransitionLayout list detail` · `Glance widget compose`.

**Freshness rule:** adopt only libraries/repos with activity in the last 12 months. Newer official docs win for API usage; the design decisions in this spec stand.

## C3. Approved stack (2026)

| Need | Use |
|---|---|
| Design system | `material3` + `material3-expressive` (latest stable, verified against current BOM) |
| Drag & drop reorder | `sh.calvin.reorderable` |
| Image loading | Coil 3 |
| Markdown rendering | `mikepenz/multiplatform-markdown-renderer` |
| Rich text (notes, if needed) | `MohamedRejeb/compose-rich-editor` |
| Icons | Material Symbols + ComposeIcons |
| Theming | Built-in Material You (MaterialKolor only if custom seeds appear) |
| Widget | `androidx.glance` |
| Motion | M3 Expressive springs first; Lottie for at most one hero moment |

Study before building: **Now in Android** (architecture + M3), **Jetlagged** (Expressive showcase), **Reply** (adaptive), **Jetnews** (editorial layouts).

## C4. AGENTS.md — append verbatim (Phase 0, first action)

```markdown
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
```

## C5. Acceptance criteria (definition of done)

- [ ] Adding a subtask from opening Task Edit: ≤ 2 taps, 0 extra scrolls.
- [ ] Date mode = one segmented row; choosing a mode visibly reshapes the form.
- [ ] Tags/links/location/icons live in sheets; save is sticky on every edit screen.
- [ ] An unfinished overdue task shows as Overdue/Missed — never as Completed.
- [ ] List grouped by server `timeBucket`; templates absent; swipe-to-complete with haptic + undo.
- [ ] Sorting matches the A6 worked example exactly.
- [ ] A10 test suite green; every new component traceable to the reuse policy.
- [ ] Notes & People share the Tasks design language; edge-to-edge + predictive back correct.
- [ ] AGENTS.md contains both blocks; `docs/MASTER_SPEC.md` exists.
