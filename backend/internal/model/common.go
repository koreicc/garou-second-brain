package model

import "time"

type EntityStatus string

const (
	StatusActive     EntityStatus = "active"
	StatusArchived   EntityStatus = "archived"
	StatusPending    EntityStatus = "pending"
	StatusInProgress EntityStatus = "in-progress"
	StatusCompleted  EntityStatus = "completed"
	StatusExpired    EntityStatus = "expired"
)

const (
	TypeNote      = "note"
	TypeTask      = "task"
	TypeQuickTask = "quick-task"
	TypePerson    = "person"
)

const (
	TaskStatusPending    = "pending"
	TaskStatusInProgress = "in-progress"
	TaskStatusCompleted  = "completed"
	TaskStatusExpired    = "expired"
)

const (
	QuickTaskStatusPending   = "pending"
	QuickTaskStatusCompleted = "completed"
)

type Recurrence struct {
	Type       string `yaml:"type" json:"type"`
	Interval   int    `yaml:"interval" json:"interval"`
	DaysOfWeek []int  `yaml:"days_of_week,omitempty" json:"days_of_week,omitempty"`
}

type Subtask struct {
	ID        string `yaml:"id" json:"id"`
	Title     string `yaml:"title" json:"title"`
	Completed bool   `yaml:"completed" json:"completed"`
}

type Contact struct {
	Type  string `yaml:"type" json:"type"`
	Value string `yaml:"value" json:"value"`
	Label string `yaml:"label,omitempty" json:"label,omitempty"`
}

type SocialLink struct {
	Platform string `yaml:"platform" json:"platform"`
	URL      string `yaml:"url" json:"url"`
}

type BaseEntity struct {
	ID        string       `yaml:"id" json:"id"`
	Type      string       `yaml:"type" json:"type"`
	Status    EntityStatus `yaml:"status" json:"status"`
	Tags      []string     `yaml:"tags,omitempty" json:"tags,omitempty"`
	Links     []string     `yaml:"links,omitempty" json:"links,omitempty"`
	CreatedAt time.Time    `yaml:"created_at" json:"created_at"`
	UpdatedAt time.Time    `yaml:"updated_at" json:"updated_at"`
}

func ValidateBase(e *BaseEntity) error {
	if e.ID == "" {
		return NewValidationError("id is required")
	}
	if e.Type == "" {
		return NewValidationError("type is required")
	}
	if e.Status == "" {
		return NewValidationError("status is required")
	}
	if e.UpdatedAt.IsZero() {
		return NewValidationError("updated_at is required")
	}
	return nil
}
