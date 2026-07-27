package model

import "time"

type Person struct {
	BaseEntity  `yaml:",inline" json:",inline"`
	Name        string       `yaml:"name" json:"name"`
	Contacts    []Contact    `yaml:"contacts,omitempty" json:"contacts,omitempty"`
	SocialLinks []SocialLink `yaml:"social_links,omitempty" json:"social_links,omitempty"`
	Notes       string       `yaml:"notes,omitempty" json:"notes,omitempty"`
	Body        string       `yaml:"-" json:"body"`
}

func (p *Person) SetBody(body string) { p.Body = body }

func NewPerson(id, name string) *Person {
	now := time.Now().UTC()
	return &Person{
		BaseEntity: BaseEntity{
			ID:        id,
			Type:      TypePerson,
			Status:    StatusActive,
			Tags:      []string{},
			Links:     []string{},
			CreatedAt: now,
			UpdatedAt: now,
		},
		Name:        name,
		Contacts:    []Contact{},
		SocialLinks: []SocialLink{},
		Notes:       "",
		Body:        "",
	}
}

func (p *Person) Validate() error {
	if err := ValidateBase(&p.BaseEntity); err != nil {
		return err
	}
	if p.Name == "" {
		return NewValidationError("person.name is required")
	}
	return nil
}
