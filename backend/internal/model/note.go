package model

import "time"

type Note struct {
	BaseEntity `yaml:",inline" json:",inline"`
	Title      string `yaml:"title" json:"title"`
	Body       string `yaml:"-" json:"body"`
}

func NewNote(id, title string) *Note {
	now := time.Now().UTC()
	return &Note{
		BaseEntity: BaseEntity{
			ID:        id,
			Type:      TypeNote,
			Status:    StatusActive,
			Tags:      []string{},
			Links:     []string{},
			CreatedAt: now,
			UpdatedAt: now,
		},
		Title: title,
		Body:  "",
	}
}

func (n *Note) SetBody(body string) { n.Body = body }

func (n *Note) Validate() error {
	if err := ValidateBase(&n.BaseEntity); err != nil {
		return err
	}
	if n.Title == "" {
		return NewValidationError("note.title is required")
	}
	return nil
}
