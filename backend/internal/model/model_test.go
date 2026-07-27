package model

import (
	"testing"
	"time"

	"github.com/google/uuid"
)

func TestNoteValidate(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name    string
		note    *Note
		wantErr bool
		errMsg  string
	}{
		{
			name: "valid note",
			note: func() *Note {
				n := NewNote(uuid.NewString(), "Test Note")
				return n
			}(),
			wantErr: false,
		},
		{
			name: "missing title",
			note: func() *Note {
				n := NewNote(uuid.NewString(), "")
				n.Title = ""
				return n
			}(),
			wantErr: true,
			errMsg:  "note.title is required",
		},
		{
			name: "empty id",
			note: func() *Note {
				n := NewNote("", "Test")
				n.ID = ""
				return n
			}(),
			wantErr: true,
			errMsg:  "id is required",
		},
		{
			name: "empty type",
			note: func() *Note {
				n := NewNote(uuid.NewString(), "Test")
				n.Type = ""
				return n
			}(),
			wantErr: true,
			errMsg:  "type is required",
		},
		{
			name: "empty status",
			note: func() *Note {
				n := NewNote(uuid.NewString(), "Test")
				n.Status = ""
				return n
			}(),
			wantErr: true,
			errMsg:  "status is required",
		},
		{
			name: "zero updated_at",
			note: func() *Note {
				n := NewNote(uuid.NewString(), "Test")
				n.UpdatedAt = time.Time{}
				return n
			}(),
			wantErr: true,
			errMsg:  "updated_at is required",
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			err := tc.note.Validate()
			if tc.wantErr {
				if err == nil {
					t.Fatal("expected error, got nil")
				}
				if err.Error() != tc.errMsg {
					t.Fatalf("expected error %q, got %q", tc.errMsg, err.Error())
				}
				if !IsValidationError(err) {
					t.Fatalf("expected ValidationError, got %T", err)
				}
			} else {
				if err != nil {
					t.Fatalf("unexpected error: %v", err)
				}
			}
		})
	}
}

func TestTaskValidate(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name    string
		task    *Task
		wantErr bool
		errMsg  string
	}{
		{
			name:    "valid task",
			task:    NewTask(uuid.NewString(), "Test Task"),
			wantErr: false,
		},
		{
			name: "missing title",
			task: func() *Task {
				t := NewTask(uuid.NewString(), "")
				t.Title = ""
				return t
			}(),
			wantErr: true,
			errMsg:  "task.title is required",
		},
		{
			name: "empty id",
			task: func() *Task {
				t := NewTask("", "Test")
				t.ID = ""
				return t
			}(),
			wantErr: true,
			errMsg:  "id is required",
		},
		{
			name: "empty type",
			task: func() *Task {
				t := NewTask(uuid.NewString(), "Test")
				t.Type = ""
				return t
			}(),
			wantErr: true,
			errMsg:  "type is required",
		},
		{
			name: "empty status",
			task: func() *Task {
				t := NewTask(uuid.NewString(), "Test")
				t.Status = ""
				return t
			}(),
			wantErr: true,
			errMsg:  "status is required",
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			err := tc.task.Validate()
			if tc.wantErr {
				if err == nil {
					t.Fatal("expected error, got nil")
				}
				if err.Error() != tc.errMsg {
					t.Fatalf("expected error %q, got %q", tc.errMsg, err.Error())
				}
				if !IsValidationError(err) {
					t.Fatalf("expected ValidationError, got %T", err)
				}
			} else {
				if err != nil {
					t.Fatalf("unexpected error: %v", err)
				}
			}
		})
	}
}

func TestQuickTaskValidate(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name    string
		qt      *QuickTask
		wantErr bool
		errMsg  string
	}{
		{
			name:    "valid quick task",
			qt:      NewQuickTask(uuid.NewString(), "Buy milk"),
			wantErr: false,
		},
		{
			name: "missing title",
			qt: func() *QuickTask {
				q := NewQuickTask(uuid.NewString(), "")
				q.Title = ""
				return q
			}(),
			wantErr: true,
			errMsg:  "quick-task.title is required",
		},
		{
			name: "empty id",
			qt: func() *QuickTask {
				q := NewQuickTask("", "Buy milk")
				q.ID = ""
				return q
			}(),
			wantErr: true,
			errMsg:  "id is required",
		},
		{
			name: "empty type",
			qt: func() *QuickTask {
				q := NewQuickTask(uuid.NewString(), "Buy milk")
				q.Type = ""
				return q
			}(),
			wantErr: true,
			errMsg:  "type is required",
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			err := tc.qt.Validate()
			if tc.wantErr {
				if err == nil {
					t.Fatal("expected error, got nil")
				}
				if err.Error() != tc.errMsg {
					t.Fatalf("expected error %q, got %q", tc.errMsg, err.Error())
				}
				if !IsValidationError(err) {
					t.Fatalf("expected ValidationError, got %T", err)
				}
			} else {
				if err != nil {
					t.Fatalf("unexpected error: %v", err)
				}
			}
		})
	}
}

func TestPersonValidate(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name    string
		person  *Person
		wantErr bool
		errMsg  string
	}{
		{
			name:    "valid person",
			person:  NewPerson(uuid.NewString(), "John Doe"),
			wantErr: false,
		},
		{
			name: "missing name",
			person: func() *Person {
				p := NewPerson(uuid.NewString(), "")
				p.Name = ""
				return p
			}(),
			wantErr: true,
			errMsg:  "person.name is required",
		},
		{
			name: "empty id",
			person: func() *Person {
				p := NewPerson("", "John Doe")
				p.ID = ""
				return p
			}(),
			wantErr: true,
			errMsg:  "id is required",
		},
		{
			name: "empty type",
			person: func() *Person {
				p := NewPerson(uuid.NewString(), "John Doe")
				p.Type = ""
				return p
			}(),
			wantErr: true,
			errMsg:  "type is required",
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			err := tc.person.Validate()
			if tc.wantErr {
				if err == nil {
					t.Fatal("expected error, got nil")
				}
				if err.Error() != tc.errMsg {
					t.Fatalf("expected error %q, got %q", tc.errMsg, err.Error())
				}
				if !IsValidationError(err) {
					t.Fatalf("expected ValidationError, got %T", err)
				}
			} else {
				if err != nil {
					t.Fatalf("unexpected error: %v", err)
				}
			}
		})
	}
}

func TestNewNoteDefaults(t *testing.T) {
	id := uuid.NewString()
	n := NewNote(id, "My Note")

	if n.ID != id {
		t.Fatalf("ID = %q, want %q", n.ID, id)
	}
	if n.Type != TypeNote {
		t.Fatalf("Type = %q, want %q", n.Type, TypeNote)
	}
	if n.Status != StatusActive {
		t.Fatalf("Status = %q, want %q", n.Status, StatusActive)
	}
	if n.Title != "My Note" {
		t.Fatalf("Title = %q, want %q", n.Title, "My Note")
	}
	if n.CreatedAt.IsZero() {
		t.Fatal("CreatedAt should not be zero")
	}
	if n.UpdatedAt.IsZero() {
		t.Fatal("UpdatedAt should not be zero")
	}
	if n.Tags == nil {
		t.Fatal("Tags should be initialized to empty slice, not nil")
	}
	if n.Links == nil {
		t.Fatal("Links should be initialized to empty slice, not nil")
	}
}

func TestNewTaskDefaults(t *testing.T) {
	id := uuid.NewString()
	task := NewTask(id, "Write docs")

	if task.ID != id {
		t.Fatalf("ID = %q, want %q", task.ID, id)
	}
	if task.Type != TypeTask {
		t.Fatalf("Type = %q, want %q", task.Type, TypeTask)
	}
	if task.Status != StatusPending {
		t.Fatalf("Status = %q, want %q", task.Status, StatusPending)
	}
	if task.Title != "Write docs" {
		t.Fatalf("Title = %q, want %q", task.Title, "Write docs")
	}
	if task.Subtasks == nil {
		t.Fatal("Subtasks should be initialized to empty slice, not nil")
	}
}

func TestNewQuickTaskDefaults(t *testing.T) {
	id := uuid.NewString()
	qt := NewQuickTask(id, "Buy milk")

	if qt.ID != id {
		t.Fatalf("ID = %q, want %q", qt.ID, id)
	}
	if qt.Type != TypeQuickTask {
		t.Fatalf("Type = %q, want %q", qt.Type, TypeQuickTask)
	}
	if qt.Title != "Buy milk" {
		t.Fatalf("Title = %q, want %q", qt.Title, "Buy milk")
	}
}

func TestNewPersonDefaults(t *testing.T) {
	id := uuid.NewString()
	p := NewPerson(id, "John Doe")

	if p.ID != id {
		t.Fatalf("ID = %q, want %q", p.ID, id)
	}
	if p.Type != TypePerson {
		t.Fatalf("Type = %q, want %q", p.Type, TypePerson)
	}
	if p.Status != StatusActive {
		t.Fatalf("Status = %q, want %q", p.Status, StatusActive)
	}
	if p.Name != "John Doe" {
		t.Fatalf("Name = %q, want %q", p.Name, "John Doe")
	}
	if p.Contacts == nil {
		t.Fatal("Contacts should be initialized to empty slice, not nil")
	}
	if p.SocialLinks == nil {
		t.Fatal("SocialLinks should be initialized to empty slice, not nil")
	}
}

func TestValidationError(t *testing.T) {
	t.Parallel()

	err := NewValidationError("test error")
	if err == nil {
		t.Fatal("expected error, got nil")
	}
	if err.Error() != "test error" {
		t.Fatalf("error = %q, want %q", err.Error(), "test error")
	}
	if !IsValidationError(err) {
		t.Fatal("expected IsValidationError to be true")
	}

	otherErr := NewConflictError("conflict")
	if IsValidationError(otherErr) {
		t.Fatal("expected IsValidationError to be false for ConflictError")
	}
}

func TestConflictError(t *testing.T) {
	t.Parallel()

	err := NewConflictError("conflict error")
	if err == nil {
		t.Fatal("expected error, got nil")
	}
	if !IsConflictError(err) {
		t.Fatal("expected IsConflictError to be true")
	}
}

func TestNotFoundError(t *testing.T) {
	t.Parallel()

	err := NewNotFoundError("not found")
	if err == nil {
		t.Fatal("expected error, got nil")
	}
	if !IsNotFoundError(err) {
		t.Fatal("expected IsNotFoundError to be true")
	}
}

func TestHabitValidate(t *testing.T) {
	t.Parallel()

	tests := []struct {
		name    string
		habit   *Habit
		wantErr bool
		errMsg  string
	}{
		{
			name: "valid habit",
			habit: func() *Habit {
				h := NewHabit(uuid.NewString(), "Exercise")
				h.DaysOfWeek = []int{1, 2, 3}
				return h
			}(),
			wantErr: false,
		},
		{
			name: "missing title",
			habit: func() *Habit {
				h := NewHabit(uuid.NewString(), "")
				h.Title = ""
				return h
			}(),
			wantErr: true,
			errMsg:  "habit.title is required",
		},
		{
			name: "empty id",
			habit: func() *Habit {
				h := NewHabit("", "Exercise")
				h.ID = ""
				return h
			}(),
			wantErr: true,
			errMsg:  "id is required",
		},
		{
			name: "empty type",
			habit: func() *Habit {
				h := NewHabit(uuid.NewString(), "Exercise")
				h.Type = ""
				return h
			}(),
			wantErr: true,
			errMsg:  "type is required",
		},
		{
			name: "empty days_of_week",
			habit: func() *Habit {
				h := NewHabit(uuid.NewString(), "Exercise")
				h.DaysOfWeek = []int{}
				return h
			}(),
			wantErr: true,
			errMsg:  "habit.days_of_week is required (at least 1 day)",
		},
		{
			name: "invalid day value 0",
			habit: func() *Habit {
				h := NewHabit(uuid.NewString(), "Exercise")
				h.DaysOfWeek = []int{0}
				return h
			}(),
			wantErr: true,
			errMsg:  "habit.days_of_week values must be 1-7 (Mon-Sun)",
		},
		{
			name: "invalid day value 8",
			habit: func() *Habit {
				h := NewHabit(uuid.NewString(), "Exercise")
				h.DaysOfWeek = []int{8}
				return h
			}(),
			wantErr: true,
			errMsg:  "habit.days_of_week values must be 1-7 (Mon-Sun)",
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			err := tc.habit.Validate()
			if tc.wantErr {
				if err == nil {
					t.Fatal("expected error, got nil")
				}
				if err.Error() != tc.errMsg {
					t.Fatalf("expected error %q, got %q", tc.errMsg, err.Error())
				}
				if !IsValidationError(err) {
					t.Fatalf("expected ValidationError, got %T", err)
				}
			} else {
				if err != nil {
					t.Fatalf("unexpected error: %v", err)
				}
			}
		})
	}
}

func TestNewHabitDefaults(t *testing.T) {
	id := uuid.NewString()
	h := NewHabit(id, "Meditate")

	if h.ID != id {
		t.Fatalf("ID = %q, want %q", h.ID, id)
	}
	if h.Type != TypeHabit {
		t.Fatalf("Type = %q, want %q", h.Type, TypeHabit)
	}
	if h.Status != StatusActive {
		t.Fatalf("Status = %q, want %q", h.Status, StatusActive)
	}
	if h.Title != "Meditate" {
		t.Fatalf("Title = %q, want %q", h.Title, "Meditate")
	}
	if h.DaysOfWeek == nil {
		t.Fatal("DaysOfWeek should be initialized to empty slice, not nil")
	}
	if h.Subtasks == nil {
		t.Fatal("Subtasks should be initialized to empty slice, not nil")
	}
	if h.Tags == nil {
		t.Fatal("Tags should be initialized to empty slice, not nil")
	}
	if h.Links == nil {
		t.Fatal("Links should be initialized to empty slice, not nil")
	}
}
