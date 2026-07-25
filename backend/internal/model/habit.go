package model

import "time"

type Habit struct {
	BaseEntity      `yaml:",inline" json:",inline"`
	Title           string    `yaml:"title" json:"title"`
	Icon            string    `yaml:"icon,omitempty" json:"icon,omitempty"`
	Location        string    `yaml:"location,omitempty" json:"location,omitempty"`
	Priority        string    `yaml:"priority,omitempty" json:"priority,omitempty"`
	DaysOfWeek      []int     `yaml:"days_of_week" json:"days_of_week"` // 1=Mon..7=Sun
	TimeMode        string    `yaml:"time_mode,omitempty" json:"time_mode,omitempty"`
	StartTime       string    `yaml:"start_time,omitempty" json:"start_time,omitempty"`
	EndTime         string    `yaml:"end_time,omitempty" json:"end_time,omitempty"`
	DurationMinutes int       `yaml:"duration_minutes,omitempty" json:"duration_minutes,omitempty"`
	DueTime         string    `yaml:"due_time,omitempty" json:"due_time,omitempty"`
	Subtasks        []Subtask `yaml:"subtasks,omitempty" json:"subtasks,omitempty"`
	Body            string    `yaml:"-" json:"body"`

	// TodayCompleted is computed by the server and NOT persisted to YAML.
	TodayCompleted bool `yaml:"-" json:"today_completed,omitempty"`
}

func NewHabit(id, title string) *Habit {
	now := time.Now().UTC()
	return &Habit{
		BaseEntity: BaseEntity{
			ID:        id,
			Type:      TypeHabit,
			Status:    StatusActive,
			Tags:      []string{},
			Links:     []string{},
			CreatedAt: now,
			UpdatedAt: now,
		},
		Title:      title,
		DaysOfWeek: []int{},
		Subtasks:   []Subtask{},
		Body:       "",
	}
}

func (h *Habit) Validate() error {
	if err := ValidateBase(&h.BaseEntity); err != nil {
		return err
	}
	if h.Title == "" {
		return NewValidationError("habit.title is required")
	}
	if len(h.DaysOfWeek) == 0 {
		return NewValidationError("habit.days_of_week is required (at least 1 day)")
	}
	for _, d := range h.DaysOfWeek {
		if d < 1 || d > 7 {
			return NewValidationError("habit.days_of_week values must be 1-7 (Mon-Sun)")
		}
	}
	return nil
}
