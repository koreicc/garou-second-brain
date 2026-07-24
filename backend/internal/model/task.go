package model

import "time"

type Task struct {
	BaseEntity `yaml:",inline" json:",inline"`
	Title      string `yaml:"title" json:"title"`
	Icon       string `yaml:"icon,omitempty" json:"icon,omitempty"`
	Location   string `yaml:"location,omitempty" json:"location,omitempty"`

	// Template / occurrence fields
	ParentID       string `yaml:"parent_id,omitempty" json:"parent_id,omitempty"`
	IsTemplate     bool   `yaml:"is_template,omitempty" json:"is_template,omitempty"`
	OccurrenceDate string `yaml:"occurrence_date,omitempty" json:"occurrence_date,omitempty"`

	// Date mode: "due_date" (single) or "range" (start_date + end_date)
	// "range" is required when recurrence is set
	DateMode  string     `yaml:"date_mode,omitempty" json:"date_mode,omitempty"`
	DueDate   *time.Time `yaml:"due_date,omitempty" json:"due_date,omitempty"`
	StartDate *time.Time `yaml:"start_date,omitempty" json:"start_date,omitempty"`
	EndDate   *time.Time `yaml:"end_date,omitempty" json:"end_date,omitempty"`

	// Time mode: "due_time", "start_end", "start_duration"
	TimeMode        string `yaml:"time_mode,omitempty" json:"time_mode,omitempty"`
	StartTime       string `yaml:"start_time,omitempty" json:"start_time,omitempty"`
	EndTime         string `yaml:"end_time,omitempty" json:"end_time,omitempty"`
	DurationMinutes int    `yaml:"duration_minutes,omitempty" json:"duration_minutes,omitempty"`
	DueTime         string `yaml:"due_time,omitempty" json:"due_time,omitempty"`

	Recurrence *Recurrence `yaml:"recurrence,omitempty" json:"recurrence,omitempty"`
	Subtasks   []Subtask   `yaml:"subtasks,omitempty" json:"subtasks,omitempty"`
	Body       string      `yaml:"-" json:"body"`
}

func NewTask(id, title string) *Task {
	now := time.Now().UTC()
	return &Task{
		BaseEntity: BaseEntity{
			ID:        id,
			Type:      TypeTask,
			Status:    StatusPending,
			Tags:      []string{},
			Links:     []string{},
			CreatedAt: now,
			UpdatedAt: now,
		},
		Title:          title,
		Subtasks:       []Subtask{},
		Body:           "",
		IsTemplate:     false,
		OccurrenceDate: "",
	}
}

// OccurrenceID generates a deterministic ID for an occurrence of a template
// on a given date. Format: "<parent-id>_<date>".
func OccurrenceID(parentID, date string) string {
	return parentID + "_" + date
}

const (
	DateModeDueDate = "due_date"
	DateModeRange   = "range"
)

const (
	TimeModeDueTime       = "due_time"
	TimeModeStartEnd      = "start_end"
	TimeModeStartDuration = "start_duration"
)

func (t *Task) Validate() error {
	if err := ValidateBase(&t.BaseEntity); err != nil {
		return err
	}
	if t.Title == "" {
		return NewValidationError("task.title is required")
	}
	// Validate status
	switch t.Status {
	case StatusPending, StatusInProgress, StatusCompleted, StatusExpired:
		// valid
	case "":
		return NewValidationError("task.status is required")
	default:
		return NewValidationError("task.status must be one of: pending, in-progress, completed, expired")
	}
	// Validate recurrence if present
	if t.Recurrence != nil {
		switch t.Recurrence.Type {
		case "daily", "weekly", "monthly", "yearly":
			// valid
		case "":
			return NewValidationError("task.recurrence.type is required when recurrence is set")
		default:
			return NewValidationError("task.recurrence.type must be one of: daily, weekly, monthly, yearly")
		}
		if t.Recurrence.Interval < 1 {
			return NewValidationError("task.recurrence.interval must be >= 1")
		}
		for _, d := range t.Recurrence.DaysOfWeek {
			if d < 0 || d > 6 {
				return NewValidationError("task.recurrence.days_of_week values must be 0-6 (Sun-Sat)")
			}
		}
	}
	return nil
}
