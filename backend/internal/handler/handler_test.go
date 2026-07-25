package handler

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/koreicc/garou-second-brain/backend/internal/model"
	"github.com/koreicc/garou-second-brain/backend/internal/vault"
	"github.com/labstack/echo/v4"
)

func setupTestVault(t *testing.T) *vault.Vault {
	t.Helper()
	dir, err := os.MkdirTemp("", "handler-test-*")
	if err != nil {
		t.Fatalf("create temp dir: %v", err)
	}
	t.Cleanup(func() { os.RemoveAll(dir) })

	v := vault.New(dir)
	if err := v.Init(); err != nil {
		t.Fatalf("init vault: %v", err)
	}
	return v
}

func setupEcho() *echo.Echo {
	e := echo.New()
	e.Use(func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(c echo.Context) error {
			c.Response().Header().Set("Content-Type", "application/json")
			return next(c)
		}
	})
	return e
}

func parseResponse(t *testing.T, body []byte) model.ApiResponse {
	t.Helper()
	var resp model.ApiResponse
	if err := json.Unmarshal(body, &resp); err != nil {
		t.Fatalf("parse response: %v", err)
	}
	return resp
}

// ---------- Note Handler Tests ----------

func TestNoteHandlerCRUD(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewNoteHandler(v)

	// Create
	createBody := `{"title":"Test Note","body":"Hello world","tags":["test"],"links":[]}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/notes", strings.NewReader(createBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.Create(c); err != nil {
		t.Fatalf("Create: %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("Create status = %d, want %d", rec.Code, http.StatusCreated)
	}

	resp := parseResponse(t, rec.Body.Bytes())
	if resp.Error != "" {
		t.Fatalf("Create error: %s", resp.Error)
	}

	noteData, err := json.Marshal(resp.Data)
	if err != nil {
		t.Fatalf("marshal note data: %v", err)
	}
	var note model.Note
	if err := json.Unmarshal(noteData, &note); err != nil {
		t.Fatalf("unmarshal note: %v", err)
	}
	if note.Title != "Test Note" {
		t.Fatalf("Title = %q, want %q", note.Title, "Test Note")
	}
	if note.ID == "" {
		t.Fatal("ID should not be empty")
	}

	noteID := note.ID

	// Get
	req2 := httptest.NewRequest(http.MethodGet, "/api/v1/notes/"+noteID, nil)
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)
	c2.SetParamNames("id")
	c2.SetParamValues(noteID)

	if err := h.Get(c2); err != nil {
		t.Fatalf("Get: %v", err)
	}
	if rec2.Code != http.StatusOK {
		t.Fatalf("Get status = %d, want %d", rec2.Code, http.StatusOK)
	}

	resp2 := parseResponse(t, rec2.Body.Bytes())
	if resp2.Error != "" {
		t.Fatalf("Get error: %s", resp2.Error)
	}
	noteData2, _ := json.Marshal(resp2.Data)
	var note2 model.Note
	json.Unmarshal(noteData2, &note2)
	if note2.Title != "Test Note" {
		t.Fatalf("Get Title = %q, want %q", note2.Title, "Test Note")
	}

	// Update
	updateBody := `{"title":"Updated Note","body":"Updated body"}`
	req3 := httptest.NewRequest(http.MethodPut, "/api/v1/notes/"+noteID, strings.NewReader(updateBody))
	req3.Header.Set(echo.HeaderContentType, "application/json")
	rec3 := httptest.NewRecorder()
	c3 := e.NewContext(req3, rec3)
	c3.SetParamNames("id")
	c3.SetParamValues(noteID)

	if err := h.Update(c3); err != nil {
		t.Fatalf("Update: %v", err)
	}
	if rec3.Code != http.StatusOK {
		t.Fatalf("Update status = %d, want %d", rec3.Code, http.StatusOK)
	}

	resp3 := parseResponse(t, rec3.Body.Bytes())
	noteData3, _ := json.Marshal(resp3.Data)
	var note3 model.Note
	json.Unmarshal(noteData3, &note3)
	if note3.Title != "Updated Note" {
		t.Fatalf("Update Title = %q, want %q", note3.Title, "Updated Note")
	}

	// List
	req4 := httptest.NewRequest(http.MethodGet, "/api/v1/notes", nil)
	rec4 := httptest.NewRecorder()
	c4 := e.NewContext(req4, rec4)

	if err := h.List(c4); err != nil {
		t.Fatalf("List: %v", err)
	}
	if rec4.Code != http.StatusOK {
		t.Fatalf("List status = %d, want %d", rec4.Code, http.StatusOK)
	}

	resp4 := parseResponse(t, rec4.Body.Bytes())
	notesData, _ := json.Marshal(resp4.Data)
	var notes []*model.Note
	json.Unmarshal(notesData, &notes)
	if len(notes) != 1 {
		t.Fatalf("List len = %d, want 1", len(notes))
	}

	// Delete
	req5 := httptest.NewRequest(http.MethodDelete, "/api/v1/notes/"+noteID, nil)
	rec5 := httptest.NewRecorder()
	c5 := e.NewContext(req5, rec5)
	c5.SetParamNames("id")
	c5.SetParamValues(noteID)

	if err := h.Delete(c5); err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if rec5.Code != http.StatusOK {
		t.Fatalf("Delete status = %d, want %d", rec5.Code, http.StatusOK)
	}

	// Verify deleted
	req6 := httptest.NewRequest(http.MethodGet, "/api/v1/notes/"+noteID, nil)
	rec6 := httptest.NewRecorder()
	c6 := e.NewContext(req6, rec6)
	c6.SetParamNames("id")
	c6.SetParamValues(noteID)

	if err := h.Get(c6); err != nil {
		t.Fatalf("Get after delete: %v", err)
	}
	if rec6.Code != http.StatusNotFound {
		t.Fatalf("Get after delete status = %d, want %d", rec6.Code, http.StatusNotFound)
	}
}

func TestNoteCreateValidation(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewNoteHandler(v)

	tests := []struct {
		name       string
		body       string
		wantStatus int
	}{
		{
			name:       "empty title",
			body:       `{"title":""}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "missing title field",
			body:       `{"body":"hello"}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "invalid json",
			body:       `{invalid}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "empty body",
			body:       `{}`,
			wantStatus: http.StatusBadRequest,
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodPost, "/api/v1/notes", strings.NewReader(tc.body))
			req.Header.Set(echo.HeaderContentType, "application/json")
			rec := httptest.NewRecorder()
			c := e.NewContext(req, rec)

			if err := h.Create(c); err != nil {
				t.Fatalf("Create: %v", err)
			}
			if rec.Code != tc.wantStatus {
				t.Fatalf("status = %d, want %d, body=%s", rec.Code, tc.wantStatus, rec.Body.String())
			}
		})
	}
}

func TestNoteGetNotFound(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewNoteHandler(v)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/notes/nonexistent", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	c.SetParamNames("id")
	c.SetParamValues("nonexistent")

	if err := h.Get(c); err != nil {
		t.Fatalf("Get: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusNotFound)
	}
}

func TestNoteDeleteNotFound(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewNoteHandler(v)

	req := httptest.NewRequest(http.MethodDelete, "/api/v1/notes/nonexistent", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	c.SetParamNames("id")
	c.SetParamValues("nonexistent")

	if err := h.Delete(c); err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusNotFound)
	}
}

func TestNoteListEmpty(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewNoteHandler(v)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/notes", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.List(c); err != nil {
		t.Fatalf("List: %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusOK)
	}

	resp := parseResponse(t, rec.Body.Bytes())
	notesData, _ := json.Marshal(resp.Data)
	var notes []*model.Note
	json.Unmarshal(notesData, &notes)
	if len(notes) != 0 {
		t.Fatalf("len = %d, want 0", len(notes))
	}
}

// ---------- Task Handler Tests ----------

func TestTaskHandlerCRUD(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewTaskHandler(v)

	// Create
	createBody := `{"title":"Test Task","body":"Task body","icon":"edit","location":"Home","tags":["work"]}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/tasks", strings.NewReader(createBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.Create(c); err != nil {
		t.Fatalf("Create: %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("Create status = %d, want %d", rec.Code, http.StatusCreated)
	}

	resp := parseResponse(t, rec.Body.Bytes())
	taskData, _ := json.Marshal(resp.Data)
	var task model.Task
	json.Unmarshal(taskData, &task)
	if task.Title != "Test Task" {
		t.Fatalf("Title = %q, want %q", task.Title, "Test Task")
	}

	taskID := task.ID

	// Get
	req2 := httptest.NewRequest(http.MethodGet, "/api/v1/tasks/"+taskID, nil)
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)
	c2.SetParamNames("id")
	c2.SetParamValues(taskID)

	if err := h.Get(c2); err != nil {
		t.Fatalf("Get: %v", err)
	}
	if rec2.Code != http.StatusOK {
		t.Fatalf("Get status = %d, want %d", rec2.Code, http.StatusOK)
	}

	// Update
	updateBody := `{"title":"Updated Task","status":"completed"}`
	req3 := httptest.NewRequest(http.MethodPut, "/api/v1/tasks/"+taskID, strings.NewReader(updateBody))
	req3.Header.Set(echo.HeaderContentType, "application/json")
	rec3 := httptest.NewRecorder()
	c3 := e.NewContext(req3, rec3)
	c3.SetParamNames("id")
	c3.SetParamValues(taskID)

	if err := h.Update(c3); err != nil {
		t.Fatalf("Update: %v", err)
	}
	if rec3.Code != http.StatusOK {
		t.Fatalf("Update status = %d, want %d", rec3.Code, http.StatusOK)
	}

	// List
	req4 := httptest.NewRequest(http.MethodGet, "/api/v1/tasks", nil)
	rec4 := httptest.NewRecorder()
	c4 := e.NewContext(req4, rec4)

	if err := h.List(c4); err != nil {
		t.Fatalf("List: %v", err)
	}
	if rec4.Code != http.StatusOK {
		t.Fatalf("List status = %d, want %d", rec4.Code, http.StatusOK)
	}

	resp4 := parseResponse(t, rec4.Body.Bytes())
	tasksData, _ := json.Marshal(resp4.Data)
	var tasks []*model.Task
	json.Unmarshal(tasksData, &tasks)
	if len(tasks) != 1 {
		t.Fatalf("List len = %d, want 1", len(tasks))
	}

	// Delete
	req5 := httptest.NewRequest(http.MethodDelete, "/api/v1/tasks/"+taskID, nil)
	rec5 := httptest.NewRecorder()
	c5 := e.NewContext(req5, rec5)
	c5.SetParamNames("id")
	c5.SetParamValues(taskID)

	if err := h.Delete(c5); err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if rec5.Code != http.StatusOK {
		t.Fatalf("Delete status = %d, want %d", rec5.Code, http.StatusOK)
	}
}

func TestTaskCreateValidation(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewTaskHandler(v)

	tests := []struct {
		name       string
		body       string
		wantStatus int
	}{
		{
			name:       "empty title",
			body:       `{"title":""}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "missing title",
			body:       `{"location":"Home"}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "invalid json",
			body:       `not-json`,
			wantStatus: http.StatusBadRequest,
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodPost, "/api/v1/tasks", strings.NewReader(tc.body))
			req.Header.Set(echo.HeaderContentType, "application/json")
			rec := httptest.NewRecorder()
			c := e.NewContext(req, rec)

			if err := h.Create(c); err != nil {
				t.Fatalf("Create: %v", err)
			}
			if rec.Code != tc.wantStatus {
				t.Fatalf("status = %d, want %d, body=%s", rec.Code, tc.wantStatus, rec.Body.String())
			}
		})
	}
}

func TestTaskGetNotFound(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewTaskHandler(v)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/tasks/nonexistent", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	c.SetParamNames("id")
	c.SetParamValues("nonexistent")

	if err := h.Get(c); err != nil {
		t.Fatalf("Get: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusNotFound)
	}
}

// ---------- QuickTask Handler Tests ----------

func TestQuickTaskHandlerCRUD(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewQuickTaskHandler(v)

	// Create
	createBody := `{"title":"Buy milk"}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/quick-tasks", strings.NewReader(createBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.Create(c); err != nil {
		t.Fatalf("Create: %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("Create status = %d, want %d", rec.Code, http.StatusCreated)
	}

	resp := parseResponse(t, rec.Body.Bytes())
	qtData, _ := json.Marshal(resp.Data)
	var qt model.QuickTask
	json.Unmarshal(qtData, &qt)
	if qt.Title != "Buy milk" {
		t.Fatalf("Title = %q, want %q", qt.Title, "Buy milk")
	}

	qtID := qt.ID

	// List
	req2 := httptest.NewRequest(http.MethodGet, "/api/v1/quick-tasks", nil)
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)

	if err := h.List(c2); err != nil {
		t.Fatalf("List: %v", err)
	}
	if rec2.Code != http.StatusOK {
		t.Fatalf("List status = %d, want %d", rec2.Code, http.StatusOK)
	}

	resp2 := parseResponse(t, rec2.Body.Bytes())
	qtsData, _ := json.Marshal(resp2.Data)
	var qts []*model.QuickTask
	json.Unmarshal(qtsData, &qts)
	if len(qts) != 1 {
		t.Fatalf("List len = %d, want 1", len(qts))
	}

	// MarkComplete
	req3 := httptest.NewRequest(http.MethodPut, "/api/v1/quick-tasks/"+qtID+"/complete", nil)
	rec3 := httptest.NewRecorder()
	c3 := e.NewContext(req3, rec3)
	c3.SetParamNames("id")
	c3.SetParamValues(qtID)

	if err := h.MarkComplete(c3); err != nil {
		t.Fatalf("MarkComplete: %v", err)
	}
	if rec3.Code != http.StatusOK {
		t.Fatalf("MarkComplete status = %d, want %d", rec3.Code, http.StatusOK)
	}

	resp3 := parseResponse(t, rec3.Body.Bytes())
	qtData2, _ := json.Marshal(resp3.Data)
	var qt2 model.QuickTask
	json.Unmarshal(qtData2, &qt2)
	if qt2.Status != model.QuickTaskStatusCompleted {
		t.Fatalf("Status = %q, want %q", qt2.Status, model.QuickTaskStatusCompleted)
	}

	// Delete
	req4 := httptest.NewRequest(http.MethodDelete, "/api/v1/quick-tasks/"+qtID, nil)
	rec4 := httptest.NewRecorder()
	c4 := e.NewContext(req4, rec4)
	c4.SetParamNames("id")
	c4.SetParamValues(qtID)

	if err := h.Delete(c4); err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if rec4.Code != http.StatusOK {
		t.Fatalf("Delete status = %d, want %d", rec4.Code, http.StatusOK)
	}
}

func TestQuickTaskCreateValidation(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewQuickTaskHandler(v)

	tests := []struct {
		name       string
		body       string
		wantStatus int
	}{
		{
			name:       "empty title",
			body:       `{"title":""}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "missing title",
			body:       `{}`,
			wantStatus: http.StatusBadRequest,
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodPost, "/api/v1/quick-tasks", strings.NewReader(tc.body))
			req.Header.Set(echo.HeaderContentType, "application/json")
			rec := httptest.NewRecorder()
			c := e.NewContext(req, rec)

			if err := h.Create(c); err != nil {
				t.Fatalf("Create: %v", err)
			}
			if rec.Code != tc.wantStatus {
				t.Fatalf("status = %d, want %d, body=%s", rec.Code, tc.wantStatus, rec.Body.String())
			}
		})
	}
}

func TestQuickTaskMarkCompleteNotFound(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewQuickTaskHandler(v)

	req := httptest.NewRequest(http.MethodPut, "/api/v1/quick-tasks/nonexistent/complete", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	c.SetParamNames("id")
	c.SetParamValues("nonexistent")

	if err := h.MarkComplete(c); err != nil {
		t.Fatalf("MarkComplete: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusNotFound)
	}
}

// ---------- Person Handler Tests ----------

func TestPersonHandlerCRUD(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewPersonHandler(v)

	// Create
	createBody := `{"name":"John Doe","contacts":[{"type":"phone","value":"+905551234567","label":"Personal"}],"tags":["friend"]}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/people", strings.NewReader(createBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.Create(c); err != nil {
		t.Fatalf("Create: %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("Create status = %d, want %d", rec.Code, http.StatusCreated)
	}

	resp := parseResponse(t, rec.Body.Bytes())
	pData, _ := json.Marshal(resp.Data)
	var person model.Person
	json.Unmarshal(pData, &person)
	if person.Name != "John Doe" {
		t.Fatalf("Name = %q, want %q", person.Name, "John Doe")
	}
	if len(person.Contacts) != 1 {
		t.Fatalf("len(Contacts) = %d, want 1", len(person.Contacts))
	}

	personID := person.ID

	// Get
	req2 := httptest.NewRequest(http.MethodGet, "/api/v1/people/"+personID, nil)
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)
	c2.SetParamNames("id")
	c2.SetParamValues(personID)

	if err := h.Get(c2); err != nil {
		t.Fatalf("Get: %v", err)
	}
	if rec2.Code != http.StatusOK {
		t.Fatalf("Get status = %d, want %d", rec2.Code, http.StatusOK)
	}

	// Update
	updateBody := `{"name":"John Updated","notes":"Updated notes"}`
	req3 := httptest.NewRequest(http.MethodPut, "/api/v1/people/"+personID, strings.NewReader(updateBody))
	req3.Header.Set(echo.HeaderContentType, "application/json")
	rec3 := httptest.NewRecorder()
	c3 := e.NewContext(req3, rec3)
	c3.SetParamNames("id")
	c3.SetParamValues(personID)

	if err := h.Update(c3); err != nil {
		t.Fatalf("Update: %v", err)
	}
	if rec3.Code != http.StatusOK {
		t.Fatalf("Update status = %d, want %d", rec3.Code, http.StatusOK)
	}

	// List
	req4 := httptest.NewRequest(http.MethodGet, "/api/v1/people", nil)
	rec4 := httptest.NewRecorder()
	c4 := e.NewContext(req4, rec4)

	if err := h.List(c4); err != nil {
		t.Fatalf("List: %v", err)
	}
	if rec4.Code != http.StatusOK {
		t.Fatalf("List status = %d, want %d", rec4.Code, http.StatusOK)
	}

	resp4 := parseResponse(t, rec4.Body.Bytes())
	peopleData, _ := json.Marshal(resp4.Data)
	var people []*model.Person
	json.Unmarshal(peopleData, &people)
	if len(people) != 1 {
		t.Fatalf("List len = %d, want 1", len(people))
	}

	// Delete
	req5 := httptest.NewRequest(http.MethodDelete, "/api/v1/people/"+personID, nil)
	rec5 := httptest.NewRecorder()
	c5 := e.NewContext(req5, rec5)
	c5.SetParamNames("id")
	c5.SetParamValues(personID)

	if err := h.Delete(c5); err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if rec5.Code != http.StatusOK {
		t.Fatalf("Delete status = %d, want %d", rec5.Code, http.StatusOK)
	}
}

func TestPersonCreateValidation(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewPersonHandler(v)

	tests := []struct {
		name       string
		body       string
		wantStatus int
	}{
		{
			name:       "empty name",
			body:       `{"name":""}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "missing name",
			body:       `{"tags":["friend"]}`,
			wantStatus: http.StatusBadRequest,
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodPost, "/api/v1/people", strings.NewReader(tc.body))
			req.Header.Set(echo.HeaderContentType, "application/json")
			rec := httptest.NewRecorder()
			c := e.NewContext(req, rec)

			if err := h.Create(c); err != nil {
				t.Fatalf("Create: %v", err)
			}
			if rec.Code != tc.wantStatus {
				t.Fatalf("status = %d, want %d, body=%s", rec.Code, tc.wantStatus, rec.Body.String())
			}
		})
	}
}

func TestPersonGetNotFound(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewPersonHandler(v)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/people/nonexistent", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	c.SetParamNames("id")
	c.SetParamValues("nonexistent")

	if err := h.Get(c); err != nil {
		t.Fatalf("Get: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusNotFound)
	}
}

// ---------- Search Handler Tests ----------

func TestSearchHandler(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewSearchHandler(v)

	// Create some test data
	noteHandler := NewNoteHandler(v)
	taskHandler := NewTaskHandler(v)
	personHandler := NewPersonHandler(v)

	createEntity := func(handler func(echo.Context) error, body string) {
		req := httptest.NewRequest(http.MethodPost, "/", strings.NewReader(body))
		req.Header.Set(echo.HeaderContentType, "application/json")
		rec := httptest.NewRecorder()
		c := e.NewContext(req, rec)
		if err := handler(c); err != nil {
			t.Fatalf("create entity: %v", err)
		}
	}

	createEntity(noteHandler.Create, `{"title":"Go Programming","body":"Learning Go language","tags":["go","programming"]}`)
	createEntity(noteHandler.Create, `{"title":"JavaScript Guide","body":"JS basics","tags":["js"]}`)
	createEntity(taskHandler.Create, `{"title":"Write Go tests","body":"Add unit tests"}`)
	createEntity(personHandler.Create, `{"name":"Go Developer","tags":["go"]}`)

	tests := []struct {
		name       string
		query      string
		wantMin    int
		wantStatus int
	}{
		{
			name:       "search by title",
			query:      "Go",
			wantMin:    3,
			wantStatus: http.StatusOK,
		},
		{
			name:       "search by tag",
			query:      "programming",
			wantMin:    1,
			wantStatus: http.StatusOK,
		},
		{
			name:       "search by body",
			query:      "Language",
			wantMin:    1,
			wantStatus: http.StatusOK,
		},
		{
			name:       "no results",
			query:      "zzzznotexist",
			wantMin:    0,
			wantStatus: http.StatusOK,
		},
		{
			name:       "empty query",
			query:      "",
			wantStatus: http.StatusBadRequest,
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			var url string
			if tc.query != "" {
				url = "/api/v1/search?q=" + tc.query
			} else {
				url = "/api/v1/search"
			}
			req := httptest.NewRequest(http.MethodGet, url, nil)
			rec := httptest.NewRecorder()
			c := e.NewContext(req, rec)

			if err := h.Search(c); err != nil {
				t.Fatalf("Search: %v", err)
			}
			if rec.Code != tc.wantStatus {
				t.Fatalf("status = %d, want %d, body=%s", rec.Code, tc.wantStatus, rec.Body.String())
			}

			if tc.wantStatus == http.StatusOK {
				resp := parseResponse(t, rec.Body.Bytes())
				resultsData, _ := json.Marshal(resp.Data)
				var results []SearchResult
				json.Unmarshal(resultsData, &results)
				if len(results) < tc.wantMin {
					t.Fatalf("got %d results, want at least %d", len(results), tc.wantMin)
				}
			}
		})
	}
}

func TestSearchHandlerEmptyQuery(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewSearchHandler(v)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/search", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.Search(c); err != nil {
		t.Fatalf("Search: %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusBadRequest)
	}
}

// ---------- WikiLink Handler Tests ----------

func TestWikiLinkByTitle(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewSearchHandler(v)
	noteHandler := NewNoteHandler(v)

	// Create a note
	createBody := `{"title":"Go Programming","body":"Learning Go"}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/notes", strings.NewReader(createBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	if err := noteHandler.Create(c); err != nil {
		t.Fatalf("Create note: %v", err)
	}

	// Resolve by title
	req2 := httptest.NewRequest(http.MethodGet, "/api/v1/wikilink?q=Go+Programming", nil)
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)

	if err := h.WikiLink(c2); err != nil {
		t.Fatalf("WikiLink: %v", err)
	}
	if rec2.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d, body=%s", rec2.Code, http.StatusOK, rec2.Body.String())
	}

	resp := parseResponse(t, rec2.Body.Bytes())
	data, _ := resp.Data.(map[string]interface{})
	if data == nil {
		t.Fatal("expected data map")
	}
	if data["type"] != "note" {
		t.Fatalf("type = %q, want %q", data["type"], "note")
	}
	if data["title"] != "Go Programming" {
		t.Fatalf("title = %q, want %q", data["title"], "Go Programming")
	}
}

func TestWikiLinkByID(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewSearchHandler(v)
	noteHandler := NewNoteHandler(v)

	// Create a note and capture its ID
	createBody := `{"title":"Test Note"}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/notes", strings.NewReader(createBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	if err := noteHandler.Create(c); err != nil {
		t.Fatalf("Create note: %v", err)
	}

	resp := parseResponse(t, rec.Body.Bytes())
	noteData, _ := json.Marshal(resp.Data)
	var note model.Note
	json.Unmarshal(noteData, &note)

	// Resolve by ID
	req2 := httptest.NewRequest(http.MethodGet, "/api/v1/wikilink?q="+note.ID, nil)
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)

	if err := h.WikiLink(c2); err != nil {
		t.Fatalf("WikiLink: %v", err)
	}
	if rec2.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", rec2.Code, http.StatusOK)
	}
}

func TestWikiLinkNotFound(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewSearchHandler(v)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/wikilink?q=Nonexistent", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.WikiLink(c); err != nil {
		t.Fatalf("WikiLink: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusNotFound)
	}
}

func TestWikiLinkEmptyQuery(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewSearchHandler(v)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/wikilink", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.WikiLink(c); err != nil {
		t.Fatalf("WikiLink: %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusBadRequest)
	}
}

// ---------- Error Response Tests ----------

func TestErrorResponseFormat(t *testing.T) {
	resp := model.ErrorResponse("something went wrong")
	if resp.Data != nil {
		t.Fatalf("Data = %v, want nil", resp.Data)
	}
	if resp.Error != "something went wrong" {
		t.Fatalf("Error = %q, want %q", resp.Error, "something went wrong")
	}
}

func TestDataResponseFormat(t *testing.T) {
	resp := model.DataResponse(map[string]string{"key": "value"})
	if resp.Error != "" {
		t.Fatalf("Error = %q, want empty", resp.Error)
	}
	if resp.Data == nil {
		t.Fatal("Data should not be nil")
	}
}

// ---------- Habit Handler Tests ----------

func TestHabitHandlerCRUD(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewHabitHandler(v)

	// Create
	createBody := `{"title":"Morning Exercise","days_of_week":[1,2,3,4,5],"icon":"run","tags":["health"]}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/habits", strings.NewReader(createBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.Create(c); err != nil {
		t.Fatalf("Create: %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("Create status = %d, want %d", rec.Code, http.StatusCreated)
	}

	resp := parseResponse(t, rec.Body.Bytes())
	if resp.Error != "" {
		t.Fatalf("Create error: %s", resp.Error)
	}

	habitData, _ := json.Marshal(resp.Data)
	var habit model.Habit
	json.Unmarshal(habitData, &habit)
	if habit.Title != "Morning Exercise" {
		t.Fatalf("Title = %q, want %q", habit.Title, "Morning Exercise")
	}
	if habit.ID == "" {
		t.Fatal("ID should not be empty")
	}
	if habit.Type != model.TypeHabit {
		t.Fatalf("Type = %q, want %q", habit.Type, model.TypeHabit)
	}
	if len(habit.DaysOfWeek) != 5 {
		t.Fatalf("DaysOfWeek len = %d, want 5", len(habit.DaysOfWeek))
	}

	habitID := habit.ID

	// Get
	req2 := httptest.NewRequest(http.MethodGet, "/api/v1/habits/"+habitID, nil)
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)
	c2.SetParamNames("id")
	c2.SetParamValues(habitID)

	if err := h.Get(c2); err != nil {
		t.Fatalf("Get: %v", err)
	}
	if rec2.Code != http.StatusOK {
		t.Fatalf("Get status = %d, want %d", rec2.Code, http.StatusOK)
	}

	resp2 := parseResponse(t, rec2.Body.Bytes())
	habitData2, _ := json.Marshal(resp2.Data)
	var habit2 model.Habit
	json.Unmarshal(habitData2, &habit2)
	if habit2.Title != "Morning Exercise" {
		t.Fatalf("Get Title = %q, want %q", habit2.Title, "Morning Exercise")
	}

	// Update
	updateBody := `{"title":"Evening Exercise","days_of_week":[1,3,5]}`
	req3 := httptest.NewRequest(http.MethodPut, "/api/v1/habits/"+habitID, strings.NewReader(updateBody))
	req3.Header.Set(echo.HeaderContentType, "application/json")
	rec3 := httptest.NewRecorder()
	c3 := e.NewContext(req3, rec3)
	c3.SetParamNames("id")
	c3.SetParamValues(habitID)

	if err := h.Update(c3); err != nil {
		t.Fatalf("Update: %v", err)
	}
	if rec3.Code != http.StatusOK {
		t.Fatalf("Update status = %d, want %d", rec3.Code, http.StatusOK)
	}

	resp3 := parseResponse(t, rec3.Body.Bytes())
	habitData3, _ := json.Marshal(resp3.Data)
	var habit3 model.Habit
	json.Unmarshal(habitData3, &habit3)
	if habit3.Title != "Evening Exercise" {
		t.Fatalf("Update Title = %q, want %q", habit3.Title, "Evening Exercise")
	}

	// List
	req4 := httptest.NewRequest(http.MethodGet, "/api/v1/habits", nil)
	rec4 := httptest.NewRecorder()
	c4 := e.NewContext(req4, rec4)

	if err := h.List(c4); err != nil {
		t.Fatalf("List: %v", err)
	}
	if rec4.Code != http.StatusOK {
		t.Fatalf("List status = %d, want %d", rec4.Code, http.StatusOK)
	}

	resp4 := parseResponse(t, rec4.Body.Bytes())
	habitsData, _ := json.Marshal(resp4.Data)
	var habits []*model.Habit
	json.Unmarshal(habitsData, &habits)
	if len(habits) != 1 {
		t.Fatalf("List len = %d, want 1", len(habits))
	}

	// Delete
	req5 := httptest.NewRequest(http.MethodDelete, "/api/v1/habits/"+habitID, nil)
	rec5 := httptest.NewRecorder()
	c5 := e.NewContext(req5, rec5)
	c5.SetParamNames("id")
	c5.SetParamValues(habitID)

	if err := h.Delete(c5); err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if rec5.Code != http.StatusOK {
		t.Fatalf("Delete status = %d, want %d", rec5.Code, http.StatusOK)
	}

	// Verify deleted
	req6 := httptest.NewRequest(http.MethodGet, "/api/v1/habits/"+habitID, nil)
	rec6 := httptest.NewRecorder()
	c6 := e.NewContext(req6, rec6)
	c6.SetParamNames("id")
	c6.SetParamValues(habitID)

	if err := h.Get(c6); err != nil {
		t.Fatalf("Get after delete: %v", err)
	}
	if rec6.Code != http.StatusNotFound {
		t.Fatalf("Get after delete status = %d, want %d", rec6.Code, http.StatusNotFound)
	}
}

func TestHabitCreateValidation(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewHabitHandler(v)

	tests := []struct {
		name       string
		body       string
		wantStatus int
	}{
		{
			name:       "empty title",
			body:       `{"title":"","days_of_week":[1]}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "missing title",
			body:       `{"days_of_week":[1]}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "missing days_of_week",
			body:       `{"title":"Exercise"}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "empty days_of_week",
			body:       `{"title":"Exercise","days_of_week":[]}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "invalid day value",
			body:       `{"title":"Exercise","days_of_week":[0]}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "invalid day value too high",
			body:       `{"title":"Exercise","days_of_week":[8]}`,
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "invalid json",
			body:       `{invalid}`,
			wantStatus: http.StatusBadRequest,
		},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodPost, "/api/v1/habits", strings.NewReader(tc.body))
			req.Header.Set(echo.HeaderContentType, "application/json")
			rec := httptest.NewRecorder()
			c := e.NewContext(req, rec)

			if err := h.Create(c); err != nil {
				t.Fatalf("Create: %v", err)
			}
			if rec.Code != tc.wantStatus {
				t.Fatalf("status = %d, want %d, body=%s", rec.Code, tc.wantStatus, rec.Body.String())
			}
		})
	}
}

func TestHabitGetNotFound(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewHabitHandler(v)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/habits/nonexistent", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	c.SetParamNames("id")
	c.SetParamValues("nonexistent")

	if err := h.Get(c); err != nil {
		t.Fatalf("Get: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusNotFound)
	}
}

func TestHabitDeleteNotFound(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewHabitHandler(v)

	req := httptest.NewRequest(http.MethodDelete, "/api/v1/habits/nonexistent", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	c.SetParamNames("id")
	c.SetParamValues("nonexistent")

	if err := h.Delete(c); err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusNotFound)
	}
}

func TestHabitListEmpty(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewHabitHandler(v)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/habits", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.List(c); err != nil {
		t.Fatalf("List: %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusOK)
	}

	resp := parseResponse(t, rec.Body.Bytes())
	habitsData, _ := json.Marshal(resp.Data)
	var habits []*model.Habit
	json.Unmarshal(habitsData, &habits)
	if len(habits) != 0 {
		t.Fatalf("len = %d, want 0", len(habits))
	}
}

func TestHabitComplete(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewHabitHandler(v)

	// Create a habit
	createBody := `{"title":"Meditation","days_of_week":[1,2,3,4,5,6,7]}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/habits", strings.NewReader(createBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.Create(c); err != nil {
		t.Fatalf("Create: %v", err)
	}

	resp := parseResponse(t, rec.Body.Bytes())
	habitData, _ := json.Marshal(resp.Data)
	var habit model.Habit
	json.Unmarshal(habitData, &habit)
	habitID := habit.ID

	// Complete
	req2 := httptest.NewRequest(http.MethodPost, "/api/v1/habits/"+habitID+"/complete", nil)
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)
	c2.SetParamNames("id")
	c2.SetParamValues(habitID)

	if err := h.Complete(c2); err != nil {
		t.Fatalf("Complete: %v", err)
	}
	if rec2.Code != http.StatusOK {
		t.Fatalf("Complete status = %d, want %d", rec2.Code, http.StatusOK)
	}

	resp2 := parseResponse(t, rec2.Body.Bytes())
	habitData2, _ := json.Marshal(resp2.Data)
	var habit2 model.Habit
	json.Unmarshal(habitData2, &habit2)
	if !habit2.TodayCompleted {
		t.Fatal("TodayCompleted should be true after completing")
	}
}

func TestHabitCompleteNotFound(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewHabitHandler(v)

	req := httptest.NewRequest(http.MethodPost, "/api/v1/habits/nonexistent/complete", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	c.SetParamNames("id")
	c.SetParamValues("nonexistent")

	if err := h.Complete(c); err != nil {
		t.Fatalf("Complete: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusNotFound)
	}
}

func TestHabitToday(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewHabitHandler(v)

	// Create habits with all 7 days
	createBody := `{"title":"Daily Habit","days_of_week":[1,2,3,4,5,6,7]}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/habits", strings.NewReader(createBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.Create(c); err != nil {
		t.Fatalf("Create: %v", err)
	}

	// Get today's habits
	req2 := httptest.NewRequest(http.MethodGet, "/api/v1/habits/today", nil)
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)

	if err := h.Today(c2); err != nil {
		t.Fatalf("Today: %v", err)
	}
	if rec2.Code != http.StatusOK {
		t.Fatalf("Today status = %d, want %d", rec2.Code, http.StatusOK)
	}

	resp2 := parseResponse(t, rec2.Body.Bytes())
	habitsData, _ := json.Marshal(resp2.Data)
	var todayHabits []*model.Habit
	json.Unmarshal(habitsData, &todayHabits)
	if len(todayHabits) != 1 {
		t.Fatalf("Today len = %d, want 1", len(todayHabits))
	}
	if todayHabits[0].Title != "Daily Habit" {
		t.Fatalf("Title = %q, want %q", todayHabits[0].Title, "Daily Habit")
	}
	if todayHabits[0].TodayCompleted {
		t.Fatal("TodayCompleted should be false initially")
	}
}

func TestHabitTodayEmpty(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewHabitHandler(v)

	req := httptest.NewRequest(http.MethodGet, "/api/v1/habits/today", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.Today(c); err != nil {
		t.Fatalf("Today: %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusOK)
	}

	resp := parseResponse(t, rec.Body.Bytes())
	habitsData, _ := json.Marshal(resp.Data)
	var todayHabits []*model.Habit
	json.Unmarshal(habitsData, &todayHabits)
	if len(todayHabits) != 0 {
		t.Fatalf("len = %d, want 0", len(todayHabits))
	}
}

func TestHabitTodayFiltersCorrectDay(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	h := NewHabitHandler(v)

	// Create a habit only on Monday (1)
	createBody := `{"title":"Monday Habit","days_of_week":[1]}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/habits", strings.NewReader(createBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if err := h.Create(c); err != nil {
		t.Fatalf("Create: %v", err)
	}

	// Get today's habits — may or may not include it depending on day of week
	req2 := httptest.NewRequest(http.MethodGet, "/api/v1/habits/today", nil)
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)

	if err := h.Today(c2); err != nil {
		t.Fatalf("Today: %v", err)
	}
	if rec2.Code != http.StatusOK {
		t.Fatalf("Today status = %d, want %d", rec2.Code, http.StatusOK)
	}

	// Verify the list is correct for today
	resp2 := parseResponse(t, rec2.Body.Bytes())
	habitsData, _ := json.Marshal(resp2.Data)
	var todayHabits []*model.Habit
	json.Unmarshal(habitsData, &todayHabits)

	today := time.Now().Weekday()
	isMonday := today == time.Monday
	if isMonday && len(todayHabits) != 1 {
		t.Fatalf("Today (Monday) len = %d, want 1", len(todayHabits))
	}
	if !isMonday && len(todayHabits) != 0 {
		t.Fatalf("Today (not Monday) len = %d, want 0", len(todayHabits))
	}
}

func TestHabitListDoesNotIncludeTasks(t *testing.T) {
	v := setupTestVault(t)
	e := setupEcho()
	habitHandler := NewHabitHandler(v)
	taskHandler := NewTaskHandler(v)

	// Create a task
	taskBody := `{"title":"A Task"}`
	req := httptest.NewRequest(http.MethodPost, "/api/v1/tasks", strings.NewReader(taskBody))
	req.Header.Set(echo.HeaderContentType, "application/json")
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)
	if err := taskHandler.Create(c); err != nil {
		t.Fatalf("Create task: %v", err)
	}

	// Create a habit
	habitBody := `{"title":"A Habit","days_of_week":[1,2,3,4,5]}`
	req2 := httptest.NewRequest(http.MethodPost, "/api/v1/habits", strings.NewReader(habitBody))
	req2.Header.Set(echo.HeaderContentType, "application/json")
	rec2 := httptest.NewRecorder()
	c2 := e.NewContext(req2, rec2)
	if err := habitHandler.Create(c2); err != nil {
		t.Fatalf("Create habit: %v", err)
	}

	// List habits — should only return the habit
	req3 := httptest.NewRequest(http.MethodGet, "/api/v1/habits", nil)
	rec3 := httptest.NewRecorder()
	c3 := e.NewContext(req3, rec3)

	if err := habitHandler.List(c3); err != nil {
		t.Fatalf("List: %v", err)
	}

	resp := parseResponse(t, rec3.Body.Bytes())
	habitsData, _ := json.Marshal(resp.Data)
	var habits []*model.Habit
	json.Unmarshal(habitsData, &habits)
	if len(habits) != 1 {
		t.Fatalf("habits len = %d, want 1", len(habits))
	}
	if habits[0].Title != "A Habit" {
		t.Fatalf("Title = %q, want %q", habits[0].Title, "A Habit")
	}

	// List tasks — should only return the task
	req4 := httptest.NewRequest(http.MethodGet, "/api/v1/tasks", nil)
	rec4 := httptest.NewRecorder()
	c4 := e.NewContext(req4, rec4)

	if err := taskHandler.List(c4); err != nil {
		t.Fatalf("List tasks: %v", err)
	}

	resp4 := parseResponse(t, rec4.Body.Bytes())
	tasksData, _ := json.Marshal(resp4.Data)
	var tasks []*model.Task
	json.Unmarshal(tasksData, &tasks)
	if len(tasks) != 1 {
		t.Fatalf("tasks len = %d, want 1", len(tasks))
	}
}
