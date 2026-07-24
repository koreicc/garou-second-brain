package handler

import (
	"fmt"
	"net/http"
	"strings"
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

// enrichTasks sets the EffectiveStatus field on one or more tasks.
func enrichTasks(tasks ...*model.Task) {
	now := time.Now().UTC()
	for _, t := range tasks {
		if t != nil {
			t.EffectiveStatus = model.ComputeEffectiveStatus(t, now)
		}
	}
}

// List returns all tasks (templates + occurrences).
func (h *TaskHandler) List(c echo.Context) error {
	tasks, err := h.vault.ListTasks()
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list tasks: %v", err)))
	}
	if tasks == nil {
		tasks = []*model.Task{}
	}
	tasks = paginate(tasks, c)
	enrichTasks(tasks...)
	return c.JSON(http.StatusOK, model.DataResponse(tasks))
}

// ListTemplates returns only template tasks.
func (h *TaskHandler) ListTemplates(c echo.Context) error {
	tasks, err := h.vault.ListTemplates()
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list templates: %v", err)))
	}
	if tasks == nil {
		tasks = []*model.Task{}
	}
	enrichTasks(tasks...)
	return c.JSON(http.StatusOK, model.DataResponse(tasks))
}

// ListByDate returns tasks (occurrences) for a given date.
// Query param: date=YYYY-MM-DD
func (h *TaskHandler) ListByDate(c echo.Context) error {
	date := c.QueryParam("date")
	if date == "" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("date query parameter is required (YYYY-MM-DD)"))
	}
	tasks, err := h.vault.ListTasksByDate(date)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list tasks by date: %v", err)))
	}
	if tasks == nil {
		tasks = []*model.Task{}
	}
	enrichTasks(tasks...)
	return c.JSON(http.StatusOK, model.DataResponse(tasks))
}

// Get returns a task by ID. If the task is not found on disk, the handler
// checks whether the ID matches a dynamic occurrence pattern (<parent-id>_<date>)
// and computes the occurrence on-the-fly from the parent template.
func (h *TaskHandler) Get(c echo.Context) error {
	id := c.Param("id")
	task, err := h.vault.ReadTask(id)
	if err != nil {
		if isNotFound(err) {
			// Check if this is a dynamic occurrence ID: <parentID>_<YYYY-MM-DD>
			underscoreIdx := strings.LastIndex(id, "_")
			if underscoreIdx > 0 && underscoreIdx < len(id)-1 {
				parentID := id[:underscoreIdx]
				date := id[underscoreIdx+1:]
				if _, parseErr := time.Parse("2006-01-02", date); parseErr == nil {
					parent, parentErr := h.vault.ReadTask(parentID)
					if parentErr == nil && parent.IsTemplate && parent.Recurrence != nil {
						parsedDate, _ := time.Parse("2006-01-02", date)
						if vault.DateMatchesRecurrence(parsedDate, parent) {
							occ := vault.ComputeDynamicOccurrence(parent, date)
							if occ != nil {
								// Apply any per-occurrence overrides
								if override, _ := h.vault.ReadOccurrenceOverride(parentID, date); override != nil {
									vault.ApplyOccurrenceOverride(occ, override)
								}
								enrichTasks(occ)
								return c.JSON(http.StatusOK, model.DataResponse(occ))
							}
						}
					}
				}
			}
			return c.JSON(http.StatusNotFound, model.ErrorResponse("task not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read task: %v", err)))
	}
	enrichTasks(task)
	return c.JSON(http.StatusOK, model.DataResponse(task))
}

type CreateTaskRequest struct {
	Title           string            `json:"title"`
	Status          string            `json:"status"`
	Icon            string            `json:"icon"`
	Location        string            `json:"location"`
	Tags            []string          `json:"tags"`
	Links           []string          `json:"links"`
	ParentID        string            `json:"parent_id"`
	IsTemplate      bool              `json:"is_template"`
	OccurrenceDate  string            `json:"occurrence_date"`
	DateMode        string            `json:"date_mode"`
	DueDate         *time.Time        `json:"due_date"`
	StartDate       *time.Time        `json:"start_date"`
	EndDate         *time.Time        `json:"end_date"`
	TimeMode        string            `json:"time_mode"`
	StartTime       string            `json:"start_time"`
	EndTime         string            `json:"end_time"`
	DurationMinutes int               `json:"duration_minutes"`
	DueTime         string            `json:"due_time"`
	Recurrence      *model.Recurrence `json:"recurrence"`
	Subtasks        []model.Subtask   `json:"subtasks"`
	Body            string            `json:"body"`
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
	taskStatus := model.TaskStatusPending
	if req.Status != "" {
		switch req.Status {
		case model.TaskStatusPending, model.TaskStatusInProgress, model.TaskStatusCompleted:
			taskStatus = req.Status
		}
	}

	id := uuid.New().String()

	// Determine if this is a template: is_template=true OR has both recurrence and date range
	isTemplate := req.IsTemplate
	if !isTemplate && req.Recurrence != nil && req.DateMode == "range" && req.StartDate != nil && req.EndDate != nil {
		isTemplate = true
	}

	task := &model.Task{
		BaseEntity: model.BaseEntity{
			ID:        id,
			Type:      model.TypeTask,
			Status:    model.EntityStatus(taskStatus),
			Tags:      req.Tags,
			Links:     req.Links,
			CreatedAt: now,
			UpdatedAt: now,
		},
		Title:           req.Title,
		Icon:            req.Icon,
		Location:        req.Location,
		ParentID:        req.ParentID,
		IsTemplate:      isTemplate,
		OccurrenceDate:  req.OccurrenceDate,
		DateMode:        req.DateMode,
		DueDate:         req.DueDate,
		StartDate:       req.StartDate,
		EndDate:         req.EndDate,
		TimeMode:        req.TimeMode,
		StartTime:       req.StartTime,
		EndTime:         req.EndTime,
		DurationMinutes: req.DurationMinutes,
		DueTime:         req.DueTime,
		Recurrence:      req.Recurrence,
		Subtasks:        req.Subtasks,
		Body:            req.Body,
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

	// Save the task (template or standalone)
	if err := h.vault.WriteTask(task); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save task: %v", err)))
	}

	enrichTasks(task)
	return c.JSON(http.StatusCreated, model.DataResponse(task))
}

type UpdateTaskRequest struct {
	Title           string            `json:"title"`
	Status          string            `json:"status"`
	Icon            string            `json:"icon"`
	Location        string            `json:"location"`
	Tags            []string          `json:"tags"`
	Links           []string          `json:"links"`
	DateMode        string            `json:"date_mode"`
	DueDate         *time.Time        `json:"due_date"`
	StartDate       *time.Time        `json:"start_date"`
	EndDate         *time.Time        `json:"end_date"`
	TimeMode        string            `json:"time_mode"`
	StartTime       string            `json:"start_time"`
	EndTime         string            `json:"end_time"`
	DurationMinutes int               `json:"duration_minutes"`
	DueTime         string            `json:"due_time"`
	Recurrence      *model.Recurrence `json:"recurrence"`
	Subtasks        []model.Subtask   `json:"subtasks"`
	Body            string            `json:"body"`
}

// UpdateOccurrenceRequest is for updating a single dynamic occurrence.
// Only mutable fields are allowed: status, title, body, subtasks.
type UpdateOccurrenceRequest struct {
	Status   string          `json:"status"`
	Title    string          `json:"title"`
	Body     string          `json:"body"`
	Subtasks []model.Subtask `json:"subtasks"`
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
		switch req.Status {
		case model.TaskStatusPending, model.TaskStatusInProgress, model.TaskStatusCompleted, model.TaskStatusExpired:
			task.Status = model.EntityStatus(req.Status)
		default:
			return c.JSON(http.StatusBadRequest, model.ErrorResponse("status must be one of: pending, in-progress, completed, expired"))
		}
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
	if req.DateMode != "" {
		task.DateMode = req.DateMode
	}
	if req.DueDate != nil {
		task.DueDate = req.DueDate
	}
	if req.StartDate != nil {
		task.StartDate = req.StartDate
	}
	if req.EndDate != nil {
		task.EndDate = req.EndDate
	}
	if req.TimeMode != "" {
		task.TimeMode = req.TimeMode
	}
	if req.StartTime != "" {
		task.StartTime = req.StartTime
	}
	if req.EndTime != "" {
		task.EndTime = req.EndTime
	}
	if req.DurationMinutes != 0 {
		task.DurationMinutes = req.DurationMinutes
	}
	if req.DueTime != "" {
		task.DueTime = req.DueTime
	}
	if req.Recurrence != nil {
		task.Recurrence = req.Recurrence
	}
	if req.Subtasks != nil {
		task.Subtasks = req.Subtasks
	}
	if req.Body != "" {
		task.Body = req.Body
	}
	task.UpdatedAt = time.Now().UTC()

	if err := h.vault.WriteTask(task); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save task: %v", err)))
	}

	enrichTasks(task)
	return c.JSON(http.StatusOK, model.DataResponse(task))
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

// UpdateOccurrence updates a single dynamic occurrence (not the template).
// Only status, title, body, and subtasks can be overridden.
func (h *TaskHandler) UpdateOccurrence(c echo.Context) error {
	parentID := c.Param("parentId")
	date := c.Param("date")

	// Verify parent template exists
	parent, err := h.vault.ReadTask(parentID)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("template not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read template: %v", err)))
	}
	if !parent.IsTemplate {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("parent is not a template"))
	}

	var req UpdateOccurrenceRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}

	// Validate status if provided
	if req.Status != "" {
		switch req.Status {
		case model.TaskStatusPending, model.TaskStatusInProgress, model.TaskStatusCompleted:
			// valid
		default:
			return c.JSON(http.StatusBadRequest, model.ErrorResponse("status must be one of: pending, in-progress, completed"))
		}
	}

	override := &vault.OccurrenceOverride{
		Status:   req.Status,
		Title:    req.Title,
		Body:     req.Body,
		Subtasks: req.Subtasks,
	}

	if err := h.vault.WriteOccurrenceOverride(parentID, date, override); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save override: %v", err)))
	}

	// Return the updated occurrence
	occ := vault.ComputeDynamicOccurrence(parent, date)
	if occ == nil {
		return c.JSON(http.StatusNotFound, model.ErrorResponse("occurrence not found for this date"))
	}
	vault.ApplyOccurrenceOverride(occ, override)

	enrichTasks(occ)
	return c.JSON(http.StatusOK, model.DataResponse(occ))
}
