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

type HabitHandler struct {
	vault *vault.Vault
}

func NewHabitHandler(v *vault.Vault) *HabitHandler {
	return &HabitHandler{vault: v}
}

// weekdayToInt converts Go's time.Weekday (0=Sun..6=Sat) to 1=Mon..7=Sun.
func weekdayToInt(wd time.Weekday) int {
	if wd == time.Sunday {
		return 7
	}
	return int(wd)
}

// isHabitScheduledForDate returns true if the habit's days_of_week includes
// the given date's weekday.
func isHabitScheduledForDate(h *model.Habit, date time.Weekday) bool {
	habitWeekday := weekdayToInt(date)
	for _, d := range h.DaysOfWeek {
		if habitWeekday == d {
			return true
		}
	}
	return false
}

// List returns all habits.
func (h *HabitHandler) List(c echo.Context) error {
	habits, err := h.vault.ListHabits()
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list habits: %v", err)))
	}
	if habits == nil {
		habits = []*model.Habit{}
	}
	return c.JSON(http.StatusOK, model.DataResponse(habits))
}

// Get returns a habit by ID.
func (h *HabitHandler) Get(c echo.Context) error {
	id := c.Param("id")
	habit, err := h.vault.ReadHabit(id)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("habit not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read habit: %v", err)))
	}
	return c.JSON(http.StatusOK, model.DataResponse(habit))
}

type CreateHabitRequest struct {
	Title           string          `json:"title"`
	Status          string          `json:"status"`
	Icon            string          `json:"icon"`
	Location        string          `json:"location"`
	Priority        string          `json:"priority"`
	Tags            []string        `json:"tags"`
	Links           []string        `json:"links"`
	DaysOfWeek      []int           `json:"days_of_week"`
	TimeMode        string          `json:"time_mode"`
	StartTime       string          `json:"start_time"`
	EndTime         string          `json:"end_time"`
	DurationMinutes int             `json:"duration_minutes"`
	DueTime         string          `json:"due_time"`
	Subtasks        []model.Subtask `json:"subtasks"`
	Body            string          `json:"body"`
}

func (h *HabitHandler) Create(c echo.Context) error {
	var req CreateHabitRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}
	if req.Title == "" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("title is required"))
	}
	if len(req.DaysOfWeek) == 0 {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("days_of_week is required"))
	}

	now := time.Now().UTC()
	id := uuid.New().String()

	habitStatus := model.StatusActive
	if req.Status != "" {
		habitStatus = model.EntityStatus(req.Status)
	}

	habit := &model.Habit{
		BaseEntity: model.BaseEntity{
			ID:        id,
			Type:      model.TypeHabit,
			Status:    habitStatus,
			Tags:      req.Tags,
			Links:     req.Links,
			CreatedAt: now,
			UpdatedAt: now,
		},
		Title:           req.Title,
		Icon:            req.Icon,
		Location:        req.Location,
		Priority:        req.Priority,
		DaysOfWeek:      req.DaysOfWeek,
		TimeMode:        req.TimeMode,
		StartTime:       req.StartTime,
		EndTime:         req.EndTime,
		DurationMinutes: req.DurationMinutes,
		DueTime:         req.DueTime,
		Subtasks:        req.Subtasks,
		Body:            req.Body,
	}
	if habit.Tags == nil {
		habit.Tags = []string{}
	}
	if habit.Links == nil {
		habit.Links = []string{}
	}
	if habit.Subtasks == nil {
		habit.Subtasks = []model.Subtask{}
	}

	if err := habit.Validate(); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse(err.Error()))
	}

	if err := h.vault.WriteHabit(habit); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save habit: %v", err)))
	}

	return c.JSON(http.StatusCreated, model.DataResponse(habit))
}

type UpdateHabitRequest struct {
	Title           string          `json:"title"`
	Status          string          `json:"status"`
	Icon            string          `json:"icon"`
	Location        string          `json:"location"`
	Priority        string          `json:"priority"`
	Tags            []string        `json:"tags"`
	Links           []string        `json:"links"`
	DaysOfWeek      []int           `json:"days_of_week"`
	TimeMode        string          `json:"time_mode"`
	StartTime       string          `json:"start_time"`
	EndTime         string          `json:"end_time"`
	DurationMinutes int             `json:"duration_minutes"`
	DueTime         string          `json:"due_time"`
	Subtasks        []model.Subtask `json:"subtasks"`
	Body            string          `json:"body"`
}

func (h *HabitHandler) Update(c echo.Context) error {
	id := c.Param("id")
	habit, err := h.vault.ReadHabit(id)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("habit not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read habit: %v", err)))
	}

	var req UpdateHabitRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("invalid request body"))
	}

	if req.Title != "" {
		habit.Title = req.Title
	}
	if req.Status != "" {
		habit.Status = model.EntityStatus(req.Status)
	}
	if req.Icon != "" {
		habit.Icon = req.Icon
	}
	if req.Location != "" {
		habit.Location = req.Location
	}
	if req.Priority != "" {
		habit.Priority = req.Priority
	}
	if req.Tags != nil {
		habit.Tags = req.Tags
	}
	if req.Links != nil {
		habit.Links = req.Links
	}
	if req.DaysOfWeek != nil {
		habit.DaysOfWeek = req.DaysOfWeek
	}
	if req.TimeMode != "" {
		habit.TimeMode = req.TimeMode
	}
	if req.StartTime != "" {
		habit.StartTime = req.StartTime
	}
	if req.EndTime != "" {
		habit.EndTime = req.EndTime
	}
	if req.DurationMinutes != 0 {
		habit.DurationMinutes = req.DurationMinutes
	}
	if req.DueTime != "" {
		habit.DueTime = req.DueTime
	}
	if req.Subtasks != nil {
		habit.Subtasks = req.Subtasks
	}
	if req.Body != "" {
		habit.Body = req.Body
	}
	habit.UpdatedAt = time.Now().UTC()

	if err := habit.Validate(); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse(err.Error()))
	}

	if err := h.vault.WriteHabit(habit); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save habit: %v", err)))
	}

	return c.JSON(http.StatusOK, model.DataResponse(habit))
}

func (h *HabitHandler) Delete(c echo.Context) error {
	id := c.Param("id")
	if err := h.vault.Delete(model.TypeHabit, id); err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("habit not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("delete habit: %v", err)))
	}
	return c.JSON(http.StatusOK, model.DataResponse(nil))
}

// Complete marks today's occurrence of a habit as completed.
func (h *HabitHandler) Complete(c echo.Context) error {
	id := c.Param("id")
	habit, err := h.vault.ReadHabit(id)
	if err != nil {
		if isNotFound(err) {
			return c.JSON(http.StatusNotFound, model.ErrorResponse("habit not found"))
		}
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("read habit: %v", err)))
	}

	now := timezoneNow(c)
	today := now.Format("2006-01-02")

	if err := h.vault.WriteHabitCompletion(habit.ID, today, true); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("save completion: %v", err)))
	}

	habit.TodayCompleted = true
	return c.JSON(http.StatusOK, model.DataResponse(habit))
}

// Today returns habits scheduled for today with their completion status.
func (h *HabitHandler) Today(c echo.Context) error {
	habits, err := h.vault.ListHabits()
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse(fmt.Sprintf("list habits: %v", err)))
	}
	if habits == nil {
		habits = []*model.Habit{}
	}

	now := timezoneNow(c)
	today := now.Format("2006-01-02")
	todayWeekday := now.Weekday()

	var todayHabits []*model.Habit
	for _, habit := range habits {
		if !isHabitScheduledForDate(habit, todayWeekday) {
			continue
		}
		completed, err := h.vault.ReadHabitCompletion(habit.ID, today)
		if err != nil {
			continue
		}
		habit.TodayCompleted = completed
		todayHabits = append(todayHabits, habit)
	}

	if todayHabits == nil {
		todayHabits = []*model.Habit{}
	}
	return c.JSON(http.StatusOK, model.DataResponse(todayHabits))
}
