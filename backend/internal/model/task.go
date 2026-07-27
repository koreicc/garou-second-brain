package model

import (
	"strconv"
	"strings"
	"time"
)

// Task priority levels.
const (
	PriorityNone   = ""
	PriorityLow    = "low"
	PriorityMedium = "medium"
	PriorityHigh   = "high"
	PriorityUrgent = "urgent"
)

type Task struct {
	BaseEntity `yaml:",inline" json:",inline"`
	Title      string `yaml:"title" json:"title"`
	Icon       string `yaml:"icon,omitempty" json:"icon,omitempty"`
	Location   string `yaml:"location,omitempty" json:"location,omitempty"`
	Priority   string `yaml:"priority,omitempty" json:"priority,omitempty"`

	// EffectiveStatus is computed by the server and NOT persisted to YAML.
	EffectiveStatus EntityStatus `yaml:"-" json:"effective_status,omitempty"`

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

func (t *Task) SetBody(body string) { t.Body = body }

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
	// Validate priority if set
	if t.Priority != "" {
		switch t.Priority {
		case PriorityLow, PriorityMedium, PriorityHigh, PriorityUrgent:
			// valid
		default:
			return NewValidationError("task.priority must be one of: low, medium, high, urgent")
		}
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

// startOfDay returns the start (00:00:00.000) of the given date in UTC.
func startOfDay(t time.Time) time.Time {
	return time.Date(t.Year(), t.Month(), t.Day(), 0, 0, 0, 0, time.UTC)
}

// endOfDay returns the last nanosecond of the given date in UTC.
func endOfDay(t time.Time) time.Time {
	return time.Date(t.Year(), t.Month(), t.Day(), 23, 59, 59, 999999999, time.UTC)
}

// combineDateAndTime parses a "HH:mm" string and combines it with a date to
// produce a UTC timestamp. If parsing fails, startOfDay is returned.
func combineDateAndTime(date time.Time, timeStr string) time.Time {
	parts := strings.Split(timeStr, ":")
	if len(parts) != 2 {
		return startOfDay(date)
	}
	h, errH := strconv.Atoi(parts[0])
	m, errM := strconv.Atoi(parts[1])
	if errH != nil || errM != nil {
		return startOfDay(date)
	}
	return time.Date(date.Year(), date.Month(), date.Day(), h, m, 0, 0, time.UTC)
}

// ComputeEffectiveStatus calculates the task's status based on date/time
// configuration and the current time. It respects manual overrides.
// now is the current time in the user's timezone.
func ComputeEffectiveStatus(t *Task, now time.Time) EntityStatus {
	// No date mode -> fully manual
	if t.DateMode == "" {
		return t.Status
	}

	// Manually completed or expired -> don't override
	if t.Status == StatusCompleted || t.Status == StatusExpired {
		return t.Status
	}

	switch t.DateMode {
	case DateModeDueDate:
		return computeDueDateStatus(t, now)
	case DateModeRange:
		return computeRangeStatus(t, now)
	default:
		return t.Status
	}
}

func computeDueDateStatus(t *Task, now time.Time) EntityStatus {
	if t.DueDate == nil {
		return t.Status
	}

	startBoundary := startOfDay(*t.DueDate)
	if t.DueTime != "" {
		startBoundary = combineDateAndTime(*t.DueDate, t.DueTime)
	}
	endBoundary := endOfDay(*t.DueDate)

	if now.Before(startBoundary) {
		return StatusPending
	}
	if now.After(endBoundary) {
		return StatusCompleted
	}
	return StatusInProgress
}

func computeRangeStatus(t *Task, now time.Time) EntityStatus {
	if t.StartDate == nil || t.EndDate == nil {
		return t.Status
	}

	startBoundary := startOfDay(*t.StartDate)
	if t.TimeMode == TimeModeStartEnd && t.StartTime != "" {
		startBoundary = combineDateAndTime(*t.StartDate, t.StartTime)
	} else if t.TimeMode == TimeModeStartDuration && t.StartTime != "" {
		startBoundary = combineDateAndTime(*t.StartDate, t.StartTime)
	}

	endBoundary := endOfDay(*t.EndDate)
	if t.TimeMode == TimeModeStartEnd && t.EndTime != "" {
		endBoundary = combineDateAndTime(*t.EndDate, t.EndTime)
	} else if t.TimeMode == TimeModeStartDuration && t.StartTime != "" && t.DurationMinutes > 0 {
		startParsed := combineDateAndTime(*t.StartDate, t.StartTime)
		endBoundary = startParsed.Add(time.Duration(t.DurationMinutes) * time.Minute)
	}

	if now.Before(startBoundary) {
		return StatusPending
	}
	if now.After(endBoundary) {
		return StatusCompleted
	}
	return StatusInProgress
}
