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
