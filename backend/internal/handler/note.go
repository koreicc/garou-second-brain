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

type NoteHandler struct {
	vault *vault.Vault
}

func NewNoteHandler(v *vault.Vault) *NoteHandler {
	return &NoteHandler{vault: v}
}

func (h *NoteHandler) List(c echo.Context) error {
	notes, err := h.vault.ListNotes()
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list notes: %v", err)))
	}
	if notes == nil {
		notes = []*model.Note{}
	}
	notes = paginate(notes, c)
	return c.JSON(http.StatusOK, model.DataResponse(notes))
}

func (h *NoteHandler) Get(c echo.Context) error {
	id := c.Param("id")
	note, err := h.vault.ReadNote(id)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("note not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read note: %v", err)))
	}
	return c.JSON(http.StatusOK, model.DataResponse(note))
}

type CreateNoteRequest struct {
	Title string   `json:"title"`
	Tags  []string `json:"tags"`
	Links []string `json:"links"`
	Body  string   `json:"body"`
}

func (h *NoteHandler) Create(c echo.Context) error {
	var req CreateNoteRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}
	if req.Title == "" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("title is required"))
	}

	now := time.Now().UTC()
	note := &model.Note{
		BaseEntity: model.BaseEntity{
			ID:        uuid.New().String(),
			Type:      model.TypeNote,
			Status:    model.StatusActive,
			Tags:      req.Tags,
			Links:     req.Links,
			CreatedAt: now,
			UpdatedAt: now,
		},
		Title: req.Title,
		Body:  req.Body,
	}
	if note.Tags == nil {
		note.Tags = []string{}
	}
	if note.Links == nil {
		note.Links = []string{}
	}

	if err := h.vault.WriteNote(note); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save note: %v", err)))
	}

	return c.JSON(http.StatusCreated, model.DataResponse(note))
}

type UpdateNoteRequest struct {
	Title  string   `json:"title"`
	Status string   `json:"status"`
	Tags   []string `json:"tags"`
	Links  []string `json:"links"`
	Body   string   `json:"body"`
}

func (h *NoteHandler) Update(c echo.Context) error {
	id := c.Param("id")
	note, err := h.vault.ReadNote(id)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("note not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read note: %v", err)))
	}

	var req UpdateNoteRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}

	if req.Title != "" {
		note.Title = req.Title
	}
	if req.Status != "" {
		note.Status = model.EntityStatus(req.Status)
	}
	if req.Tags != nil {
		note.Tags = req.Tags
	}
	if req.Links != nil {
		note.Links = req.Links
	}
	if req.Body != "" {
		note.Body = req.Body
	}
	note.UpdatedAt = time.Now().UTC()

	if err := h.vault.WriteNote(note); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save note: %v", err)))
	}

	return c.JSON(http.StatusOK, model.DataResponse(note))
}

func (h *NoteHandler) Delete(c echo.Context) error {
	id := c.Param("id")
	if err := h.vault.Delete(model.TypeNote, id); err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("note not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("delete note: %v", err)))
	}
	return c.JSON(http.StatusOK, model.DataResponse(nil))
}
