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

type QuickTaskHandler struct {
	vault *vault.Vault
}

func NewQuickTaskHandler(v *vault.Vault) *QuickTaskHandler {
	return &QuickTaskHandler{vault: v}
}

func (h *QuickTaskHandler) List(c echo.Context) error {
	qts, err := h.vault.ListQuickTasks()
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list quick tasks: %v", err)))
	}
	if qts == nil {
		qts = []*model.QuickTask{}
	}
	return c.JSON(http.StatusOK, model.DataResponse(qts))
}

type CreateQuickTaskRequest struct {
	Title string `json:"title"`
}

func (h *QuickTaskHandler) Create(c echo.Context) error {
	var req CreateQuickTaskRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}
	if req.Title == "" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("title is required"))
	}

	qt := &model.QuickTask{
		BaseEntity: model.BaseEntity{
			ID:        uuid.New().String(),
			Type:      model.TypeQuickTask,
			Status:    model.QuickTaskStatusPending,
			CreatedAt: time.Now().UTC(),
			UpdatedAt: time.Now().UTC(),
		},
		Title: req.Title,
	}

	if err := h.vault.WriteQuickTask(qt); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save quick task: %v", err)))
	}

	return c.JSON(http.StatusCreated, model.DataResponse(qt))
}

func (h *QuickTaskHandler) MarkComplete(c echo.Context) error {
	id := c.Param("id")
	qt, err := h.vault.ReadQuickTask(id)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("quick task not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read quick task: %v", err)))
	}

	qt.Status = model.QuickTaskStatusCompleted
	qt.UpdatedAt = time.Now().UTC()

	if err := h.vault.WriteQuickTask(qt); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save quick task: %v", err)))
	}

	go func() {
		time.Sleep(5 * time.Second)
		_ = h.vault.Delete(model.TypeQuickTask, id)
	}()

	return c.JSON(http.StatusOK, model.DataResponse(qt))
}

func (h *QuickTaskHandler) Delete(c echo.Context) error {
	id := c.Param("id")
	if err := h.vault.Delete(model.TypeQuickTask, id); err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("quick task not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("delete quick task: %v", err)))
	}
	return c.JSON(http.StatusOK, model.DataResponse(map[string]string{"status": "deleted"}))
}
