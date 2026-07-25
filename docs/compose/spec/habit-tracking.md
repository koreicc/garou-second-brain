---
feature: habit-tracking
status: delivered
updated: 2025-07-25
branch: feat/habit-tracking
commits: 430f5c3..77bfa7d
---

# Habit Tracking

## Report

**What was built** — Full-stack habit tracking. Backend: Habit model, CRUD+complete+today endpoints, vault storage. Frontend: data layer, HabitListScreen, HabitEditScreen, Dashboard integration, FAB menu.

**Verification** — go build/test pass. Gradle build successful. APK installed.

**Journey log** — Habits share tasks/ vault, filtered by type. Backend computes todayCompleted.

## [S1] Problem
Users need to track recurring habits (exercise, reading, meditation, etc.) that repeat on specific days of the week. Tasks with weekly recurrence are not the right abstraction — habits have no date, no dateMode, always use weekly recurrence, and need a dedicated UI for day-of-week selection.

## [S2] Design

### D1: Backend — Habit Model
- Add `TypeHabit = "habit"` to `backend/internal/model/common.go`
- Create `backend/internal/model/habit.go`:
  ```go
  type Habit struct {
      BaseEntity     `yaml:",inline" json:",inline"`
      Title          string      `yaml:"title" json:"title"`
      Icon           string      `yaml:"icon,omitempty" json:"icon,omitempty"`
      Location       string      `yaml:"location,omitempty" json:"location,omitempty"`
      Priority       string      `yaml:"priority,omitempty" json:"priority,omitempty"`
      DaysOfWeek     []int       `yaml:"days_of_week" json:"days_of_week"` // 1=Mon..7=Sun
      TimeMode       string      `yaml:"time_mode,omitempty" json:"time_mode,omitempty"`
      StartTime      string      `yaml:"start_time,omitempty" json:"start_time,omitempty"`
      EndTime        string      `yaml:"end_time,omitempty" json:"end_time,omitempty"`
      DurationMinutes int        `yaml:"duration_minutes,omitempty" json:"duration_minutes,omitempty"`
      DueTime        string      `yaml:"due_time,omitempty" json:"due_time,omitempty"`
      Subtasks       []Subtask   `yaml:"subtasks,omitempty" json:"subtasks,omitempty"`
      Body           string      `yaml:"-" json:"body"`
  }
  ```
- Validation: `daysOfWeek` must have 1-7 values (1=Mon..7=Sun), `title` required, `status` defaults to "active"
- Habits stored in `tasks/` directory (same YAML vault), distinguished by `type: habit`
- No dateMode, no recurrence field (always weekly, derived from daysOfWeek)
- No template/occurrence system — habits are standalone entities

### D2: Backend — API Endpoints
- `GET /api/v1/habits` — list all habits
- `GET /api/v1/habits/:id` — get habit detail
- `POST /api/v1/habits` — create habit
- `PUT /api/v1/habits/:id` — update habit
- `DELETE /api/v1/habits/:id` — delete habit
- `POST /api/v1/habits/:id/complete` — mark today's occurrence as completed (writes override JSON like tasks)
- `GET /api/v1/habits/today` — get today's habits with completion status

### D3: Frontend — Data Layer
- Add `Habit` data class to `domain/model/Habit.kt` (matching backend JSON)
- Add `HabitDto` to `data/dto/HabitDto.kt`
- Add `HabitRepository` to `data/repository/HabitRepository.kt`
- Add habit endpoints to `ApiService` and `KtorApiService`

### D4: Frontend — UI
- Add `HabitListScreen` — grid/list of habits with day-of-week indicators, completion checkboxes
- Add `HabitEditScreen` — create/edit form with:
  - Title, icon, location, priority (same as task)
  - Day-of-week selector (7 toggle buttons: M T W T F S S)
  - Time mode (Same as task: None / Due Time / Start/End / Duration)
  - Subtasks, tags, links, body
- Add `HabitViewModel` and `HabitEditViewModel`
- Add habit tab or section to Dashboard (today's habits with completion)
- Add navigation routes for habit screens
- Add "New Habit" option to FAB menu

### D5: Dashboard Integration
- Add a "Today's Habits" section below routine
- Shows habits scheduled for today with checkbox to complete
- Completed habits show with strikethrough and timestamp

## [S3] Out of Scope
- Habit streaks/statistics (future feature)
- Habit reminders/notifications
- Habit templates
- Multi-week or monthly recurrence for habits

## Tasks
- [x] T1: Backend habit model + validation (covers: D1)
- [x] T2: Backend API endpoints (covers: D2)
- [x] T3: Frontend data layer (covers: D3)
- [x] T4: HabitListScreen + HabitEditScreen (covers: D4)
- [x] T5: Dashboard habit section + navigation (covers: D5)
- [x] T6: Verify build + install (covers: all)
