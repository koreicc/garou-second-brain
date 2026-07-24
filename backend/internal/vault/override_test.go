package vault

import (
	"testing"
	"time"

	"github.com/koreicc/garou-second-brain/backend/internal/model"
)

// ---------------------------------------------------------------------------
// A10: Override tests — completing/skipping one occurrence leaves siblings unaffected
// ---------------------------------------------------------------------------

func TestOccurrenceOverrideIsolation(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	// Create a template with weekly recurrence
	tmpl := model.NewTask("tmpl-override-test", "Weekly Standup")
	tmpl.IsTemplate = true
	tmpl.DateMode = model.DateModeRange
	start := time.Date(2026, 1, 5, 0, 0, 0, 0, time.UTC) // Monday
	end := time.Date(2026, 1, 26, 0, 0, 0, 0, time.UTC)
	tmpl.StartDate = ptrTime(start)
	tmpl.EndDate = ptrTime(end)
	tmpl.Recurrence = &model.Recurrence{Type: "weekly", Interval: 1, DaysOfWeek: []int{1}} // Every Monday

	if err := v.WriteTask(tmpl); err != nil {
		t.Fatalf("WriteTask: %v", err)
	}

	// Complete one occurrence (Jan 12)
	override := &OccurrenceOverride{Status: model.TaskStatusCompleted}
	if err := v.WriteOccurrenceOverride(tmpl.ID, "2026-01-12", override); err != nil {
		t.Fatalf("WriteOccurrenceOverride: %v", err)
	}

	// Verify the override was written
	saved, err := v.ReadOccurrenceOverride(tmpl.ID, "2026-01-12")
	if err != nil {
		t.Fatalf("ReadOccurrenceOverride: %v", err)
	}
	if saved.Status != model.TaskStatusCompleted {
		t.Errorf("override status = %q, want %q", saved.Status, model.TaskStatusCompleted)
	}

	// Verify siblings are unaffected — they should have no override
	for _, date := range []string{"2026-01-05", "2026-01-19", "2026-01-26"} {
		sibling, err := v.ReadOccurrenceOverride(tmpl.ID, date)
		if err == nil && sibling != nil {
			t.Errorf("sibling %s has unexpected override: status=%q", date, sibling.Status)
		}
	}

	// Verify computed occurrences still work for non-overridden dates
	occ := ComputeDynamicOccurrence(tmpl, "2026-01-05")
	if occ == nil {
		t.Fatal("ComputeDynamicOccurrence returned nil for Jan 5")
	}
	if occ.Status != model.StatusPending {
		t.Errorf("non-overridden occurrence status = %q, want %q", occ.Status, model.StatusPending)
	}
}

// ---------------------------------------------------------------------------
// A10: Template title edit affects future occurrences, not overridden ones
// ---------------------------------------------------------------------------

func TestTemplateEditPreservesOverrides(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	tmpl := model.NewTask("tmpl-edit-test", "Original Title")
	tmpl.IsTemplate = true
	tmpl.DateMode = model.DateModeRange
	start := time.Date(2026, 2, 1, 0, 0, 0, 0, time.UTC)
	end := time.Date(2026, 2, 28, 0, 0, 0, 0, time.UTC)
	tmpl.StartDate = ptrTime(start)
	tmpl.EndDate = ptrTime(end)
	tmpl.Recurrence = &model.Recurrence{Type: "monthly", Interval: 1}

	if err := v.WriteTask(tmpl); err != nil {
		t.Fatalf("WriteTask: %v", err)
	}

	// Override Feb 1 occurrence with a custom title
	override := &OccurrenceOverride{Title: "Custom Feb 1"}
	if err := v.WriteOccurrenceOverride(tmpl.ID, "2026-02-01", override); err != nil {
		t.Fatalf("WriteOccurrenceOverride: %v", err)
	}

	// Edit the template title
	tmpl.Title = "Updated Title"
	tmpl.UpdatedAt = time.Now().UTC()
	if err := v.WriteTask(tmpl); err != nil {
		t.Fatalf("WriteTask after edit: %v", err)
	}

	// The overridden occurrence should keep its custom title
	occ1 := ComputeDynamicOccurrence(tmpl, "2026-02-01")
	if occ1 == nil {
		t.Fatal("ComputeDynamicOccurrence returned nil for Feb 1")
	}
	if override1, _ := v.ReadOccurrenceOverride(tmpl.ID, "2026-02-01"); override1 != nil {
		ApplyOccurrenceOverride(occ1, override1)
	}
	if occ1.Title != "Custom Feb 1" {
		t.Errorf("overridden occurrence title = %q, want %q", occ1.Title, "Custom Feb 1")
	}

	// A non-overridden future occurrence should get the new template title
	occ2 := ComputeDynamicOccurrence(tmpl, "2026-02-08")
	if occ2 == nil {
		t.Fatal("ComputeDynamicOccurrence returned nil for Feb 8")
	}
	if occ2.Title != "Updated Title" {
		t.Errorf("non-overridden occurrence title = %q, want %q", occ2.Title, "Updated Title")
	}
}

// ---------------------------------------------------------------------------
// A10: Completed occurrence survives template subtask edit
// ---------------------------------------------------------------------------

func TestCompletedOccurrenceSurvivesSubtaskEdit(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	tmpl := model.NewTask("tmpl-subtask-test", "Weekly Review")
	tmpl.IsTemplate = true
	tmpl.DateMode = model.DateModeRange
	start := time.Date(2026, 3, 2, 0, 0, 0, 0, time.UTC) // Monday
	end := time.Date(2026, 3, 30, 0, 0, 0, 0, time.UTC)
	tmpl.StartDate = ptrTime(start)
	tmpl.EndDate = ptrTime(end)
	tmpl.Recurrence = &model.Recurrence{Type: "weekly", Interval: 1, DaysOfWeek: []int{1}}
	tmpl.Subtasks = []model.Subtask{
		{ID: "sub-1", Title: "Original subtask", Completed: false},
	}

	if err := v.WriteTask(tmpl); err != nil {
		t.Fatalf("WriteTask: %v", err)
	}

	// Complete the Mar 9 occurrence
	override := &OccurrenceOverride{
		Status: model.TaskStatusCompleted,
		Subtasks: []model.Subtask{
			{ID: "sub-1", Title: "Original subtask", Completed: true},
		},
	}
	if err := v.WriteOccurrenceOverride(tmpl.ID, "2026-03-09", override); err != nil {
		t.Fatalf("WriteOccurrenceOverride: %v", err)
	}

	// Edit the template: change subtask title and add a new one
	tmpl.Subtasks = []model.Subtask{
		{ID: "sub-1", Title: "Updated subtask", Completed: false},
		{ID: "sub-2", Title: "New subtask", Completed: false},
	}
	tmpl.UpdatedAt = time.Now().UTC()
	if err := v.WriteTask(tmpl); err != nil {
		t.Fatalf("WriteTask after subtask edit: %v", err)
	}

	// The completed occurrence should retain its override subtasks
	occ := ComputeDynamicOccurrence(tmpl, "2026-03-09")
	if occ == nil {
		t.Fatal("ComputeDynamicOccurrence returned nil for Mar 9")
	}
	if savedOverride, _ := v.ReadOccurrenceOverride(tmpl.ID, "2026-03-09"); savedOverride != nil {
		ApplyOccurrenceOverride(occ, savedOverride)
	}
	if occ.Status != model.StatusCompleted {
		t.Errorf("completed occurrence status = %q, want %q", occ.Status, model.StatusCompleted)
	}
	// Override subtasks should be preserved
	if len(occ.Subtasks) != 1 {
		t.Errorf("completed occurrence subtask count = %d, want 1", len(occ.Subtasks))
	} else if occ.Subtasks[0].Title != "Original subtask" {
		t.Errorf("completed occurrence subtask title = %q, want %q", occ.Subtasks[0].Title, "Original subtask")
	}

	// Non-overridden occurrence should get the new subtasks
	occ2 := ComputeDynamicOccurrence(tmpl, "2026-03-16")
	if occ2 == nil {
		t.Fatal("ComputeDynamicOccurrence returned nil for Mar 16")
	}
	if len(occ2.Subtasks) != 2 {
		t.Errorf("non-overridden occurrence subtask count = %d, want 2", len(occ2.Subtasks))
	}
}
