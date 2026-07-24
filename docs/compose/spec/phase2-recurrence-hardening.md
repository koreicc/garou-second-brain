---
feature: phase2-recurrence-hardening
status: delivered
updated: 2026-07-24
branch: feat/phase1-backend-semantics
commits: 60844c1..fffaa46
---

# Phase 2: Recurrence Hardening

## Report

**What was built** — Monthly and yearly recurrence now correctly clamp and restore day-of-month. `GenerateDatesInRange` remembers the original day from the start date and applies clamping per iteration: Jan 31 → Feb 28 → Mar 31 → Apr 30. Yearly handles Feb 29 → Feb 28 in non-leap years, and restores to 29 in leap years.

**Verification** — `go vet ./...` and `go build ./...` pass. A10 recurrence tests include full clamping cycle, interval 2, yearly Feb 29 across leap boundaries.

**Journey log** — Go's `time.AddDate` already clamps (Jan 31 + 1mo = Feb 28), but doesn't restore the original day on the next iteration (Feb 28 + 1mo = Mar 28). The fix stores `targetDay` and re-applies it with per-month last-day clamping.

## [S1] Problem
Monthly recurrence starting on Jan 31 would produce Jan 31 → Feb 28 → Mar 28 → Apr 28 instead of Jan 31 → Feb 28 → Mar 31 → Apr 30. Yearly Feb 29 didn't restore to 29 in leap years.

## [S2] Design
- `GenerateDatesInRange` monthly case: store `targetDay = start.Day()`, after each `AddDate(0, interval, 0)` check if day changed, restore `targetDay` clamped to month's last day.
- `GenerateDatesInRange` yearly case: same pattern — after `AddDate(interval, 0, 0)`, if day changed, restore original day clamped to month's last day.
- `DateMatchesRecurrence` unchanged — it checks month arithmetic, not specific dates.

## [S3] Out of Scope
- Template edit future-only semantics (separate concern)
- UI changes
- Schema migration

## Tasks
- [x] T1: Fix monthly clamping in GenerateDatesInRange — acceptance: Jan 31 → Feb 28 → Mar 31 → Apr 30 (covers: S2)
- [x] T2: Fix yearly Feb 29 clamping — acceptance: Feb 29 → Feb 28 → Feb 29 across leap boundaries (covers: S2)
- [x] T3: Add A10 recurrence tests — acceptance: full clamping cycle, interval 2, yearly Feb 29 tests pass (covers: S2)
- [x] T4: Validate — acceptance: go vet and go build pass (covers: all)
