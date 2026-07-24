# Second Brain System -- Development Roadmap & Status

## Overview

This document tracks the development status of the Second Brain System project,
a local-first PKM app with Markdown vault backend (Go/Echo) and Android frontend
(Kotlin/Jetpack Compose).

---

## Completed Features

### 1. Task Status Automation

**Files changed:**
- `backend/internal/model/task.go` -- `ComputeEffectiveStatus()` + `startOfDay`/`endOfDay` helpers
- `backend/internal/handler/task.go` -- `enrichTasks()` computes effective_status on every response
- `backend/internal/handler/entity.go` -- entity resolution uses effective status

**Behavior:**
- `date_mode=none`: status is fully manual
- `date_mode=due_date`: pending before due, in-progress on due day, completed after
- `date_mode=range`: pending before start, in-progress during range, completed after
- `time_mode` with `start_time`/`end_time`/`due_time`/`duration` narrows the boundaries
- Manual override (user sets completed/expired) is always respected

### 2. Timezone Support

**Files changed:**
- `backend/internal/handler/task.go` -- reads `X-Timezone-Offset` header (minutes from UTC)
- `backend/internal/handler/entity.go` -- same header support
- `android/.../KtorApiService.kt` -- sends device timezone offset with every task API call

**Why:** Without timezone offset, a user in GMT+3 would see status transitions
at UTC midnight (3 AM local) instead of their own midnight.

### 3. Overdue Tasks Section

**Files changed:**
- `android/.../DashboardViewModel.kt` -- computes overdue: `status != completed` but `effectiveStatus == completed` + has `dateMode`
- `android/.../DashboardScreen.kt` -- `OverdueTasksSection` in red-tinted card at top of dashboard

**Behavior:** Tasks past their deadline but not manually completed appear in a
red "Overdue" section above the daily task list.

### 4. Calendar View

**Files changed:**
- `android/.../ui/calendar/CalendarScreen.kt` -- month grid with day cells + task list
- `android/.../ui/calendar/CalendarViewModel.kt` -- loads tasks, groups by date for month
- `android/.../ui/navigation/Screen.kt` -- added `Screen.Calendar` route
- `android/.../ui/navigation/AppNavigation.kt` -- Calendar composable route
- `android/app/build.gradle.kts` -- added `com.kizitonwose.calendar:compose:2.6.2`

**Features:**
- Month grid with navigation (prev/next month)
- Days with tasks show a dot indicator
- Selected day highlighted with primary color
- Today highlighted with primaryContainer
- Task list below calendar for selected date
- Access via calendar icon in Dashboard top bar

### 5. Smart Lists (Today/Tomorrow/This Week)

**Files changed:**
- `android/.../DashboardViewModel.kt` -- `selectedScope`, `weekStartDate`, `weekTasksByDay` fields, `selectScope()`, `loadWeekTasks()`
- `android/.../DashboardScreen.kt` -- `ScopeChipsRow` with FilterChips, `WeekTasksSection`

**Features:**
- 4 filter chips: Today, Tomorrow, This Week, Pick Date
- Today/Tomorrow: tasks for that date, with date selector nav
- This Week: Mon-Sun grouped view showing all tasks in the week
- Pick Date: opens date picker, switches to custom date mode
- Week view auto-loads tasks for all 7 days from the local task list

### 6. Priority System

**Files changed:**
- `backend/internal/model/task.go` -- `Priority` field, constants, validation
- `backend/internal/handler/task.go` -- `Priority` in Create/Update requests
- `android/.../domain/model/Task.kt` -- `priority` field
- `android/.../data/dto/TaskDto.kt` -- `@SerialName("priority")` in 3 DTOs
- `android/.../ui/util/StatusBadge.kt` -- `PriorityBadge` composable (Low/Med/High/Urg)
- `android/.../ui/tasks/TaskEditViewModel.kt` -- `priority` state + save
- `android/.../ui/tasks/TaskEditScreen.kt` -- priority chip selector (5 levels)
- `android/.../ui/dashboard/DashboardScreen.kt` -- shows PriorityBadge on task cards

**Priority levels:** `None`, `Low`, `Medium`, `High`, `Urgent`
- Backend validates against allowed values
- Frontend displays colored badge before status badge
- Priority stored in YAML frontmatter: `priority: high`

---

## Remaining Features

### Priority: Medium

| Feature | Description | Where |
|---|---|---|
| **Reminders/Notifications** | Android NotificationManager + AlarmManager/WorkManager | New `reminder` service |
| **Swipe Actions** | Swipe-to-complete, swipe-to-delete on task cards | `TaskListScreen.kt` |
| **Search** | Full-text search across tasks | Frontend search bar |
| **Filtering/Sorting** | By status, date, priority, folder | `TaskListScreen.kt` |
| **Subtask Progress** | Show completion % on task card | `TaskCard` composable |
| **Batch Operations** | Multi-select + bulk complete/delete | `TaskListViewModel.kt` |
| **Natural Language Input** | "Buy milk tomorrow at 5pm" parsing | `TaskEditViewModel.kt` |

### Priority: Low

| Feature | Description | Where |
|---|---|---|
| **Analytics** | Completion charts, streak tracking | New `analytics` screen |
| **Backup/Restore** | JSON export/import of vault | New `backup` handler + UI |
| **Widget** | Android home-screen widget | New `widget` package |
| **Drag-Drop Reschedule** | Drag tasks in calendar view | `CalendarScreen.kt` |
| **ICS Import** | Import tasks from calendar files | New `import` handler |

---

## Known Issues / Edge Cases

1. **Recurrence + Feb 29** -- Yearly recurrence starting on Feb 29. On non-leap
   years, should it occur on Feb 28 or Mar 1? Currently undefined.

2. **Multi-day time boundaries** -- If a task has `start_date=Aug 6, end_date=Aug 9`
   with `start_time=07:00, end_time=09:00`, the effective status is:
   - Aug 6 07:00 to Aug 9 09:00 = in-progress
   - But is Aug 7 at 03:00 AM in-progress? Yes (between boundaries).
   - This might not be the expected behavior if times are intended to be
     daily boundaries rather than continuous.

3. **Memory** -- `taskRepository.getAll()` loads ALL tasks into memory. With
   hundreds of tasks this could be slow.

4. **No offline cache** -- If the server restarts, the frontend has no cached
   data and shows a blank screen.

5. **Error recovery** -- API errors might leave UI in loading state if
   `onFailure` handler doesn't reset loading.

---

## Key Files Reference

### Backend (Go)

| File | Purpose |
|---|---|
| `backend/cmd/server/main.go` | Entry point, routes |
| `backend/internal/handler/task.go` | Task CRUD + `enrichTasks()` |
| `backend/internal/model/task.go` | Task struct + `ComputeEffectiveStatus()` + `Priority` |
| `backend/internal/model/common.go` | EntityStatus, BaseEntity constants |
| `backend/internal/vault/vault.go` | File operations, locking |
| `backend/internal/handler/entity.go` | Entity resolution |

### Frontend (Kotlin)

| File | Purpose |
|---|---|
| `ui/dashboard/DashboardScreen.kt` | Dashboard with scope chips, week view, overdue, tasks, quick-actions |
| `ui/dashboard/DashboardViewModel.kt` | Dashboard state + scope/date/week loading |
| `ui/calendar/CalendarScreen.kt` | Month grid calendar |
| `ui/calendar/CalendarViewModel.kt` | Calendar state + task loading |
| `ui/tasks/TaskListScreen.kt` | Task list with status colors |
| `ui/tasks/TaskDetailScreen.kt` | Task detail view |
| `ui/tasks/TaskEditScreen.kt` | Task creation/editing with priority |
| `data/api/KtorApiService.kt` | API client with timezone header |
| `domain/model/Task.kt` | Task model + `displayStatus` + `priority` |
| `ui/navigation/AppNavigation.kt` | Navigation host + routes |
| `ui/navigation/Screen.kt` | Route definitions |
| `ui/util/StatusBadge.kt` | `StatusBadge` + `PriorityBadge` composables |

---

## Build & Run

```sh
# Backend
cd backend && go run ./cmd/server

# Frontend
cd android && ./gradlew assembleDebug
```

Backend runs on port 8080. Frontend connects to `http://127.0.0.1:8080/api/v1`.