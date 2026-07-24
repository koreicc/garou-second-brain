package vault

import (
	"testing"
	"time"

	"github.com/koreicc/garou-second-brain/backend/internal/model"
)

func ptrTime(t time.Time) *time.Time { return &t }

// ---------------------------------------------------------------------------
// A10: Daily recurrence tests
// ---------------------------------------------------------------------------

func TestDailyRecurrence(t *testing.T) {
	t.Parallel()

	start := time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC)
	end := time.Date(2026, 1, 10, 0, 0, 0, 0, time.UTC)

	t.Run("interval 1 every day", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "t1", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(end),
			Recurrence: &model.Recurrence{Type: "daily", Interval: 1},
		}
		dates := GenerateDatesInRange(start, end, tmpl.Recurrence)
		if len(dates) != 10 {
			t.Errorf("interval 1: got %d dates, want 10", len(dates))
		}
		// Verify first and last
		if dates[0] != "2026-01-01" {
			t.Errorf("first date = %s, want 2026-01-01", dates[0])
		}
		if dates[9] != "2026-01-10" {
			t.Errorf("last date = %s, want 2026-01-10", dates[9])
		}
	})

	t.Run("interval 3 every 3 days", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "t2", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(end),
			Recurrence: &model.Recurrence{Type: "daily", Interval: 3},
		}
		dates := GenerateDatesInRange(start, end, tmpl.Recurrence)
		expected := []string{"2026-01-01", "2026-01-04", "2026-01-07", "2026-01-10"}
		if len(dates) != len(expected) {
			t.Errorf("interval 3: got %d dates, want %d", len(dates), len(expected))
		}
		for i, d := range dates {
			if i < len(expected) && d != expected[i] {
				t.Errorf("date[%d] = %s, want %s", i, d, expected[i])
			}
		}
	})

	t.Run("endDate inclusion", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "t3", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(start), // endDate = startDate
			Recurrence: &model.Recurrence{Type: "daily", Interval: 1},
		}
		dates := GenerateDatesInRange(start, start, tmpl.Recurrence)
		if len(dates) != 1 {
			t.Errorf("single-day window: got %d dates, want 1", len(dates))
		}
		if len(dates) == 1 && dates[0] != "2026-01-01" {
			t.Errorf("single-day date = %s, want 2026-01-01", dates[0])
		}
	})
}

// ---------------------------------------------------------------------------
// A10: Weekly recurrence tests
// ---------------------------------------------------------------------------

func TestWeeklyRecurrence(t *testing.T) {
	t.Parallel()

	// Jan 1 2026 is a Thursday (weekday=4)
	start := time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC)
	end := time.Date(2026, 1, 31, 0, 0, 0, 0, time.UTC)

	t.Run("interval 1 with daysOfWeek=[1,3,5] (Mon,Wed,Fri)", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "w1", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(end),
			Recurrence: &model.Recurrence{Type: "weekly", Interval: 1, DaysOfWeek: []int{1, 3, 5}},
		}
		dates := GenerateDatesInRange(start, end, tmpl.Recurrence)
		// Week 0 (Jan 1-4): Thu(4) not matched, only check Mon-Fri
		// Jan 1=Thu(4), Jan 2=Fri(5) -> match
		// Jan 3=Sat(6), Jan 4=Sun(0)
		// Jan 5=Mon(1) -> match, Jan 6=Tue(2), Jan 7=Wed(3) -> match, Jan 8=Thu(4), Jan 9=Fri(5) -> match
		// ... continues every week
		for _, d := range dates {
			parsed, _ := time.Parse("2006-01-02", d)
			wd := parsed.Weekday()
			if wd != time.Monday && wd != time.Wednesday && wd != time.Friday {
				t.Errorf("date %s is %s, expected Mon/Wed/Fri", d, wd)
			}
		}
		// Should have matches in each week
		if len(dates) < 10 {
			t.Errorf("expected at least 10 dates, got %d", len(dates))
		}
	})

	t.Run("interval 2 anchoring", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "w2", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(end),
			Recurrence: &model.Recurrence{Type: "weekly", Interval: 2},
		}
		dates := GenerateDatesInRange(start, end, tmpl.Recurrence)
		// Interval 2 means every 14 days from start
		// Jan 1, Jan 15, Jan 29
		expected := []string{"2026-01-01", "2026-01-15", "2026-01-29"}
		if len(dates) != len(expected) {
			t.Errorf("interval 2: got %d dates, want %d", len(dates), len(expected))
		}
		for i, d := range dates {
			if i < len(expected) && d != expected[i] {
				t.Errorf("date[%d] = %s, want %s", i, d, expected[i])
			}
		}
	})

	t.Run("empty daysOfWeek every 7*interval days", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "w3", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(end),
			Recurrence: &model.Recurrence{Type: "weekly", Interval: 1, DaysOfWeek: []int{}},
		}
		dates := GenerateDatesInRange(start, end, tmpl.Recurrence)
		// Every 7 days: Jan 1, Jan 8, Jan 15, Jan 22, Jan 29
		expected := []string{"2026-01-01", "2026-01-08", "2026-01-15", "2026-01-22", "2026-01-29"}
		if len(dates) != len(expected) {
			t.Errorf("empty daysOfWeek: got %d dates, want %d", len(dates), len(expected))
		}
		for i, d := range dates {
			if i < len(expected) && d != expected[i] {
				t.Errorf("date[%d] = %s, want %s", i, d, expected[i])
			}
		}
	})

	t.Run("start day not in daysOfWeek", func(t *testing.T) {
		t.Parallel()
		// Jan 1 is Thursday (4), daysOfWeek=[1,3,5] (Mon,Wed,Fri)
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "w4", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(end),
			Recurrence: &model.Recurrence{Type: "weekly", Interval: 1, DaysOfWeek: []int{1, 3, 5}},
		}
		dates := GenerateDatesInRange(start, end, tmpl.Recurrence)
		// Jan 1 (Thu) should NOT be in the results
		for _, d := range dates {
			if d == "2026-01-01" {
				t.Error("start date 2026-01-01 (Thursday) should not match daysOfWeek=[1,3,5]")
			}
		}
	})
}

// ---------------------------------------------------------------------------
// A10: Monthly recurrence tests
// ---------------------------------------------------------------------------

func TestMonthlyRecurrence(t *testing.T) {
	t.Parallel()

	t.Run("Jan 31 clamps to Feb 28", func(t *testing.T) {
		t.Parallel()
		start := time.Date(2026, 1, 31, 0, 0, 0, 0, time.UTC)
		end := time.Date(2026, 4, 30, 0, 0, 0, 0, time.UTC)
		rec := &model.Recurrence{Type: "monthly", Interval: 1}
		dates := GenerateDatesInRange(start, end, rec)
		// Jan 31, Feb 28 (2026 not leap), Mar 31, Apr 30
		expected := []string{"2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30"}
		if len(dates) != len(expected) {
			t.Errorf("monthly clamp: got %d dates, want %d", len(dates), len(expected))
		}
		for i, d := range dates {
			if i < len(expected) && d != expected[i] {
				t.Errorf("date[%d] = %s, want %s", i, d, expected[i])
			}
		}
	})

	t.Run("interval 2 every other month", func(t *testing.T) {
		t.Parallel()
		start := time.Date(2026, 1, 15, 0, 0, 0, 0, time.UTC)
		end := time.Date(2026, 12, 31, 0, 0, 0, 0, time.UTC)
		rec := &model.Recurrence{Type: "monthly", Interval: 2}
		dates := GenerateDatesInRange(start, end, rec)
		expected := []string{"2026-01-15", "2026-03-15", "2026-05-15", "2026-07-15", "2026-09-15", "2026-11-15"}
		if len(dates) != len(expected) {
			t.Errorf("monthly interval 2: got %d dates, want %d", len(dates), len(expected))
		}
		for i, d := range dates {
			if i < len(expected) && d != expected[i] {
				t.Errorf("date[%d] = %s, want %s", i, d, expected[i])
			}
		}
	})
}

// ---------------------------------------------------------------------------
// A10: Yearly recurrence tests
// ---------------------------------------------------------------------------

func TestYearlyRecurrence(t *testing.T) {
	t.Parallel()

	t.Run("Feb 29 -> Feb 28 in non-leap year", func(t *testing.T) {
		t.Parallel()
		// 2028 is a leap year (Feb 29 exists), 2027 and 2029 are not
		start := time.Date(2028, 2, 29, 0, 0, 0, 0, time.UTC)
		end := time.Date(2030, 3, 1, 0, 0, 0, 0, time.UTC)
		rec := &model.Recurrence{Type: "yearly", Interval: 1}
		dates := GenerateDatesInRange(start, end, rec)
		// 2028-02-29, 2029-02-28, 2030-02-28
		expected := []string{"2028-02-29", "2029-02-28", "2030-02-28"}
		if len(dates) != len(expected) {
			t.Errorf("yearly Feb 29: got %d dates, want %d", len(dates), len(expected))
		}
		for i, d := range dates {
			if i < len(expected) && d != expected[i] {
				t.Errorf("date[%d] = %s, want %s", i, d, expected[i])
			}
		}
	})
}

// ---------------------------------------------------------------------------
// A10: DateMatchesRecurrence tests
// ---------------------------------------------------------------------------

func TestDateMatchesRecurrence(t *testing.T) {
	t.Parallel()

	start := time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC)
	end := time.Date(2026, 1, 31, 0, 0, 0, 0, time.UTC)

	t.Run("daily interval 1 matches every day", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "d1", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(end),
			Recurrence: &model.Recurrence{Type: "daily", Interval: 1},
		}
		for d := 1; d <= 15; d++ {
			date := time.Date(2026, 1, d, 0, 0, 0, 0, time.UTC)
			if !DateMatchesRecurrence(date, tmpl) {
				t.Errorf("daily interval 1: Jan %d should match", d)
			}
		}
	})

	t.Run("daily interval 3 matches every 3rd day", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "d3", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(end),
			Recurrence: &model.Recurrence{Type: "daily", Interval: 3},
		}
		matches := []int{1, 4, 7, 10, 13, 16, 19, 22, 25, 28}
		for _, d := range matches {
			date := time.Date(2026, 1, d, 0, 0, 0, 0, time.UTC)
			if !DateMatchesRecurrence(date, tmpl) {
				t.Errorf("daily interval 3: Jan %d should match", d)
			}
		}
		nonMatches := []int{2, 3, 5, 6, 8, 9}
		for _, d := range nonMatches {
			date := time.Date(2026, 1, d, 0, 0, 0, 0, time.UTC)
			if DateMatchesRecurrence(date, tmpl) {
				t.Errorf("daily interval 3: Jan %d should NOT match", d)
			}
		}
	})

	t.Run("date before start does not match", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "before", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(end),
			Recurrence: &model.Recurrence{Type: "daily", Interval: 1},
		}
		date := time.Date(2025, 12, 31, 0, 0, 0, 0, time.UTC)
		if DateMatchesRecurrence(date, tmpl) {
			t.Error("date before start should not match")
		}
	})

	t.Run("date after end does not match", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "after", Status: model.StatusPending},
			IsTemplate: true,
			DateMode:   model.DateModeRange,
			StartDate:  ptrTime(start),
			EndDate:    ptrTime(end),
			Recurrence: &model.Recurrence{Type: "daily", Interval: 1},
		}
		date := time.Date(2026, 2, 1, 0, 0, 0, 0, time.UTC)
		if DateMatchesRecurrence(date, tmpl) {
			t.Error("date after end should not match")
		}
	})

	t.Run("nil recurrence returns false", func(t *testing.T) {
		t.Parallel()
		tmpl := &model.Task{
			BaseEntity: model.BaseEntity{ID: "nil", Status: model.StatusPending},
			IsTemplate: true,
		}
		date := time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC)
		if DateMatchesRecurrence(date, tmpl) {
			t.Error("nil recurrence should not match")
		}
	})
}
