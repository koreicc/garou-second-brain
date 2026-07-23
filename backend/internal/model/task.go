package model

import "time"

type Task struct {
	BaseEntity `yaml:",inline" json:",inline"`
	Title      string      `yaml:"title" json:"title"`
	Icon       string      `yaml:"icon,omitempty" json:"icon,omitempty"`
	Location   string      `yaml:"location,omitempty" json:"location,omitempty"`

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
		Title:    title,
		Subtasks: []Subtask{},
		Body:     "",
	}
}

const (
	DateModeDueDate = "due_date"
	DateModeRange   = "range"
)

const (
	TimeModeDueTime      = "due_time"
	TimeModeStartEnd     = "start_end"
	TimeModeStartDuration = "start_duration"
)

func (t *Task) Validate() error {
	if err := ValidateBase(&t.BaseEntity); err != nil {
		return err
	}
	if t.Title == "" {
		return NewValidationError("task.title is required")
	}
	return nil
}
