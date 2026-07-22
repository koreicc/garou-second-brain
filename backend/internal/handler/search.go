package handler

import (
	"net/http"
	"strings"

	"github.com/koreicc/garou-second-brain/backend/internal/model"
	"github.com/koreicc/garou-second-brain/backend/internal/vault"
	"github.com/labstack/echo/v4"
)

type SearchHandler struct {
	vault *vault.Vault
}

type SearchResult struct {
	ID    string      `json:"id"`
	Type  string      `json:"type"`
	Title string      `json:"title"`
	Data  interface{} `json:"data,omitempty"`
}

func NewSearchHandler(v *vault.Vault) *SearchHandler {
	return &SearchHandler{vault: v}
}

func (h *SearchHandler) Search(c echo.Context) error {
	q := c.QueryParam("q")
	if strings.TrimSpace(q) == "" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("query parameter 'q' is required"))
	}

	q = strings.ToLower(strings.TrimSpace(q))
	var results []SearchResult

	notes, err := h.vault.ListNotes()
	if err == nil {
		for _, n := range notes {
			if matchesEntity(n.Title, n.Tags, n.Body, q) {
				results = append(results, SearchResult{
					ID:    n.ID,
					Type:  model.TypeNote,
					Title: n.Title,
					Data:  n,
				})
			}
		}
	}

	tasks, err := h.vault.ListTasks()
	if err == nil {
		for _, t := range tasks {
			if matchesEntity(t.Title, t.Tags, t.Body, q) {
				results = append(results, SearchResult{
					ID:    t.ID,
					Type:  model.TypeTask,
					Title: t.Title,
					Data:  t,
				})
			}
		}
	}

	qts, err := h.vault.ListQuickTasks()
	if err == nil {
		for _, qt := range qts {
			if strings.Contains(strings.ToLower(qt.Title), q) {
				results = append(results, SearchResult{
					ID:    qt.ID,
					Type:  model.TypeQuickTask,
					Title: qt.Title,
					Data:  qt,
				})
			}
		}
	}

	people, err := h.vault.ListPeople()
	if err == nil {
		for _, p := range people {
			if matchesEntity(p.Name, p.Tags, p.Notes, q) {
				results = append(results, SearchResult{
					ID:    p.ID,
					Type:  model.TypePerson,
					Title: p.Name,
					Data:  p,
				})
			}
		}
	}

	if results == nil {
		results = []SearchResult{}
	}

	return c.JSON(http.StatusOK, model.DataResponse(results))
}

// WikiLink resolves a [[wikilink]] reference (by title or ID) to an entity.
// GET /api/v1/wikilink?q=Title
func (h *SearchHandler) WikiLink(c echo.Context) error {
	q := c.QueryParam("q")
	if strings.TrimSpace(q) == "" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse("query parameter 'q' is required"))
	}

	entityType, id, title, err := h.vault.ResolveWikiLink(strings.TrimSpace(q))
	if err != nil {
		return c.JSON(http.StatusNotFound, model.ErrorResponse("wikilink not found: "+q))
	}

	return c.JSON(http.StatusOK, model.DataResponse(map[string]string{
		"type":  entityType,
		"id":    id,
		"title": title,
	}))
}

func matchesEntity(title string, tags []string, body string, query string) bool {
	if strings.Contains(strings.ToLower(title), query) {
		return true
	}
	for _, tag := range tags {
		if strings.Contains(strings.ToLower(tag), query) {
			return true
		}
	}
	if strings.Contains(strings.ToLower(body), query) {
		return true
	}
	return false
}
