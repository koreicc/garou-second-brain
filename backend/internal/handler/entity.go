package handler

import (
	"net/http"
	"strings"

	"github.com/koreicc/garou-second-brain/backend/internal/model"
	"github.com/koreicc/garou-second-brain/backend/internal/vault"
	"github.com/labstack/echo/v4"
)

type EntityHandler struct {
	vault *vault.Vault
}

func NewEntityHandler(v *vault.Vault) *EntityHandler {
	return &EntityHandler{vault: v}
}

// EntityInfo is a lightweight representation of an entity for link resolution.
type EntityInfo struct {
	ID     string `json:"id"`
	Type   string `json:"type"`
	Title  string `json:"title"`
	Status string `json:"status"`
}

// GetByIDs returns basic info for multiple entities by their IDs.
// GET /api/v1/entities/by-ids?id=uuid1,uuid2,uuid3
func (h *EntityHandler) GetByIDs(c echo.Context) error {
	idsParam := c.QueryParam("ids")
	if idsParam == "" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("ids parameter is required"))
	}

	ids := strings.Split(idsParam, ",")
	results := make([]EntityInfo, 0, len(ids))

	now := timezoneNow(c)

	for _, id := range ids {
		id = strings.TrimSpace(id)
		if id == "" {
			continue
		}

		// Try note
		if note, err := h.vault.ReadNote(id); err == nil {
			results = append(results, EntityInfo{ID: note.ID, Type: "note", Title: note.Title, Status: string(note.Status)})
			continue
		}
		// Try task
		if task, err := h.vault.ReadTask(id); err == nil {
			effectiveStatus := model.ComputeEffectiveStatus(task, now)
			results = append(results, EntityInfo{ID: task.ID, Type: "task", Title: task.Title, Status: string(effectiveStatus)})
			continue
		}
		// Try person
		if person, err := h.vault.ReadPerson(id); err == nil {
			results = append(results, EntityInfo{ID: person.ID, Type: "person", Title: person.Name, Status: string(person.Status)})
			continue
		}
	}

	return c.JSON(http.StatusOK, model.DataResponse(results))
}
