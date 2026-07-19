package handler

import (
	"fmt"
	"net/http"
	"time"

	"github.com/google/uuid"
	"github.com/koreicc/garou-second-brain/backend/internal/model"
	"github.com/koreicc/garou-second-brain/backend/internal/vault"
	"github.com/labstack/echo/v4"
)

type PersonHandler struct {
	vault *vault.Vault
}

func NewPersonHandler(v *vault.Vault) *PersonHandler {
	return &PersonHandler{vault: v}
}

func (h *PersonHandler) List(c echo.Context) error {
	people, err := h.vault.ListPeople()
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list people: %v", err)))
	}
	if people == nil {
		people = []*model.Person{}
	}
	return c.JSON(http.StatusOK, model.DataResponse(people))
}

func (h *PersonHandler) Get(c echo.Context) error {
	id := c.Param("id")
	person, err := h.vault.ReadPerson(id)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("person not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read person: %v", err)))
	}
	return c.JSON(http.StatusOK, model.DataResponse(person))
}

type CreatePersonRequest struct {
	Name        string             `json:"name"`
	Contacts    []model.Contact    `json:"contacts"`
	SocialLinks []model.SocialLink `json:"social_links"`
	Tags        []string           `json:"tags"`
	Links       []string           `json:"links"`
	Notes       string             `json:"notes"`
	Body        string             `json:"body"`
}

func (h *PersonHandler) Create(c echo.Context) error {
	var req CreatePersonRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}
	if req.Name == "" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("name is required"))
	}

	now := time.Now().UTC()
	person := &model.Person{
		BaseEntity: model.BaseEntity{
			ID:        uuid.New().String(),
			Type:      model.TypePerson,
			Status:    model.StatusActive,
			Tags:      req.Tags,
			Links:     req.Links,
			CreatedAt: now,
			UpdatedAt: now,
		},
		Name:        req.Name,
		Contacts:    req.Contacts,
		SocialLinks: req.SocialLinks,
		Notes:       req.Notes,
		Body:        req.Body,
	}
	if person.Tags == nil {
		person.Tags = []string{}
	}
	if person.Links == nil {
		person.Links = []string{}
	}
	if person.Contacts == nil {
		person.Contacts = []model.Contact{}
	}
	if person.SocialLinks == nil {
		person.SocialLinks = []model.SocialLink{}
	}

	if err := h.vault.WritePerson(person); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save person: %v", err)))
	}

	return c.JSON(http.StatusCreated, model.DataResponse(person))
}

type UpdatePersonRequest struct {
	Name        string             `json:"name"`
	Status      string             `json:"status"`
	Contacts    []model.Contact    `json:"contacts"`
	SocialLinks []model.SocialLink `json:"social_links"`
	Tags        []string           `json:"tags"`
	Links       []string           `json:"links"`
	Notes       string             `json:"notes"`
	Body        string             `json:"body"`
}

func (h *PersonHandler) Update(c echo.Context) error {
	id := c.Param("id")
	person, err := h.vault.ReadPerson(id)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("person not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read person: %v", err)))
	}

	var req UpdatePersonRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}

	if req.Name != "" {
		person.Name = req.Name
	}
	if req.Status != "" {
		person.Status = model.EntityStatus(req.Status)
	}
	if req.Contacts != nil {
		person.Contacts = req.Contacts
	}
	if req.SocialLinks != nil {
		person.SocialLinks = req.SocialLinks
	}
	if req.Tags != nil {
		person.Tags = req.Tags
	}
	if req.Links != nil {
		person.Links = req.Links
	}
	if req.Notes != "" {
		person.Notes = req.Notes
	}
	if req.Body != "" {
		person.Body = req.Body
	}
	person.UpdatedAt = time.Now().UTC()

	if err := h.vault.WritePerson(person); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save person: %v", err)))
	}

	return c.JSON(http.StatusOK, model.DataResponse(person))
}

func (h *PersonHandler) Delete(c echo.Context) error {
	id := c.Param("id")
	if err := h.vault.Delete(model.TypePerson, id); err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("person not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("delete person: %v", err)))
	}
	return c.JSON(http.StatusOK, model.DataResponse(map[string]string{"status": "deleted"}))
}
