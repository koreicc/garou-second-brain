package model

import "time"

type Task struct {
	BaseEntity `yaml:",inline" json:",inline"`
	Title      string      `yaml:"title" json:"title"`
	Icon       string      `yaml:"icon,omitempty" json:"icon,omitempty"`
	Location   string      `yaml:"location,omitempty" json:"location,omitempty"`
	StartDate  *time.Time  `yaml:"start_date,omitempty" json:"start_date,omitempty"`
	EndDate    *time.Time  `yaml:"end_date,omitempty" json:"end_date,omitempty"`
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

func (t *Task) Validate() error {
	if err := ValidateBase(&t.BaseEntity); err != nil {
		return err
	}
	if t.Title == "" {
		return NewValidationError("task.title is required")
	}
	return nil
}
