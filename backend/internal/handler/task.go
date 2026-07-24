package handler

import (
	"fmt"
	"net/http"
	"strconv"
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
func enrichTasks(c echo.Context, tasks ...*model.Task) {
	now := time.Now().UTC()
	tzOffsetStr := c.Request().Header.Get("X-Timezone-Offset")
	if tzOffsetStr != "" {
		offset, err := strconv.Atoi(tzOffsetStr)
		if err == nil {
			now = now.Add(time.Duration(offset) * time.Minute)
		}
	}
	for _, t := range tasks {
		if t != nil {
			t.EffectiveStatus = model.ComputeEffectiveStatus(t, now)
		}
	}
}

// getNowForRequest returns the current time adjusted by the timezone offset header.
func getNowForRequest(c echo.Context) time.Time {
	now := time.Now().UTC()
	tzOffsetStr := c.Request().Header.Get("X-Timezone-Offset")
	if tzOffsetStr != "" {
		offset, err := strconv.Atoi(tzOffsetStr)
		if err == nil {
			now = now.Add(time.Duration(offset) * time.Minute)
		}
	}
	return now
}

// List returns all tasks with optional filtering and sorting.
// Query params:
//
//	status=        filter by status (pending, in-progress, completed, expired)
//	priority=      filter by priority (low, medium, high, urgent)
//	search=        filter by title/body text (case-insensitive substring match)
//	sort_by=       field to sort by (title, created_at, updated_at, due_date, priority) default: created_at
//	sort_order=    asc or desc (default: desc)
func (h *TaskHandler) List(c echo.Context) error {
	tasks, err := h.vault.ListTasks()
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list tasks: %v", err)))
	}
	if tasks == nil {
		tasks = []*model.Task{}
	}

	// Apply filters
	statusFilter := c.QueryParam("status")
	priorityFilter := c.QueryParam("priority")
	searchQuery := c.QueryParam("search")

	filtered := make([]*model.Task, 0, len(tasks))
	for _, t := range tasks {
		if statusFilter != "" && string(t.Status) != statusFilter {
			continue
		}
		if priorityFilter != "" && t.Priority != priorityFilter {
			continue
		}
		if searchQuery != "" {
			q := strings.ToLower(searchQuery)
			if !strings.Contains(strings.ToLower(t.Title), q) &&
				!strings.Contains(strings.ToLower(t.Body), q) {
				continue
			}
		}
		filtered = append(filtered, t)
	}

	// Apply sorting
	sortBy := c.QueryParam("sort_by")
	sortOrder := c.QueryParam("sort_order")
	if sortOrder == "" {
		sortOrder = "desc"
	}

	switch sortBy {
	case "title":
		sortTasks(filtered, func(t *model.Task) string { return t.Title }, sortOrder == "asc")
	case "priority":
		sortTasks(filtered, func(t *model.Task) string { return t.Priority }, sortOrder == "asc")
	case "due_date":
		sortTasksByDueDate(filtered, sortOrder == "asc")
	case "updated_at":
		sortTasks(filtered, func(t *model.Task) string { return t.UpdatedAt.Format(time.RFC3339) }, sortOrder == "asc")
	default:
		// Default: sort by created_at descending
		sortTasks(filtered, func(t *model.Task) string { return t.CreatedAt.Format(time.RFC3339) }, false)
	}

	filtered = paginate(filtered, c)
	enrichTasks(c, filtered...)
	return c.JSON(http.StatusOK, model.DataResponse(filtered))
}

func sortTasks(tasks []*model.Task, key func(*model.Task) string, ascending bool) {
	for i := 0; i < len(tasks); i++ {
		for j := i + 1; j < len(tasks); j++ {
			less := key(tasks[i]) < key(tasks[j])
			if ascending != less {
				tasks[i], tasks[j] = tasks[j], tasks[i]
			}
		}
	}
}

func sortTasksByDueDate(tasks []*model.Task, ascending bool) {
	for i := 0; i < len(tasks); i++ {
		for j := i + 1; j < len(tasks); j++ {
			ti := tasks[i].DueDate
			tj := tasks[j].DueDate
			var less bool
			if ti == nil && tj == nil {
				less = false
			} else if ti == nil {
				less = !ascending
			} else if tj == nil {
				less = ascending
			} else {
				less = ti.Before(*tj)
			}
			if ascending != less {
				tasks[i], tasks[j] = tasks[j], tasks[i]
			}
		}
	}
}

// BatchRequest is used for batch operations on tasks.
type BatchRequest struct {
	IDs    []string `json:"ids"`
	Action string   `json:"action"` // "delete" or "complete"
}

// Batch handles batch operations on tasks (delete, complete).
func (h *TaskHandler) Batch(c echo.Context) error {
	var req BatchRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}
	if len(req.IDs) == 0 {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("ids array is required"))
	}
	if req.Action != "delete" && req.Action != "complete" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("action must be 'delete' or 'complete'"))
	}

	now := time.Now().UTC()
	var errors []string

	for _, id := range req.IDs {
		switch req.Action {
		case "delete":
			if err := h.vault.Delete(model.TypeTask, id); err != nil {
				errors = append(errors, fmt.Sprintf("delete %s: %v", id, err))
			}
		case "complete":
			task, err := h.vault.ReadTask(id)
			if err != nil {
				errors = append(errors, fmt.Sprintf("read %s: %v", id, err))
				continue
			}
			task.Status = model.StatusCompleted
			task.UpdatedAt = now
			if err := h.vault.WriteTask(task); err != nil {
				errors = append(errors, fmt.Sprintf("complete %s: %v", id, err))
			}
		}
	}

	if errors != nil {
		return c.JSON(http.StatusOK, model.DataResponse(map[string]interface{}{
			"success": len(req.IDs) - len(errors),
			"errors":  errors,
		}))
	}
	return c.JSON(http.StatusOK, model.DataResponse(map[string]interface{}{
		"success": len(req.IDs),
	}))
}

// Upcoming returns tasks due within the next N days.
// Query param: days=7 (default 7)
func (h *TaskHandler) Upcoming(c echo.Context) error {
	daysStr := c.QueryParam("days")
	days := 7
	if daysStr != "" {
		if d, err := strconv.Atoi(daysStr); err == nil && d > 0 && d <= 365 {
			days = d
		}
	}

	now := getNowForRequest(c)
	cutoff := now.AddDate(0, 0, days)

	tasks, err := h.vault.ListTasks()
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list tasks: %v", err)))
	}
	if tasks == nil {
		tasks = []*model.Task{}
	}

	upcoming := make([]*model.Task, 0)
	for _, t := range tasks {
		if t.Status == model.StatusCompleted || t.Status == model.StatusExpired {
			continue
		}
		status := model.ComputeEffectiveStatus(t, now)
		if status == model.StatusCompleted || status == model.StatusExpired {
			continue
		}
		if t.DateMode == "" {
			continue
		}
		// Check if due date or start date falls within the window
		var taskDate time.Time
		if t.DateMode == "due_date" && t.DueDate != nil {
			taskDate = *t.DueDate
		} else if t.DateMode == "range" && t.StartDate != nil {
			taskDate = *t.StartDate
		} else {
			continue
		}
		if !taskDate.Before(now) && !taskDate.After(cutoff) {
			upcoming = append(upcoming, t)
		}
	}

	// Sort by due/start date ascending
	sortTasksByDueDate(upcoming, true)
	enrichTasks(c, upcoming...)
	return c.JSON(http.StatusOK, model.DataResponse(upcoming))
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
	enrichTasks(c, tasks...)
	return c.JSON(http.StatusOK, model.DataResponse(tasks))
}

// ListByDate returns tasks (occurrences) for a given date.
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
	enrichTasks(c, tasks...)
	return c.JSON(http.StatusOK, model.DataResponse(tasks))
}

// Get returns a task by ID.
func (h *TaskHandler) Get(c echo.Context) error {
	id := c.Param("id")
	task, err := h.vault.ReadTask(id)
	if err != nil {
		if isNotFound(err) {
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
								if override, _ := h.vault.ReadOccurrenceOverride(parentID, date); override != nil {
									vault.ApplyOccurrenceOverride(occ, override)
								}
								enrichTasks(c, occ)
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
	enrichTasks(c, task)
	return c.JSON(http.StatusOK, model.DataResponse(task))
}

type CreateTaskRequest struct {
	Title           string            `json:"title"`
	Status          string            `json:"status"`
	Icon            string            `json:"icon"`
	Location        string            `json:"location"`
	Priority        string            `json:"priority"`
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
		Priority:        req.Priority,
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

	if err := h.vault.WriteTask(task); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save task: %v", err)))
	}

	enrichTasks(c, task)
	return c.JSON(http.StatusCreated, model.DataResponse(task))
}

type UpdateTaskRequest struct {
	Title           string            `json:"title"`
	Status          string            `json:"status"`
	Icon            string            `json:"icon"`
	Location        string            `json:"location"`
	Priority        string            `json:"priority"`
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
	if req.Priority != "" {
		task.Priority = req.Priority
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

	enrichTasks(c, task)
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

func (h *TaskHandler) UpdateOccurrence(c echo.Context) error {
	parentID := c.Param("parentId")
	date := c.Param("date")

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

	if req.Status != "" {
		switch req.Status {
		case model.TaskStatusPending, model.TaskStatusInProgress, model.TaskStatusCompleted:
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

	occ := vault.ComputeDynamicOccurrence(parent, date)
	if occ == nil {
		return c.JSON(http.StatusNotFound, model.ErrorResponse("occurrence not found for this date"))
	}
	vault.ApplyOccurrenceOverride(occ, override)

	enrichTasks(c, occ)
	return c.JSON(http.StatusOK, model.DataResponse(occ))
}
