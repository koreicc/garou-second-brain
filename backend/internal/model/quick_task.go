package model

import "time"

type QuickTask struct {
	BaseEntity `yaml:",inline" json:",inline"`
	Title      string `yaml:"title" json:"title"`
}

func NewQuickTask(id, title string) *QuickTask {
	now := time.Now().UTC()
	return &QuickTask{
		BaseEntity: BaseEntity{
			ID:        id,
			Type:      TypeQuickTask,
			Status:    StatusPending,
			CreatedAt: now,
			UpdatedAt: now,
		},
		Title: title,
	}
}

func (q *QuickTask) Validate() error {
	if err := ValidateBase(&q.BaseEntity); err != nil {
		return err
	}
	if q.Title == "" {
		return NewValidationError("quick-task.title is required")
	}
	return nil
}
