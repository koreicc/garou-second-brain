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

type TaskHandler struct {
	vault *vault.Vault
}

func NewTaskHandler(v *vault.Vault) *TaskHandler {
	return &TaskHandler{vault: v}
}

func (h *TaskHandler) List(c echo.Context) error {
	tasks, err := h.vault.ListTasks()
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list tasks: %v", err)))
	}
	if tasks == nil {
		tasks = []*model.Task{}
	}
	return c.JSON(http.StatusOK, model.DataResponse(tasks))
}

func (h *TaskHandler) Get(c echo.Context) error {
	id := c.Param("id")
	task, err := h.vault.ReadTask(id)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("task not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read task: %v", err)))
	}
	return c.JSON(http.StatusOK, model.DataResponse(task))
}

type CreateTaskRequest struct {
	Title      string            `json:"title"`
	Icon       string            `json:"icon"`
	Location   string            `json:"location"`
	Tags       []string          `json:"tags"`
	Links      []string          `json:"links"`
	StartDate  *time.Time        `json:"start_date"`
	EndDate    *time.Time        `json:"end_date"`
	Recurrence *model.Recurrence `json:"recurrence"`
	Subtasks   []model.Subtask   `json:"subtasks"`
	Body       string            `json:"body"`
}

func (h *TaskHandler) Create(c echo.Context) error {
	var req CreateTaskRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}
	if req.Title == "" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("title is required"))
	}

	now := time.Now().UTC()
	task := &model.Task{
		BaseEntity: model.BaseEntity{
			ID:        uuid.New().String(),
			Type:      model.TypeTask,
			Status:    model.TaskStatusPending,
			Tags:      req.Tags,
			Links:     req.Links,
			CreatedAt: now,
			UpdatedAt: now,
		},
		Title:      req.Title,
		Icon:       req.Icon,
		Location:   req.Location,
		StartDate:  req.StartDate,
		EndDate:    req.EndDate,
		Recurrence: req.Recurrence,
		Subtasks:   req.Subtasks,
		Body:       req.Body,
	}
	if task.Tags == nil {
		task.Tags = []string{}
	}
	if task.Links == nil {
		task.Links = []string{}
	}
	if task.Subtasks == nil {
		task.Subtasks = []model.Subtask{}
	}

	if err := h.vault.WriteTask(task); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save task: %v", err)))
	}

	return c.JSON(http.StatusCreated, model.DataResponse(task))
}

type UpdateTaskRequest struct {
	Title      string            `json:"title"`
	Status     string            `json:"status"`
	Icon       string            `json:"icon"`
	Location   string            `json:"location"`
	Tags       []string          `json:"tags"`
	Links      []string          `json:"links"`
	StartDate  *time.Time        `json:"start_date"`
	EndDate    *time.Time        `json:"end_date"`
	Recurrence *model.Recurrence `json:"recurrence"`
	Subtasks   []model.Subtask   `json:"subtasks"`
	Body       string            `json:"body"`
}

func (h *TaskHandler) Update(c echo.Context) error {
	id := c.Param("id")
	task, err := h.vault.ReadTask(id)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("task not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read task: %v", err)))
	}

	var req UpdateTaskRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}

	if req.Title != "" {
		task.Title = req.Title
	}
	if req.Status != "" {
		task.Status = model.EntityStatus(req.Status)
	}
	if req.Icon != "" {
		task.Icon = req.Icon
	}
	if req.Location != "" {
		task.Location = req.Location
	}
	if req.Tags != nil {
		task.Tags = req.Tags
	}
	if req.Links != nil {
		task.Links = req.Links
	}
	if req.StartDate != nil {
		task.StartDate = req.StartDate
	}
	if req.EndDate != nil {
		task.EndDate = req.EndDate
	}
	if req.Recurrence != nil || c.Request().Method == http.MethodPut {
		task.Recurrence = req.Recurrence
	}
	if req.Subtasks != nil {
		task.Subtasks = req.Subtasks
	}
	if req.Body != "" || c.Request().Method == http.MethodPut {
		task.Body = req.Body
	}
	task.UpdatedAt = time.Now().UTC()

	var spawnedTask *model.Task
	if req.Status == model.TaskStatusCompleted && task.Recurrence != nil {
		spawnedTask = h.spawnRecurringTask(task)
	}

	if err := h.vault.WriteTask(task); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save task: %v", err)))
	}

	result := map[string]interface{}{
		"task": task,
	}
	if spawnedTask != nil {
		result["spawned_task"] = spawnedTask
	}

	return c.JSON(http.StatusOK, model.DataResponse(result))
}

func (h *TaskHandler) spawnRecurringTask(oldTask *model.Task) *model.Task {
	now := time.Now().UTC()

	newTask := &model.Task{
		BaseEntity: model.BaseEntity{
			ID:        uuid.New().String(),
			Type:      model.TypeTask,
			Status:    model.TaskStatusPending,
			Tags:      copySlice(oldTask.Tags),
			Links:     copySlice(oldTask.Links),
			CreatedAt: now,
			UpdatedAt: now,
		},
		Title:      oldTask.Title,
		Icon:       oldTask.Icon,
		Location:   oldTask.Location,
		Recurrence: oldTask.Recurrence,
		Subtasks:   resetSubtasks(oldTask.Subtasks),
		Body:       oldTask.Body,
	}

	if oldTask.EndDate != nil {
		newEnd := calculateNextDate(*oldTask.EndDate, oldTask.Recurrence)
		newTask.EndDate = &newEnd

		if oldTask.StartDate != nil {
			diff := oldTask.EndDate.Sub(*oldTask.StartDate)
			newStart := newEnd.Add(-diff)
			newTask.StartDate = &newStart
		}
	}

	if err := h.vault.WriteTask(newTask); err != nil {
		return nil
	}

	oldTask.Status = model.TaskStatusExpired

	return newTask
}

func resetSubtasks(subtasks []model.Subtask) []model.Subtask {
	result := make([]model.Subtask, len(subtasks))
	for i, s := range subtasks {
		result[i] = model.Subtask{
			ID:        uuid.New().String(),
			Title:     s.Title,
			Completed: false,
		}
	}
	return result
}

func calculateNextDate(from time.Time, rec *model.Recurrence) time.Time {
	switch rec.Type {
	case "daily":
		return from.AddDate(0, 0, rec.Interval)
	case "weekly":
		return from.AddDate(0, 0, 7*rec.Interval)
	case "monthly":
		return from.AddDate(0, rec.Interval, 0)
	case "yearly":
		return from.AddDate(rec.Interval, 0, 0)
	default:
		return from.AddDate(0, 0, 1)
	}
}

func (h *TaskHandler) Delete(c echo.Context) error {
	id := c.Param("id")
	if err := h.vault.Delete(model.TypeTask, id); err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("task not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("delete task: %v", err)))
	}
	return c.JSON(http.StatusOK, model.DataResponse(nil))
}

func copySlice(s []string) []string {
	if s == nil {
		return nil
	}
	result := make([]string, len(s))
	copy(result, s)
	return result
}
