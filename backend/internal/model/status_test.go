package model

import (
	"testing"
	"time"
)

func ptrTime(t time.Time) *time.Time { return &t }

// ---------------------------------------------------------------------------
// A10: Status computation tests
// ---------------------------------------------------------------------------

func TestComputeEffectiveStatus(t *testing.T) {
	t.Parallel()

	jan1 := time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC)
	jan5 := time.Date(2026, 1, 5, 0, 0, 0, 0, time.UTC)
	jan10 := time.Date(2026, 1, 10, 0, 0, 0, 0, time.UTC)
	jan15 := time.Date(2026, 1, 15, 0, 0, 0, 0, time.UTC)

	tests := []struct {
		name     string
		task     *Task
		now      time.Time
		expected EntityStatus
	}{
		// dateMode="" -> fully manual, never overdue
		{
			name:     "anytime task returns stored status",
			task:     &Task{BaseEntity: BaseEntity{Status: StatusPending}, DateMode: ""},
			now:      jan15,
			expected: StatusPending,
		},
		{
			name:     "anytime task completed stays completed",
			task:     &Task{BaseEntity: BaseEntity{Status: StatusCompleted}, DateMode: ""},
			now:      jan15,
			expected: StatusCompleted,
		},

		// Manual completed/expired always wins
		{
			name: "manually completed due_date task stays completed",
			task: &Task{
				BaseEntity: BaseEntity{Status: StatusCompleted},
				DateMode:   DateModeDueDate,
				DueDate:    ptrTime(jan1),
			},
			now:      jan15,
			expected: StatusCompleted,
		},
		{
			name: "manually expired due_date task stays expired",
			task: &Task{
				BaseEntity: BaseEntity{Status: StatusExpired},
				DateMode:   DateModeDueDate,
				DueDate:    ptrTime(jan5),
			},
			now:      jan15,
			expected: StatusExpired,
		},

		// due_date: future -> pending
		{
			name: "due_date future task is pending",
			task: &Task{
				BaseEntity: BaseEntity{Status: StatusPending},
				DateMode:   DateModeDueDate,
				DueDate:    ptrTime(jan15),
			},
			now:      jan10,
			expected: StatusPending,
		},

		// due_date: today -> in-progress
		{
			name: "due_date today task is in-progress",
			task: &Task{
				BaseEntity: BaseEntity{Status: StatusPending},
				DateMode:   DateModeDueDate,
				DueDate:    ptrTime(jan10),
			},
			now:      jan10,
			expected: StatusInProgress,
		},

		// due_date: past incomplete -> expired (CHANGED from completed)
		{
			name: "due_date past incomplete task is expired",
			task: &Task{
				BaseEntity: BaseEntity{Status: StatusPending},
				DateMode:   DateModeDueDate,
				DueDate:    ptrTime(jan1),
			},
			now:      jan10,
			expected: StatusExpired,
		},

		// range: before start -> pending
		{
			name: "range before start is pending",
			task: &Task{
				BaseEntity: BaseEntity{Status: StatusPending},
				DateMode:   DateModeRange,
				StartDate:  ptrTime(jan5),
				EndDate:    ptrTime(jan10),
			},
			now:      jan1,
			expected: StatusPending,
		},

		// range: during -> in-progress
		{
			name: "range during is in-progress",
			task: &Task{
				BaseEntity: BaseEntity{Status: StatusPending},
				DateMode:   DateModeRange,
				StartDate:  ptrTime(jan1),
				EndDate:    ptrTime(jan10),
			},
			now:      jan5,
			expected: StatusInProgress,
		},

		// range: past end incomplete -> expired (CHANGED from completed)
		{
			name: "range past end incomplete task is expired",
			task: &Task{
				BaseEntity: BaseEntity{Status: StatusPending},
				DateMode:   DateModeRange,
				StartDate:  ptrTime(jan1),
				EndDate:    ptrTime(jan5),
			},
			now:      jan10,
			expected: StatusExpired,
		},

		// due_time: expired at dueTime
		{
			name: "due_time expired at dueTime",
			task: &Task{
				BaseEntity: BaseEntity{Status: StatusPending},
				DateMode:   DateModeDueDate,
				TimeMode:   TimeModeDueTime,
				DueDate:    ptrTime(jan10),
				DueTime:    "14:00",
			},
			now:      time.Date(2026, 1, 10, 15, 0, 0, 0, time.UTC),
			expected: StatusExpired,
		},
		{
			name: "due_time in-progress before dueTime",
			task: &Task{
				BaseEntity: BaseEntity{Status: StatusPending},
				DateMode:   DateModeDueDate,
				TimeMode:   TimeModeDueTime,
				DueDate:    ptrTime(jan10),
				DueTime:    "14:00",
			},
			now:      time.Date(2026, 1, 10, 10, 0, 0, 0, time.UTC),
			expected: StatusInProgress,
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			got := ComputeEffectiveStatus(tc.task, tc.now)
			if got != tc.expected {
				t.Errorf("ComputeEffectiveStatus() = %q, want %q", got, tc.expected)
			}
		})
	}
}

// ---------------------------------------------------------------------------
// A10: TimeBucket computation tests
// ---------------------------------------------------------------------------

func TestComputeTimeBucket(t *testing.T) {
	t.Parallel()

	jan1 := time.Date(2026, 1, 1, 0, 0, 0, 0, time.UTC)
	jan7 := time.Date(2026, 1, 7, 0, 0, 0, 0, time.UTC) // a Tuesday
	jan8 := time.Date(2026, 1, 8, 0, 0, 0, 0, time.UTC) // tomorrow relative to Jan 7
	jan10 := time.Date(2026, 1, 10, 0, 0, 0, 0, time.UTC)
	jan20 := time.Date(2026, 1, 20, 0, 0, 0, 0, time.UTC)

	tests := []struct {
		name            string
		task            *Task
		effectiveStatus EntityStatus
		now             time.Time
		expected        int
	}{
		{
			name:            "completed -> TimeBucketCompleted",
			task:            &Task{DateMode: DateModeDueDate, DueDate: ptrTime(jan1)},
			effectiveStatus: StatusCompleted,
			now:             jan10,
			expected:        TimeBucketCompleted,
		},
		{
			name:            "anytime -> TimeBucketAnytime",
			task:            &Task{DateMode: ""},
			effectiveStatus: StatusPending,
			now:             jan10,
			expected:        TimeBucketAnytime,
		},
		{
			name:            "expired -> TimeBucketOverdue",
			task:            &Task{DateMode: DateModeDueDate, DueDate: ptrTime(jan1)},
			effectiveStatus: StatusExpired,
			now:             jan10,
			expected:        TimeBucketOverdue,
		},
		{
			name:            "due today -> TimeBucketToday",
			task:            &Task{DateMode: DateModeDueDate, DueDate: ptrTime(jan7)},
			effectiveStatus: StatusInProgress,
			now:             jan7,
			expected:        TimeBucketToday,
		},
		{
			name:            "due tomorrow -> TimeBucketTomorrow",
			task:            &Task{DateMode: DateModeDueDate, DueDate: ptrTime(jan8)},
			effectiveStatus: StatusPending,
			now:             jan7,
			expected:        TimeBucketTomorrow,
		},
		{
			name:            "due in 3 days -> TimeBucketNextWeek",
			task:            &Task{DateMode: DateModeDueDate, DueDate: ptrTime(jan10)},
			effectiveStatus: StatusPending,
			now:             jan7,
			expected:        TimeBucketNextWeek,
		},
		{
			name:            "due in 13 days -> TimeBucketLater",
			task:            &Task{DateMode: DateModeDueDate, DueDate: ptrTime(jan20)},
			effectiveStatus: StatusPending,
			now:             jan7,
			expected:        TimeBucketLater,
		},
		{
			name:            "range start today -> TimeBucketToday",
			task:            &Task{DateMode: DateModeRange, StartDate: ptrTime(jan7), EndDate: ptrTime(jan10)},
			effectiveStatus: StatusInProgress,
			now:             jan7,
			expected:        TimeBucketToday,
		},
		{
			name:            "past due date -> TimeBucketOverdue",
			task:            &Task{DateMode: DateModeDueDate, DueDate: ptrTime(jan1)},
			effectiveStatus: StatusPending,
			now:             jan7,
			expected:        TimeBucketOverdue,
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			got := ComputeTimeBucket(tc.task, tc.effectiveStatus, tc.now)
			if got != tc.expected {
				t.Errorf("ComputeTimeBucket() = %d, want %d", got, tc.expected)
			}
		})
	}
}

// ---------------------------------------------------------------------------
// A10: SortKey ordering tests — the A6 worked example
// ---------------------------------------------------------------------------

func TestSortKeyOrdering(t *testing.T) {
	t.Parallel()

	jan20 := time.Date(2026, 1, 20, 0, 0, 0, 0, time.UTC)

	// A6 worked example: A: overdue/low, B: today/urgent, C: today/low,
	// D: tomorrow/urgent, E: no-date/urgent
	type taskDef struct {
		name     string
		status   EntityStatus
		priority string
		dateMode string
		dueDate  *time.Time
		now      time.Time
	}

	defs := []taskDef{
		{"A", StatusExpired, PriorityLow, DateModeDueDate, ptrTime(jan20.AddDate(0, 0, -5)), jan20},
		{"B", StatusInProgress, PriorityUrgent, DateModeDueDate, ptrTime(jan20), jan20},
		{"C", StatusInProgress, PriorityLow, DateModeDueDate, ptrTime(jan20), jan20},
		{"D", StatusPending, PriorityUrgent, DateModeDueDate, ptrTime(jan20.AddDate(0, 0, 1)), jan20},
		{"E", StatusPending, PriorityUrgent, "", nil, jan20},
	}

	type result struct {
		name    string
		sortKey string
	}

	results := make([]result, 0, len(defs))
	for _, d := range defs {
		task := &Task{
			BaseEntity: BaseEntity{Status: d.status},
			DateMode:   d.dateMode,
			DueDate:    d.dueDate,
			Priority:   d.priority,
		}
		task.EffectiveStatus = ComputeEffectiveStatus(task, d.now)
		EnrichTask(task, d.now)
		results = append(results, result{name: d.name, sortKey: task.SortKey})
	}

	// Expected order: A, B, C, D, E
	expectedOrder := []string{"A", "B", "C", "D", "E"}
	for i, name := range expectedOrder {
		if results[i].name != name {
			t.Errorf("position %d: got %q (sortKey=%s), want %q", i, results[i].name, results[i].sortKey, name)
		}
	}

	// Verify no-date urgent stays in bucket 5
	taskE := &Task{
		BaseEntity: BaseEntity{Status: StatusPending},
		DateMode:   "",
		Priority:   PriorityUrgent,
	}
	EnrichTask(taskE, jan20)
	if taskE.TimeBucket != TimeBucketAnytime {
		t.Errorf("no-date urgent task: TimeBucket = %d, want %d (Anytime)", taskE.TimeBucket, TimeBucketAnytime)
	}
}

// ---------------------------------------------------------------------------
// A10: Priority weight tests
// ---------------------------------------------------------------------------

func TestPriorityWeightValue(t *testing.T) {
	t.Parallel()

	tests := []struct {
		priority string
		expected int
	}{
		{PriorityUrgent, 4},
		{PriorityHigh, 3},
		{PriorityMedium, 2},
		{PriorityLow, 1},
		{PriorityNone, 0},
		{"", 0},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.priority, func(t *testing.T) {
			t.Parallel()
			got := PriorityWeightValue(tc.priority)
			if got != tc.expected {
				t.Errorf("PriorityWeightValue(%q) = %d, want %d", tc.priority, got, tc.expected)
			}
		})
	}
}

// ---------------------------------------------------------------------------
// A10: SortKey format tests
// ---------------------------------------------------------------------------

func TestComputeSortKey(t *testing.T) {
	t.Parallel()

	jan15 := time.Date(2026, 1, 15, 0, 0, 0, 0, time.UTC)

	tests := []struct {
		name           string
		timeBucket     int
		priorityWeight int
		dueDate        *time.Time
		expected       string
	}{
		{
			name:           "overdue urgent with date",
			timeBucket:     TimeBucketOverdue,
			priorityWeight: 4,
			dueDate:        ptrTime(jan15),
			expected:       "0|4|2026-01-15",
		},
		{
			name:           "today low with date",
			timeBucket:     TimeBucketToday,
			priorityWeight: 1,
			dueDate:        ptrTime(jan15),
			expected:       "1|1|2026-01-15",
		},
		{
			name:           "anytime urgent without date",
			timeBucket:     TimeBucketAnytime,
			priorityWeight: 4,
			dueDate:        nil,
			expected:       "5|4|9999-12-31",
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			got := ComputeSortKey(tc.timeBucket, tc.priorityWeight, tc.dueDate)
			if got != tc.expected {
				t.Errorf("ComputeSortKey() = %q, want %q", got, tc.expected)
			}
		})
	}
}
