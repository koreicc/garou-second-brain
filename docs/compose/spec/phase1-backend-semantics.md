---
feature: phase1-backend-semantics
status: in-progress
updated: 2026-07-24
branch: feat/phase1-backend-semantics
---

# Phase 1: Backend Semantics & A10 Tests

## Report

## [S1] Problem
The current backend has three semantic gaps vs the MASTER_SPEC (Part A):
1. Past-due unfinished tasks auto-complete (`StatusCompleted`) instead of becoming `expired` (A4).
2. No response enrichment fields (`timeBucket`, `sortKey`, `priorityWeight`, `isOverdue`, `isToday`) exist (A6).
3. Missing convenience endpoints for the common task views (A7).
4. No test coverage for recurrence, effective status, or sorting (A10).

## [S2] Design

### A4: Status fix
- `computeDueDateStatus()`: change past-end return from `StatusCompleted` to `StatusExpired`.
- `computeRangeStatus()`: same change.
- `ComputeEffectiveStatus()`: no logic change needed — the sub-functions do the work.
- Breaking change: tasks previously auto-completed by the old logic will now show as `expired` when incomplete. No schema migration needed — persisted `completed` tasks are unaffected.

### A6: Enrichment fields
Add to every task/occurrence in list and single-task responses:

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

Implementation:
- New function `EnrichTask(t *Task, now time.Time, tzOffset int)` in `model/task.go` that computes and sets these fields.
- `timeBucket` values: 0=overdue, 1=today, 2=tomorrow, 3=next-7-days, 4=later, 5=anytime, 6=completed.
- `priorityWeight`: urgent=4, high=3, medium=2, low=1, none=0.
- `sortKey`: `"{timeBucket}|{priorityWeight}|{dueDate}"` (padded for string comparison).
- Applied in `enrichTasks()` in handler/task.go before serializing responses.

### A7: New endpoints
| Method | Path | Handler | Logic |
|---|---|---|---|
| GET | `/tasks/today` | `ListToday` | Load all tasks, filter timeBucket in {0, 1}, apply enrichment, sort by sortKey |
| GET | `/tasks/overdue` | `ListOverdue` | Filter timeBucket == 0 |
| GET | `/tasks/anytime` | `ListAnytime` | Filter dateMode == "" |
| POST | `/tasks/occurrence/:parentId/:date/complete` | `CompleteOccurrence` | Write override with Status=completed |
| POST | `/tasks/occurrence/:parentId/:date/skip` | `SkipOccurrence` | Write override with Status=expired |

### A10: Test coverage
New test file `backend/internal/model/status_test.go`:
- TestComputeEffectiveStatus: table-driven for all dateMode/timeMode/status combinations
- TestTimeBucketComputation: the A6 worked example
- TestSortKeyOrdering: verify sortKey string ordering matches A6

New test file `backend/internal/vault/recurrence_test.go`:
- TestDailyRecurrence: interval 1 & 3, window boundary
- TestWeeklyRecurrence: interval 1 with daysOfWeek, interval 2 anchoring, empty daysOfWeek
- TestMonthlyRecurrence: Jan 31 clamping, interval 2
- TestYearlyRecurrence: Feb 29 → Feb 28 non-leap

## [S3] Out of Scope
- UI changes (Phase 3+)
- Monthly/yearly clamping hardening (Phase 2 — A5)
- Template edit future-only semantics (Phase 2)
- Schema migration (none needed)

## Tasks
- [ ] T1: Fix A4 status — change past-end return to expired — acceptance: `ComputeEffectiveStatus` returns expired for past-due unfinished tasks (covers: S2 A4)
- [ ] T2: Add A6 enrichment fields — acceptance: every task response includes effectiveStatus, timeBucket, sortKey, priorityWeight, isOverdue, isToday (covers: S2 A6)
- [ ] T3: Add A7 convenience endpoints — acceptance: GET /tasks/today, /tasks/overdue, /tasks/anytime return correct filtered lists (covers: S2 A7)
- [ ] T4: Add occurrence complete/skip endpoints — acceptance: POST complete/skip writes override, siblings unaffected (covers: S2 A7)
- [ ] T5: Write A10 status & sorting tests — acceptance: all table-driven tests pass for status computation, timeBucket, and sortKey (covers: S2 A10)
- [ ] T6: Write A10 recurrence tests — acceptance: daily/weekly/monthly/yearly tests pass including edge cases (covers: S2 A10)
- [ ] T7: Write A10 override tests — acceptance: complete/skip one occurrence leaves siblings and template unchanged (covers: S2 A10)
- [ ] T8: Validate — acceptance: `go vet ./...` and `go test -race ./...` pass (covers: all)
