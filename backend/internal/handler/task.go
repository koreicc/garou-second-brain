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
	return c.JSON(http.StatusOK, model.DataResponse(tasks))
}

// Get returns a task (template or occurrence) by ID.
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

	// If this is a template with date range + recurrence, generate occurrences
	var occurrences []*model.Task
	if isTemplate && req.Recurrence != nil && req.StartDate != nil && req.EndDate != nil {
		occurrences = h.generateOccurrences(task, *req.StartDate, *req.EndDate)
		for _, occ := range occurrences {
			if err := h.vault.WriteTask(occ); err != nil {
				// Best-effort: log but don't fail the whole request
				continue
			}
		}
	}

	// Return appropriate response: template+occurrences for templates, plain task otherwise
	if isTemplate {
		return c.JSON(http.StatusCreated, model.DataResponse(map[string]interface{}{
			"template":    task,
			"occurrences": occurrences,
		}))
	}
	return c.JSON(http.StatusCreated, model.DataResponse(task))
}

// generateOccurrences creates occurrence tasks for a template within the given date range.
func (h *TaskHandler) generateOccurrences(template *model.Task, start, end time.Time) []*model.Task {
	var occurrences []*model.Task
	dates := generateDatesInRange(start, end, template.Recurrence)

	for _, dateStr := range dates {
		occID := model.OccurrenceID(template.ID, dateStr)

		// Parse the date for occurrence-specific settings
		occDate, err := time.Parse("2006-01-02", dateStr)
		if err != nil {
			continue
		}

		occ := &model.Task{
			BaseEntity: model.BaseEntity{
				ID:        occID,
				Type:      model.TypeTask,
				Status:    model.StatusPending,
				Tags:      copySlice(template.Tags),
				Links:     copySlice(template.Links),
				CreatedAt: time.Now().UTC(),
				UpdatedAt: time.Now().UTC(),
			},
			Title:           template.Title,
			Icon:            template.Icon,
			Location:        template.Location,
			ParentID:        template.ID,
			IsTemplate:      false,
			OccurrenceDate:  dateStr,
			DateMode:        "due_date",
			DueDate:         &occDate,
			TimeMode:        template.TimeMode,
			StartTime:       template.StartTime,
			EndTime:         template.EndTime,
			DurationMinutes: template.DurationMinutes,
			DueTime:         template.DueTime,
			Recurrence:      nil, // occurrences have no recurrence of their own
			Subtasks:        resetSubtasks(template.Subtasks),
			Body:            template.Body,
		}
		if occ.Tags == nil {
			occ.Tags = []string{}
		}
		if occ.Links == nil {
			occ.Links = []string{}
		}
		if occ.Subtasks == nil {
			occ.Subtasks = []model.Subtask{}
		}

		occurrences = append(occurrences, occ)
	}

	return occurrences
}

// generateDatesInRange returns all dates from start to end (inclusive) matching the recurrence pattern.
func generateDatesInRange(start, end time.Time, rec *model.Recurrence) []string {
	var dates []string

	start = start.Truncate(24 * time.Hour)
	end = end.Truncate(24 * time.Hour)

	switch rec.Type {
	case "daily":
		interval := rec.Interval
		if interval < 1 {
			interval = 1
		}
		for d := start; !d.After(end); d = d.AddDate(0, 0, interval) {
			dates = append(dates, d.Format("2006-01-02"))
		}

	case "weekly":
		interval := rec.Interval
		if interval < 1 {
			interval = 1
		}
		// If specific days of week are set, use them
		if len(rec.DaysOfWeek) > 0 {
			dayMap := make(map[int]bool)
			for _, d := range rec.DaysOfWeek {
				dayMap[d] = true
			}
			for d := start; !d.After(end); d = d.AddDate(0, 0, 1) {
				wd := int(d.Weekday()) // 0=Sunday, 1=Monday, ...
				if dayMap[wd] {
					dates = append(dates, d.Format("2006-01-02"))
				}
			}
		} else {
			// Every N weeks from start
			for d := start; !d.After(end); d = d.AddDate(0, 0, 7*interval) {
				dates = append(dates, d.Format("2006-01-02"))
			}
		}

	case "monthly":
		interval := rec.Interval
		if interval < 1 {
			interval = 1
		}
		for d := start; !d.After(end); d = d.AddDate(0, interval, 0) {
			dates = append(dates, d.Format("2006-01-02"))
		}

	case "yearly":
		interval := rec.Interval
		if interval < 1 {
			interval = 1
		}
		for d := start; !d.After(end); d = d.AddDate(interval, 0, 0) {
			dates = append(dates, d.Format("2006-01-02"))
		}
	}

	return dates
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
	// Template propagation flag: when true and task is a template, update all occurrences
	PropagateToOccurrences bool `json:"propagate_to_occurrences"`
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

	// Track changes for propagation
	changedFields := make(map[string]bool)

	if req.Title != "" {
		task.Title = req.Title
		changedFields["title"] = true
	}
	if req.Status != "" {
		task.Status = model.EntityStatus(req.Status)
	}
	if req.Icon != "" {
		task.Icon = req.Icon
		changedFields["icon"] = true
	}
	if req.Location != "" {
		task.Location = req.Location
		changedFields["location"] = true
	}
	if req.Tags != nil {
		task.Tags = req.Tags
		changedFields["tags"] = true
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
	if req.Recurrence != nil || c.Request().Method == http.MethodPut {
		task.Recurrence = req.Recurrence
		changedFields["recurrence"] = true
	}
	if req.Subtasks != nil {
		task.Subtasks = req.Subtasks
		changedFields["subtasks"] = true
	}
	if req.Body != "" || c.Request().Method == http.MethodPut {
		task.Body = req.Body
		changedFields["body"] = true
	}
	task.UpdatedAt = time.Now().UTC()

	// Propagate changes to occurrences if requested and this is a template
	if req.PropagateToOccurrences && task.IsTemplate && len(changedFields) > 0 {
		occurrences, err := h.vault.ListTasksByParent(task.ID)
		if err == nil {
			for _, occ := range occurrences {
				if changedFields["title"] {
					occ.Title = task.Title
				}
				if changedFields["icon"] {
					occ.Icon = task.Icon
				}
				if changedFields["location"] {
					occ.Location = task.Location
				}
				if changedFields["tags"] {
					occ.Tags = copySlice(task.Tags)
				}
				if changedFields["subtasks"] {
					occ.Subtasks = resetSubtasks(task.Subtasks)
				}
				if changedFields["body"] {
					occ.Body = task.Body
				}
				occ.UpdatedAt = time.Now().UTC()
				h.vault.WriteTask(occ)
			}
		}
	}

	if err := h.vault.WriteTask(task); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save task: %v", err)))
	}

	return c.JSON(http.StatusOK, model.DataResponse(task))
}

func (h *TaskHandler) Delete(c echo.Context) error {
	id := c.Param("id")

	// Read the task first to check if it's a template
	task, err := h.vault.ReadTask(id)
	if err == nil && task.IsTemplate {
		// Delete all occurrences first
		occurrences, listErr := h.vault.ListTasksByParent(task.ID)
		if listErr == nil {
			for _, occ := range occurrences {
				h.vault.Delete(model.TypeTask, occ.ID)
			}
		}
	}

	if err := h.vault.Delete(model.TypeTask, id); err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("task not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("delete task: %v", err)))
	}
	return c.JSON(http.StatusOK, model.DataResponse(nil))
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

func copySlice(s []string) []string {
	if s == nil {
		return nil
	}
	result := make([]string, len(s))
	copy(result, s)
	return result
}
