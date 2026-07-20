package handler

import (
	"fmt"
	"net/http"

	"github.com/koreicc/garou-second-brain/backend/internal/model"
	"github.com/koreicc/garou-second-brain/backend/internal/vault"
	"github.com/labstack/echo/v4"
)

type ArchiveHandler struct {
	vault *vault.Vault
}

func NewArchiveHandler(v *vault.Vault) *ArchiveHandler {
	return &ArchiveHandler{vault: v}
}

type ArchiveEntry struct {
	ID    string `json:"id"`
	Type  string `json:"type"`
	Title string `json:"title"`
}

// List returns all archived entities, optionally filtered by ?type=.
func (h *ArchiveHandler) List(c echo.Context) error {
	entityType := c.QueryParam("type")

	typesToScan := []string{model.TypeNote, model.TypeTask, model.TypeQuickTask, model.TypePerson}
	if entityType != "" {
		typesToScan = []string{entityType}
	}

	var entries []ArchiveEntry

	for _, et := range typesToScan {
		ids, err := h.vault.ListArchivedIDs(et)
		if err != nil {
			continue
		}
		for _, id := range ids {
			entry := h.readArchiveEntry(et, id)
			if entry != nil {
				entries = append(entries, *entry)
			}
		}
	}

	if entries == nil {
		entries = []ArchiveEntry{}
	}

	return c.JSON(http.StatusOK, model.DataResponse(entries))
}

// Restore restores an archived entity back to its original location.
func (h *ArchiveHandler) Restore(c echo.Context) error {
	entityType := c.Param("type")
	id := c.Param("id")

	if err := h.vault.RestoreArchive(entityType, id); err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("archived entity not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("restore: %v", err)))
	}

	return c.JSON(http.StatusOK, model.DataResponse(map[string]string{
		"message": "entity restored successfully",
		"id":      id,
		"type":    entityType,
	}))
}

// readArchiveEntry reads an archived entity and returns a summary entry.
// Returns nil if the entity cannot be read.
func (h *ArchiveHandler) readArchiveEntry(entityType, id string) *ArchiveEntry {
	switch entityType {
	case model.TypeNote:
		note, err := h.vault.ReadArchivedNote(id)
		if err != nil {
			return nil
		}
		return &ArchiveEntry{ID: note.ID, Type: model.TypeNote, Title: note.Title}
	case model.TypeTask:
		task, err := h.vault.ReadArchivedTask(id)
		if err != nil {
			return nil
		}
		return &ArchiveEntry{ID: task.ID, Type: model.TypeTask, Title: task.Title}
	case model.TypeQuickTask:
		qt, err := h.vault.ReadArchivedQuickTask(id)
		if err != nil {
			return nil
		}
		return &ArchiveEntry{ID: qt.ID, Type: model.TypeQuickTask, Title: qt.Title}
	case model.TypePerson:
		person, err := h.vault.ReadArchivedPerson(id)
		if err != nil {
			return nil
		}
		return &ArchiveEntry{ID: person.ID, Type: model.TypePerson, Title: person.Name}
	}
	return nil
}
