package vault

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/koreicc/garou-second-brain/backend/internal/model"
	"gopkg.in/yaml.v3"
	_ "modernc.org/sqlite"
)

type Vault struct {
	db   *sql.DB
	root string
	stmt *statements
}

// statements holds prepared SQL statements for frequent operations.
type statements struct {
	getEntity      *sql.Stmt
	getBody        *sql.Stmt
	listByType     *sql.Stmt
	insertEntity   *sql.Stmt
	updateEntity   *sql.Stmt
	deleteEntity   *sql.Stmt
	deleteBody     *sql.Stmt
	insertBody     *sql.Stmt
	listSubtasks   *sql.Stmt
	insertSubtask  *sql.Stmt
	deleteSubtasks *sql.Stmt
}

const (
	dbFilename = ".second-brain.db"

	DirNotes      = "notes"
	DirTasks      = "tasks"
	DirQuickTasks = "quick-tasks"
	DirPeople     = "people"
	DirArchive    = "archive"
	DirOverrides  = "overrides"
)

var ErrNotFound = fmt.Errorf("not found")

func New(root string) *Vault {
	return &Vault{root: root}
}

// Init opens the SQLite database and creates tables if needed.
func (v *Vault) Init() error {
	dirs := []string{DirNotes, DirTasks, DirQuickTasks, DirPeople, DirArchive, DirOverrides}
	for _, d := range dirs {
		path := filepath.Join(v.root, d)
		if err := os.MkdirAll(path, 0755); err != nil {
			return fmt.Errorf("create vault dir %s: %w", d, err)
		}
	}

	dbPath := filepath.Join(v.root, dbFilename)
	db, err := sql.Open("sqlite", dbPath+"?_journal_mode=WAL&_busy_timeout=5000")
	if err != nil {
		return fmt.Errorf("open database: %w", err)
	}
	v.db = db

	if _, err := db.Exec(schemaSQL); err != nil {
		return fmt.Errorf("create schema: %w", err)
	}

	if err := v.prepareStatements(); err != nil {
		return fmt.Errorf("prepare statements: %w", err)
	}

	// Migrate existing .md files if database is empty.
	if err := v.migrateIfNeeded(); err != nil {
		return fmt.Errorf("migration: %w", err)
	}

	return nil
}

func (v *Vault) Root() string { return v.root }

func (v *Vault) Close() error { return v.db.Close() }

// ---------------------------------------------------------------------------
// Prepared statements
// ---------------------------------------------------------------------------

func (v *Vault) prepareStatements() error {
	var err error
	v.stmt = &statements{}

	v.stmt.getEntity, err = v.db.Prepare(`SELECT id,type,title,status,icon,location,priority,
		tags,links,parent_id,is_template,occurrence_date,date_mode,due_date,start_date,end_date,
		time_mode,start_time,end_time,duration_minutes,due_time,days_of_week,name,contacts,
		social_links,notes_body,body,created_at,updated_at FROM entities WHERE id=?`)
	if err != nil {
		return err
	}

	v.stmt.getBody, err = v.db.Prepare(`SELECT body FROM entities WHERE id=?`)
	if err != nil {
		return err
	}

	v.stmt.listByType, err = v.db.Prepare(`SELECT id,type,title,status,icon,location,priority,
		tags,links,parent_id,is_template,occurrence_date,date_mode,due_date,start_date,end_date,
		time_mode,start_time,end_time,duration_minutes,due_time,days_of_week,name,contacts,
		social_links,notes_body,body,created_at,updated_at FROM entities WHERE type=?`)
	if err != nil {
		return err
	}

	v.stmt.insertEntity, err = v.db.Prepare(`INSERT OR REPLACE INTO entities
		(id,type,title,status,icon,location,priority,tags,links,parent_id,is_template,
		 occurrence_date,date_mode,due_date,start_date,end_date,time_mode,start_time,end_time,
		 duration_minutes,due_time,days_of_week,name,contacts,social_links,notes_body,body,
		 created_at,updated_at)
		VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`)
	if err != nil {
		return err
	}

	v.stmt.deleteEntity, err = v.db.Prepare(`DELETE FROM entities WHERE id=?`)
	if err != nil {
		return err
	}

	v.stmt.listSubtasks, err = v.db.Prepare(`SELECT id,title,completed FROM subtasks WHERE task_id=? ORDER BY rowid`)
	if err != nil {
		return err
	}

	v.stmt.insertSubtask, err = v.db.Prepare(`INSERT OR REPLACE INTO subtasks (id,task_id,title,completed) VALUES (?,?,?,?)`)
	if err != nil {
		return err
	}

	v.stmt.deleteSubtasks, err = v.db.Prepare(`DELETE FROM subtasks WHERE task_id=?`)
	if err != nil {
		return err
	}

	return nil
}

// ---------------------------------------------------------------------------
// Schema row helpers (scan / build)
// ---------------------------------------------------------------------------

// entityRow holds all columns from the entities table.
type entityRow struct {
	ID              string
	Type            string
	Title           string
	Status          string
	Icon            string
	Location        string
	Priority        string
	Tags            string // JSON
	Links           string // JSON
	ParentID        string
	IsTemplate      bool
	OccurrenceDate  string
	DateMode        string
	DueDate         *string
	StartDate       *string
	EndDate         *string
	TimeMode        string
	StartTime       string
	EndTime         string
	DurationMinutes int
	DueTime         string
	DaysOfWeek      string // JSON
	Name            string
	Contacts        string // JSON
	SocialLinks     string // JSON
	NotesBody       string
	Body            string
	CreatedAt       string
	UpdatedAt       string
}

func scanEntity(scanner interface {
	Scan(dest ...interface{}) error
}) (entityRow, error) {
	var r entityRow
	err := scanner.Scan(
		&r.ID, &r.Type, &r.Title, &r.Status, &r.Icon, &r.Location, &r.Priority,
		&r.Tags, &r.Links, &r.ParentID, &r.IsTemplate, &r.OccurrenceDate,
		&r.DateMode, &r.DueDate, &r.StartDate, &r.EndDate,
		&r.TimeMode, &r.StartTime, &r.EndTime, &r.DurationMinutes, &r.DueTime,
		&r.DaysOfWeek, &r.Name, &r.Contacts, &r.SocialLinks, &r.NotesBody,
		&r.Body, &r.CreatedAt, &r.UpdatedAt,
	)
	return r, err
}

func (r entityRow) toNote() *model.Note {
	return &model.Note{
		BaseEntity: model.BaseEntity{
			ID:        r.ID,
			Type:      r.Type,
			Status:    model.EntityStatus(r.Status),
			Tags:      parseJSONStringSlice(r.Tags),
			Links:     parseJSONStringSlice(r.Links),
			CreatedAt: mustParseTime(r.CreatedAt),
			UpdatedAt: mustParseTime(r.UpdatedAt),
		},
		Title: r.Title,
		Body:  r.Body,
	}
}

func (r entityRow) toTask() *model.Task {
	task := &model.Task{
		BaseEntity: model.BaseEntity{
			ID:        r.ID,
			Type:      r.Type,
			Status:    model.EntityStatus(r.Status),
			Tags:      parseJSONStringSlice(r.Tags),
			Links:     parseJSONStringSlice(r.Links),
			CreatedAt: mustParseTime(r.CreatedAt),
			UpdatedAt: mustParseTime(r.UpdatedAt),
		},
		Title:           r.Title,
		Icon:            r.Icon,
		Location:        r.Location,
		Priority:        r.Priority,
		ParentID:        r.ParentID,
		IsTemplate:      r.IsTemplate,
		OccurrenceDate:  r.OccurrenceDate,
		DateMode:        r.DateMode,
		DueDate:         parseNullableTime(r.DueDate),
		StartDate:       parseNullableTime(r.StartDate),
		EndDate:         parseNullableTime(r.EndDate),
		TimeMode:        r.TimeMode,
		StartTime:       r.StartTime,
		EndTime:         r.EndTime,
		DurationMinutes: r.DurationMinutes,
		DueTime:         r.DueTime,
		Body:            r.Body,
	}
	return task
}

func (r entityRow) toQuickTask() *model.QuickTask {
	return &model.QuickTask{
		BaseEntity: model.BaseEntity{
			ID:        r.ID,
			Type:      r.Type,
			Status:    model.EntityStatus(r.Status),
			Tags:      parseJSONStringSlice(r.Tags),
			Links:     parseJSONStringSlice(r.Links),
			CreatedAt: mustParseTime(r.CreatedAt),
			UpdatedAt: mustParseTime(r.UpdatedAt),
		},
		Title: r.Title,
	}
}

func (r entityRow) toPerson() *model.Person {
	return &model.Person{
		BaseEntity: model.BaseEntity{
			ID:        r.ID,
			Type:      r.Type,
			Status:    model.EntityStatus(r.Status),
			Tags:      parseJSONStringSlice(r.Tags),
			Links:     parseJSONStringSlice(r.Links),
			CreatedAt: mustParseTime(r.CreatedAt),
			UpdatedAt: mustParseTime(r.UpdatedAt),
		},
		Name:        r.Name,
		Contacts:    parseJSONContacts(r.Contacts),
		SocialLinks: parseJSONSocialLinks(r.SocialLinks),
		Notes:       r.NotesBody,
		Body:        r.Body,
	}
}

func (r entityRow) toHabit() *model.Habit {
	return &model.Habit{
		BaseEntity: model.BaseEntity{
			ID:        r.ID,
			Type:      r.Type,
			Status:    model.EntityStatus(r.Status),
			Tags:      parseJSONStringSlice(r.Tags),
			Links:     parseJSONStringSlice(r.Links),
			CreatedAt: mustParseTime(r.CreatedAt),
			UpdatedAt: mustParseTime(r.UpdatedAt),
		},
		Title:           r.Title,
		Icon:            r.Icon,
		Location:        r.Location,
		Priority:        r.Priority,
		DaysOfWeek:      parseJSONIntSlice(r.DaysOfWeek),
		TimeMode:        r.TimeMode,
		StartTime:       r.StartTime,
		EndTime:         r.EndTime,
		DurationMinutes: r.DurationMinutes,
		DueTime:         r.DueTime,
		Body:            r.Body,
	}
}

// buildEntityArgs builds the 29 arguments for insertEntity from any entity type.
func buildEntityArgs(id, entityType, title, status string, base model.BaseEntity, extras map[string]interface{}) []interface{} {
	args := make([]interface{}, 29)
	args[0] = id
	args[1] = entityType
	args[2] = title
	args[3] = status
	args[4] = getExtras(extras, "icon", "")
	args[5] = getExtras(extras, "location", "")
	args[6] = getExtras(extras, "priority", "")
	args[7] = toJSON(base.Tags)
	args[8] = toJSON(base.Links)
	args[9] = getExtras(extras, "parent_id", "")
	args[10] = getExtrasInt(extras, "is_template", 0)
	args[11] = getExtras(extras, "occurrence_date", "")
	args[12] = getExtras(extras, "date_mode", "")
	args[13] = getExtrasPtr(extras, "due_date")
	args[14] = getExtrasPtr(extras, "start_date")
	args[15] = getExtrasPtr(extras, "end_date")
	args[16] = getExtras(extras, "time_mode", "")
	args[17] = getExtras(extras, "start_time", "")
	args[18] = getExtras(extras, "end_time", "")
	args[19] = getExtrasInt(extras, "duration_minutes", 0)
	args[20] = getExtras(extras, "due_time", "")
	args[21] = getExtras(extras, "days_of_week", "[]")
	args[22] = getExtras(extras, "name", "")
	args[23] = getExtras(extras, "contacts", "[]")
	args[24] = getExtras(extras, "social_links", "[]")
	args[25] = getExtras(extras, "notes_body", "")
	args[26] = getExtras(extras, "body", "")
	args[27] = base.CreatedAt.Format(time.RFC3339Nano)
	args[28] = base.UpdatedAt.Format(time.RFC3339Nano)
	return args
}

// ---------------------------------------------------------------------------
// JSON / time helpers
// ---------------------------------------------------------------------------

func toJSON(v interface{}) string {
	b, _ := json.Marshal(v)
	return string(b)
}

func parseJSONStringSlice(s string) []string {
	var result []string
	if s == "" || s == "[]" {
		return []string{}
	}
	json.Unmarshal([]byte(s), &result)
	if result == nil {
		return []string{}
	}
	return result
}

func parseJSONIntSlice(s string) []int {
	var result []int
	if s == "" || s == "[]" {
		return []int{}
	}
	json.Unmarshal([]byte(s), &result)
	if result == nil {
		return []int{}
	}
	return result
}

func parseJSONContacts(s string) []model.Contact {
	var result []model.Contact
	if s == "" || s == "[]" {
		return []model.Contact{}
	}
	json.Unmarshal([]byte(s), &result)
	if result == nil {
		return []model.Contact{}
	}
	return result
}

func parseJSONSocialLinks(s string) []model.SocialLink {
	var result []model.SocialLink
	if s == "" || s == "[]" {
		return []model.SocialLink{}
	}
	json.Unmarshal([]byte(s), &result)
	if result == nil {
		return []model.SocialLink{}
	}
	return result
}

func mustParseTime(s string) time.Time {
	t, _ := time.Parse(time.RFC3339Nano, s)
	return t
}

func parseNullableTime(s *string) *time.Time {
	if s == nil || *s == "" {
		return nil
	}
	t, err := time.Parse(time.RFC3339Nano, *s)
	if err != nil {
		return nil
	}
	return &t
}

func formatTime(t time.Time) string {
	return t.Format(time.RFC3339Nano)
}

func formatTimePtr(t *time.Time) *string {
	if t == nil {
		return nil
	}
	s := t.Format(time.RFC3339Nano)
	return &s
}

func getExtras(m map[string]interface{}, key, def string) string {
	if m == nil {
		return def
	}
	if v, ok := m[key]; ok {
		if s, ok := v.(string); ok {
			return s
		}
	}
	return def
}

func getExtrasInt(m map[string]interface{}, key string, def int) int {
	if m == nil {
		return def
	}
	if v, ok := m[key]; ok {
		if i, ok := v.(int); ok {
			return i
		}
		if i, ok := v.(bool); ok {
			if i {
				return 1
			}
			return 0
		}
	}
	return def
}

func getExtrasPtr(m map[string]interface{}, key string) *string {
	if m == nil {
		return nil
	}
	if v, ok := m[key]; ok {
		switch val := v.(type) {
		case string:
			if val == "" {
				return nil
			}
			return &val
		case *time.Time:
			if val == nil {
				return nil
			}
			s := val.Format(time.RFC3339Nano)
			return &s
		case time.Time:
			s := val.Format(time.RFC3339Nano)
			return &s
		}
	}
	return nil
}

// parseRecurrenceJSON deserializes recurrence from a JSON string.
func parseRecurrenceJSON(s string) *model.Recurrence {
	if s == "" {
		return nil
	}
	var r model.Recurrence
	if err := json.Unmarshal([]byte(s), &r); err != nil {
		return nil
	}
	return &r
}

// ---------------------------------------------------------------------------
// FTS5 full-text search
// ---------------------------------------------------------------------------

// syncFTSInsert inserts or updates the FTS index for an entity.
func (v *Vault) syncFTSInsert(entityID, title, body string) error {
	// Delete old entry first, then insert new one.
	_, err := v.db.Exec(`DELETE FROM entities_fts WHERE entity_id=?`, entityID)
	if err != nil {
		return err
	}
	_, err = v.db.Exec(`INSERT INTO entities_fts (entity_id, title, body) VALUES (?,?,?)`, entityID, title, body)
	return err
}

// syncFTSDelete removes an entity from the FTS index.
func (v *Vault) syncFTSDelete(entityID string) error {
	_, err := v.db.Exec(`DELETE FROM entities_fts WHERE entity_id=?`, entityID)
	return err
}

// SearchResult is a lightweight search hit returned by SearchFTS.
type SearchResult struct {
	EntityID string `json:"entity_id"`
	Type     string `json:"type"`
	Title    string `json:"title"`
	Snippet  string `json:"snippet"`
}

// SearchFTS performs a full-text search across all entity types.
// The query is automatically escaped for FTS5 syntax.
func (v *Vault) SearchFTS(query string) ([]SearchResult, error) {
	// Escape FTS5 special characters to prevent syntax errors.
	escaped := sanitizeFTSQuery(query)
	if escaped == "" {
		return []SearchResult{}, nil
	}

	// Wrap in double quotes for phrase matching, append * for prefix matching.
	ftsQuery := fmt.Sprintf(`"%s"*`, escaped)

	rows, err := v.db.Query(
		`SELECT e.id, e.type, e.title, e.name, e.body,
		        snippet(entities_fts, 1, '<b>', '</b>', '...', 40) AS snippet
		 FROM entities_fts f
		 JOIN entities e ON e.id = f.entity_id
		 WHERE entities_fts MATCH ?
		 ORDER BY rank
		 LIMIT 50`,
		ftsQuery,
	)
	if err != nil {
		return nil, fmt.Errorf("fts search: %w", err)
	}
	defer rows.Close()

	var results []SearchResult
	for rows.Next() {
		var id, eType, title, name, body, snippet string
		if err := rows.Scan(&id, &eType, &title, &name, &body, &snippet); err != nil {
			continue
		}
		displayTitle := title
		if displayTitle == "" {
			displayTitle = name
		}
		results = append(results, SearchResult{
			EntityID: id,
			Type:     eType,
			Title:    displayTitle,
			Snippet:  snippet,
		})
	}
	if results == nil {
		results = []SearchResult{}
	}
	return results, rows.Err()
}

// sanitizeFTSQuery removes FTS5 special characters from a user query.
func sanitizeFTSQuery(q string) string {
	var result strings.Builder
	for _, r := range q {
		switch r {
		case '"', '*', '(', ')', '{', '}', ':', '^', '~', '+', '-', '!':
			continue // strip FTS5 operators
		default:
			result.WriteRune(r)
		}
	}
	return strings.TrimSpace(result.String())
}

// ---------------------------------------------------------------------------
// .md file sync helpers (bodies)
// ---------------------------------------------------------------------------

// normalizeBody ensures body has a trailing newline (matching legacy behavior).
func normalizeBody(body string) string {
	if body == "" {
		return body
	}
	if !strings.HasSuffix(body, "\n") {
		return body + "\n"
	}
	return body
}

// writeBodyFile writes the .md file for an entity so users can edit it.
// The frontmatter contains all entity fields for round-trip fidelity.
func (v *Vault) writeBodyFile(entityType, id, body string, frontmatterObj interface{}) error {
	dir, err := entityDir(entityType)
	if err != nil {
		return err
	}
	path := filepath.Join(v.root, dir, id+".md")

	fmBytes, err := yaml.Marshal(frontmatterObj)
	if err != nil {
		return fmt.Errorf("marshal frontmatter: %w", err)
	}

	normalized := normalizeBody(body)

	var buf bytes.Buffer
	buf.WriteString("---\n")
	buf.Write(fmBytes)
	buf.WriteString("---\n")
	if normalized != "" {
		buf.WriteString(normalized)
	}

	tmpPath := path + ".tmp"
	if err := os.WriteFile(tmpPath, buf.Bytes(), 0644); err != nil {
		return fmt.Errorf("write temp: %w", err)
	}
	return os.Rename(tmpPath, path)
}

// deleteBodyFile removes the .md file for an entity.
func (v *Vault) deleteBodyFile(entityType, id string) error {
	dir, err := entityDir(entityType)
	if err != nil {
		return err
	}
	path := filepath.Join(v.root, dir, id+".md")
	err = os.Remove(path)
	if os.IsNotExist(err) {
		return nil
	}
	return err
}

// ---------------------------------------------------------------------------
// Migration from existing .md files
// ---------------------------------------------------------------------------

func (v *Vault) migrateIfNeeded() error {
	var count int
	if err := v.db.QueryRow(`SELECT COUNT(*) FROM entities`).Scan(&count); err != nil {
		return err
	}
	if count > 0 {
		// Rebuild FTS index on startup to catch any missed updates.
		return v.rebuildFTS()
	}

	// Scan all .md files and import them.
	type scanEntry struct {
		dir  string
		kind string
	}
	entries := []scanEntry{
		{DirNotes, model.TypeNote},
		{DirTasks, model.TypeTask},
		{DirQuickTasks, model.TypeQuickTask},
		{DirPeople, model.TypePerson},
	}

	for _, e := range entries {
		ids, err := v.listMDFiles(e.dir)
		if err != nil {
			continue
		}
		for _, id := range ids {
			if err := v.importMDFile(e.dir, e.kind, id); err != nil {
				log.Printf("migrate %s/%s: %v", e.dir, id, err)
			}
		}
	}
	return nil
}

func (v *Vault) listMDFiles(subdir string) ([]string, error) {
	dirPath := filepath.Join(v.root, subdir)
	entries, err := os.ReadDir(dirPath)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, err
	}
	var ids []string
	for _, entry := range entries {
		if entry.IsDir() || !strings.HasSuffix(entry.Name(), ".md") {
			continue
		}
		ids = append(ids, strings.TrimSuffix(entry.Name(), ".md"))
	}
	return ids, nil
}

func (v *Vault) importMDFile(subdir, kind, id string) error {
	path := filepath.Join(v.root, subdir, id+".md")
	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil
		}
		return err
	}

	frontmatter, body, err := splitFrontmatter(data)
	if err != nil {
		return fmt.Errorf("parse frontmatter: %w", err)
	}

	switch kind {
	case model.TypeNote:
		var note model.Note
		if err := yaml.Unmarshal(frontmatter, &note); err != nil {
			return err
		}
		note.Body = string(body)
		return v.WriteNote(&note)

	case model.TypeTask:
		var task model.Task
		if err := yaml.Unmarshal(frontmatter, &task); err != nil {
			return err
		}
		task.Body = string(body)
		return v.WriteTask(&task)

	case model.TypeQuickTask:
		var qt model.QuickTask
		if err := yaml.Unmarshal(frontmatter, &qt); err != nil {
			return err
		}
		return v.WriteQuickTask(&qt)

	case model.TypePerson:
		var person model.Person
		if err := yaml.Unmarshal(frontmatter, &person); err != nil {
			return err
		}
		person.Body = string(body)
		return v.WritePerson(&person)
	}

	return nil
}

// splitFrontmatter extracts YAML frontmatter and body from a .md file.
func splitFrontmatter(data []byte) (frontmatter, body []byte, err error) {
	data = bytes.TrimLeft(data, " \t\r\n")
	if !bytes.HasPrefix(data, []byte("---")) {
		return nil, nil, fmt.Errorf("missing opening frontmatter delimiter")
	}

	rest := data[3:]
	if len(rest) > 0 && rest[0] == '\n' {
		rest = rest[1:]
	} else if len(rest) > 1 && rest[0] == '\r' && rest[1] == '\n' {
		rest = rest[2:]
	} else {
		return nil, nil, fmt.Errorf("invalid frontmatter opening delimiter")
	}

	closingIdx := bytes.Index(rest, []byte("\n---"))
	if closingIdx == -1 {
		closingIdx = bytes.Index(rest, []byte("---"))
		if closingIdx == -1 {
			return nil, nil, fmt.Errorf("missing closing frontmatter delimiter")
		}
		frontmatter = rest[:closingIdx]
		body = rest[closingIdx+3:]
	} else {
		frontmatter = rest[:closingIdx]
		body = rest[closingIdx+4:]
	}

	if len(frontmatter) > 0 && frontmatter[len(frontmatter)-1] == '\r' {
		frontmatter = frontmatter[:len(frontmatter)-1]
	}

	body = bytes.TrimLeft(body, " \t\r\n")
	return frontmatter, body, nil
}

// entityDir returns the subdirectory name for an entity type.
func entityDir(entityType string) (string, error) {
	switch entityType {
	case model.TypeNote:
		return DirNotes, nil
	case model.TypeTask:
		return DirTasks, nil
	case model.TypeQuickTask:
		return DirQuickTasks, nil
	case model.TypePerson:
		return DirPeople, nil
	case model.TypeHabit:
		return DirTasks, nil
	default:
		return "", fmt.Errorf("unknown entity type: %s", entityType)
	}
}

// ---------------------------------------------------------------------------
// Entity CRUD: Note
// ---------------------------------------------------------------------------

func (v *Vault) ReadNote(id string) (*model.Note, error) {
	row := v.stmt.getEntity.QueryRow(id)
	r, err := scanEntity(row)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, fmt.Errorf("note %s: %w", id, ErrNotFound)
		}
		return nil, fmt.Errorf("read note %s: %w", id, err)
	}
	if r.Type != model.TypeNote {
		return nil, fmt.Errorf("note %s: %w", id, ErrNotFound)
	}
	return r.toNote(), nil
}

func (v *Vault) WriteNote(note *model.Note) error {
	if note.ID == "" {
		note.ID = uuid.New().String()
	}
	now := time.Now().UTC()
	note.UpdatedAt = now
	if note.CreatedAt.IsZero() {
		note.CreatedAt = now
	}

	note.Body = normalizeBody(note.Body)
	extras := map[string]interface{}{
		"body": note.Body,
	}
	args := buildEntityArgs(note.ID, model.TypeNote, note.Title, string(note.Status), note.BaseEntity, extras)

	tx, err := v.db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if _, err := tx.Stmt(v.stmt.insertEntity).Exec(args...); err != nil {
		return fmt.Errorf("insert note: %w", err)
	}
	if err := tx.Commit(); err != nil {
		return err
	}
	if err := v.writeBodyFile(model.TypeNote, note.ID, note.Body, note); err != nil {
		return err
	}
	return v.syncFTSInsert(note.ID, note.Title, note.Body)
}

func (v *Vault) ListNotes() ([]*model.Note, error) {
	rows, err := v.stmt.listByType.Query(model.TypeNote)
	if err != nil {
		return nil, fmt.Errorf("list notes: %w", err)
	}
	defer rows.Close()

	var notes []*model.Note
	for rows.Next() {
		r, err := scanEntity(rows)
		if err != nil {
			return nil, err
		}
		notes = append(notes, r.toNote())
	}
	if notes == nil {
		notes = []*model.Note{}
	}
	return notes, rows.Err()
}

// ---------------------------------------------------------------------------
// Entity CRUD: Task
// ---------------------------------------------------------------------------

func (v *Vault) ReadTask(id string) (*model.Task, error) {
	row := v.stmt.getEntity.QueryRow(id)
	r, err := scanEntity(row)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, fmt.Errorf("task %s: %w", id, ErrNotFound)
		}
		return nil, fmt.Errorf("read task %s: %w", id, err)
	}
	if r.Type != model.TypeTask {
		return nil, fmt.Errorf("task %s: %w", id, ErrNotFound)
	}
	task := r.toTask()
	task.Subtasks = v.loadSubtasks(id)
	return task, nil
}

func (v *Vault) WriteTask(task *model.Task) error {
	if task.ID == "" {
		task.ID = uuid.New().String()
	}
	now := time.Now().UTC()
	task.UpdatedAt = now
	if task.CreatedAt.IsZero() {
		task.CreatedAt = now
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

	task.Body = normalizeBody(task.Body)
	extras := map[string]interface{}{
		"icon":             task.Icon,
		"location":         task.Location,
		"priority":         task.Priority,
		"parent_id":        task.ParentID,
		"is_template":      task.IsTemplate,
		"occurrence_date":  task.OccurrenceDate,
		"date_mode":        task.DateMode,
		"due_date":         task.DueDate,
		"start_date":       task.StartDate,
		"end_date":         task.EndDate,
		"time_mode":        task.TimeMode,
		"start_time":       task.StartTime,
		"end_time":         task.EndTime,
		"duration_minutes": task.DurationMinutes,
		"due_time":         task.DueTime,
		"body":             task.Body,
	}
	args := buildEntityArgs(task.ID, model.TypeTask, task.Title, string(task.Status), task.BaseEntity, extras)

	tx, err := v.db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if _, err := tx.Stmt(v.stmt.insertEntity).Exec(args...); err != nil {
		return fmt.Errorf("insert task: %w", err)
	}

	// Write subtasks
	if _, err := tx.Stmt(v.stmt.deleteSubtasks).Exec(task.ID); err != nil {
		return err
	}
	for _, s := range task.Subtasks {
		if _, err := tx.Stmt(v.stmt.insertSubtask).Exec(s.ID, task.ID, s.Title, s.Completed); err != nil {
			return fmt.Errorf("insert subtask: %w", err)
		}
	}

	if err := tx.Commit(); err != nil {
		return err
	}
	if err := v.writeBodyFile(model.TypeTask, task.ID, task.Body, task); err != nil {
		return err
	}
	return v.syncFTSInsert(task.ID, task.Title, task.Body)
}

func (v *Vault) ListTasks() ([]*model.Task, error) {
	rows, err := v.stmt.listByType.Query(model.TypeTask)
	if err != nil {
		return nil, fmt.Errorf("list tasks: %w", err)
	}
	defer rows.Close()

	var tasks []*model.Task
	for rows.Next() {
		r, err := scanEntity(rows)
		if err != nil {
			return nil, err
		}
		// Skip habits that share the task type
		if r.Type == model.TypeHabit {
			continue
		}
		if !r.IsTemplate && r.ParentID != "" {
			continue
		}
		task := r.toTask()
		task.Subtasks = v.loadSubtasks(r.ID)
		tasks = append(tasks, task)
	}
	if tasks == nil {
		tasks = []*model.Task{}
	}
	return tasks, rows.Err()
}

func (v *Vault) ListTemplates() ([]*model.Task, error) {
	all, err := v.ListTasks()
	if err != nil {
		return nil, err
	}
	var templates []*model.Task
	for _, t := range all {
		if t.IsTemplate {
			templates = append(templates, t)
		}
	}
	return templates, nil
}

func (v *Vault) ListTasksByDate(date string) ([]*model.Task, error) {
	all, err := v.ListTasks()
	if err != nil {
		return nil, err
	}

	parsedDate, err := time.Parse("2006-01-02", date)
	if err != nil {
		return nil, fmt.Errorf("invalid date format %q: %w", date, err)
	}
	parsedDate = parsedDate.Truncate(24 * time.Hour)

	var matches []*model.Task

	for _, t := range all {
		if !t.IsTemplate && t.ParentID == "" {
			if t.DueDate != nil {
				dueDate := t.DueDate.Truncate(24 * time.Hour)
				if dueDate.Equal(parsedDate) {
					matches = append(matches, t)
					continue
				}
			}
			if t.DateMode == "range" && t.StartDate != nil && t.EndDate != nil {
				start := t.StartDate.Truncate(24 * time.Hour)
				end := t.EndDate.Truncate(24 * time.Hour)
				if (parsedDate.Equal(start) || parsedDate.After(start)) &&
					(parsedDate.Equal(end) || parsedDate.Before(end)) {
					matches = append(matches, t)
				}
			}
		}
	}

	for _, t := range all {
		if t.IsTemplate && DateMatchesRecurrence(parsedDate, t) {
			occ := ComputeDynamicOccurrence(t, date)
			if occ != nil {
				if override, _ := v.ReadOccurrenceOverride(t.ID, date); override != nil {
					ApplyOccurrenceOverride(occ, override)
				}
				matches = append(matches, occ)
			}
		}
	}

	return matches, nil
}

func (v *Vault) ListTasksByMonth(month string) ([]*model.Task, error) {
	startOfMonth, err := time.Parse("2006-01", month)
	if err != nil {
		return nil, fmt.Errorf("invalid month format %q: %w", month, err)
	}
	endOfMonth := startOfMonth.AddDate(0, 1, -1).Add(23*time.Hour + 59*time.Minute + 59*time.Second)

	all, err := v.ListTasks()
	if err != nil {
		return nil, err
	}

	var matches []*model.Task
	for _, t := range all {
		if t.IsTemplate {
			if t.Recurrence != nil && t.StartDate != nil {
				templateStart := t.StartDate.Truncate(24 * time.Hour)
				templateEnd := t.EndDate
				if templateEnd == nil {
					continue
				}
				searchStart := startOfMonth
				searchEnd := endOfMonth
				if templateStart.After(searchEnd) || templateEnd.Before(searchStart) {
					continue
				}
				rangeStart := templateStart
				if searchStart.After(rangeStart) {
					rangeStart = searchStart
				}
				rangeEnd := *templateEnd
				if searchEnd.Before(rangeEnd) {
					rangeEnd = searchEnd
				}
				dates := GenerateDatesInRange(rangeStart, rangeEnd, t.Recurrence)
				for _, dateStr := range dates {
					occ := ComputeDynamicOccurrence(t, dateStr)
					if occ != nil {
						if override, _ := v.ReadOccurrenceOverride(t.ID, dateStr); override != nil {
							ApplyOccurrenceOverride(occ, override)
						}
						matches = append(matches, occ)
					}
				}
			}
			continue
		}

		if t.ParentID != "" {
			continue
		}

		if t.DueDate != nil {
			due := t.DueDate.Truncate(24 * time.Hour)
			if (due.Equal(startOfMonth.Truncate(24*time.Hour)) || due.After(startOfMonth.Truncate(24*time.Hour))) &&
				(due.Equal(endOfMonth.Truncate(24*time.Hour)) || due.Before(endOfMonth.Truncate(24*time.Hour))) {
				matches = append(matches, t)
				continue
			}
		}
		if t.DateMode == "range" && t.StartDate != nil && t.EndDate != nil {
			s := t.StartDate.Truncate(24 * time.Hour)
			e := t.EndDate.Truncate(24 * time.Hour)
			if !s.After(endOfMonth.Truncate(24*time.Hour)) && !e.Before(startOfMonth.Truncate(24*time.Hour)) {
				matches = append(matches, t)
			}
		}
	}

	return matches, nil
}

func (v *Vault) ListTasksByParent(parentID string) ([]*model.Task, error) {
	parent, err := v.ReadTask(parentID)
	if err != nil {
		if errors.Is(err, ErrNotFound) {
			return []*model.Task{}, nil
		}
		return nil, err
	}

	if !parent.IsTemplate || parent.Recurrence == nil || parent.StartDate == nil {
		return []*model.Task{}, nil
	}

	if parent.EndDate == nil {
		return []*model.Task{}, nil
	}

	dates := GenerateDatesInRange(*parent.StartDate, *parent.EndDate, parent.Recurrence)
	var occurrences []*model.Task
	for _, dateStr := range dates {
		occ := ComputeDynamicOccurrence(parent, dateStr)
		if occ != nil {
			occurrences = append(occurrences, occ)
		}
	}
	return occurrences, nil
}

func (v *Vault) loadSubtasks(taskID string) []model.Subtask {
	rows, err := v.stmt.listSubtasks.Query(taskID)
	if err != nil {
		return []model.Subtask{}
	}
	defer rows.Close()

	var subtasks []model.Subtask
	for rows.Next() {
		var s model.Subtask
		if err := rows.Scan(&s.ID, &s.Title, &s.Completed); err != nil {
			continue
		}
		subtasks = append(subtasks, s)
	}
	if subtasks == nil {
		return []model.Subtask{}
	}
	return subtasks
}

// ---------------------------------------------------------------------------
// Entity CRUD: QuickTask
// ---------------------------------------------------------------------------

func (v *Vault) ReadQuickTask(id string) (*model.QuickTask, error) {
	row := v.stmt.getEntity.QueryRow(id)
	r, err := scanEntity(row)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, fmt.Errorf("quicktask %s: %w", id, ErrNotFound)
		}
		return nil, fmt.Errorf("read quicktask %s: %w", id, err)
	}
	return r.toQuickTask(), nil
}

func (v *Vault) WriteQuickTask(qt *model.QuickTask) error {
	if qt.ID == "" {
		qt.ID = uuid.New().String()
	}
	now := time.Now().UTC()
	qt.UpdatedAt = now
	if qt.CreatedAt.IsZero() {
		qt.CreatedAt = now
	}

	args := buildEntityArgs(qt.ID, model.TypeQuickTask, qt.Title, string(qt.Status), qt.BaseEntity, nil)

	tx, err := v.db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if _, err := tx.Stmt(v.stmt.insertEntity).Exec(args...); err != nil {
		return fmt.Errorf("insert quicktask: %w", err)
	}
	if err := tx.Commit(); err != nil {
		return err
	}
	return v.syncFTSInsert(qt.ID, qt.Title, "")
}

func (v *Vault) ListQuickTasks() ([]*model.QuickTask, error) {
	rows, err := v.stmt.listByType.Query(model.TypeQuickTask)
	if err != nil {
		return nil, fmt.Errorf("list quicktasks: %w", err)
	}
	defer rows.Close()

	var qts []*model.QuickTask
	for rows.Next() {
		r, err := scanEntity(rows)
		if err != nil {
			return nil, err
		}
		qts = append(qts, r.toQuickTask())
	}
	if qts == nil {
		qts = []*model.QuickTask{}
	}
	return qts, rows.Err()
}

// ---------------------------------------------------------------------------
// Entity CRUD: Person
// ---------------------------------------------------------------------------

func (v *Vault) ReadPerson(id string) (*model.Person, error) {
	row := v.stmt.getEntity.QueryRow(id)
	r, err := scanEntity(row)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, fmt.Errorf("person %s: %w", id, ErrNotFound)
		}
		return nil, fmt.Errorf("read person %s: %w", id, err)
	}
	return r.toPerson(), nil
}

func (v *Vault) WritePerson(person *model.Person) error {
	if person.ID == "" {
		person.ID = uuid.New().String()
	}
	now := time.Now().UTC()
	person.UpdatedAt = now
	if person.CreatedAt.IsZero() {
		person.CreatedAt = now
	}
	if person.Tags == nil {
		person.Tags = []string{}
	}
	if person.Links == nil {
		person.Links = []string{}
	}
	if person.Contacts == nil {
		person.Contacts = []model.Contact{}
	}
	if person.SocialLinks == nil {
		person.SocialLinks = []model.SocialLink{}
	}

	person.Body = normalizeBody(person.Body)
	extras := map[string]interface{}{
		"name":         person.Name,
		"contacts":     toJSON(person.Contacts),
		"social_links": toJSON(person.SocialLinks),
		"notes_body":   person.Notes,
		"body":         person.Body,
	}
	args := buildEntityArgs(person.ID, model.TypePerson, person.Name, string(person.Status), person.BaseEntity, extras)

	tx, err := v.db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if _, err := tx.Stmt(v.stmt.insertEntity).Exec(args...); err != nil {
		return fmt.Errorf("insert person: %w", err)
	}
	if err := tx.Commit(); err != nil {
		return err
	}
	if err := v.writeBodyFile(model.TypePerson, person.ID, person.Body, person); err != nil {
		return err
	}
	return v.syncFTSInsert(person.ID, person.Name, person.Body)
}

func (v *Vault) ListPeople() ([]*model.Person, error) {
	rows, err := v.stmt.listByType.Query(model.TypePerson)
	if err != nil {
		return nil, fmt.Errorf("list people: %w", err)
	}
	defer rows.Close()

	var people []*model.Person
	for rows.Next() {
		r, err := scanEntity(rows)
		if err != nil {
			return nil, err
		}
		people = append(people, r.toPerson())
	}
	if people == nil {
		people = []*model.Person{}
	}
	return people, rows.Err()
}

// ---------------------------------------------------------------------------
// Entity CRUD: Habit
// ---------------------------------------------------------------------------

func (v *Vault) ReadHabit(id string) (*model.Habit, error) {
	row := v.stmt.getEntity.QueryRow(id)
	r, err := scanEntity(row)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, fmt.Errorf("habit %s: %w", id, ErrNotFound)
		}
		return nil, fmt.Errorf("read habit %s: %w", id, err)
	}
	habit := r.toHabit()
	habit.Subtasks = v.loadSubtasks(id)
	return habit, nil
}

func (v *Vault) WriteHabit(habit *model.Habit) error {
	if habit.ID == "" {
		habit.ID = uuid.New().String()
	}
	now := time.Now().UTC()
	habit.UpdatedAt = now
	if habit.CreatedAt.IsZero() {
		habit.CreatedAt = now
	}
	if habit.Tags == nil {
		habit.Tags = []string{}
	}
	if habit.Links == nil {
		habit.Links = []string{}
	}
	if habit.DaysOfWeek == nil {
		habit.DaysOfWeek = []int{}
	}
	if habit.Subtasks == nil {
		habit.Subtasks = []model.Subtask{}
	}

	habit.Body = normalizeBody(habit.Body)
	extras := map[string]interface{}{
		"icon":             habit.Icon,
		"location":         habit.Location,
		"priority":         habit.Priority,
		"days_of_week":     toJSON(habit.DaysOfWeek),
		"time_mode":        habit.TimeMode,
		"start_time":       habit.StartTime,
		"end_time":         habit.EndTime,
		"duration_minutes": habit.DurationMinutes,
		"due_time":         habit.DueTime,
		"body":             habit.Body,
	}
	args := buildEntityArgs(habit.ID, model.TypeHabit, habit.Title, string(habit.Status), habit.BaseEntity, extras)

	tx, err := v.db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if _, err := tx.Stmt(v.stmt.insertEntity).Exec(args...); err != nil {
		return fmt.Errorf("insert habit: %w", err)
	}

	// Write subtasks
	if _, err := tx.Stmt(v.stmt.deleteSubtasks).Exec(habit.ID); err != nil {
		return err
	}
	for _, s := range habit.Subtasks {
		if _, err := tx.Stmt(v.stmt.insertSubtask).Exec(s.ID, habit.ID, s.Title, s.Completed); err != nil {
			return fmt.Errorf("insert subtask: %w", err)
		}
	}

	if err := tx.Commit(); err != nil {
		return err
	}
	if err := v.writeBodyFile(model.TypeHabit, habit.ID, habit.Body, habit); err != nil {
		return err
	}
	return v.syncFTSInsert(habit.ID, habit.Title, habit.Body)
}

func (v *Vault) ListHabits() ([]*model.Habit, error) {
	rows, err := v.stmt.listByType.Query(model.TypeHabit)
	if err != nil {
		return nil, fmt.Errorf("list habits: %w", err)
	}
	defer rows.Close()

	var habits []*model.Habit
	for rows.Next() {
		r, err := scanEntity(rows)
		if err != nil {
			return nil, err
		}
		habit := r.toHabit()
		habit.Subtasks = v.loadSubtasks(r.ID)
		habits = append(habits, habit)
	}
	if habits == nil {
		habits = []*model.Habit{}
	}
	return habits, rows.Err()
}

// ---------------------------------------------------------------------------
// Delete & Archive
// ---------------------------------------------------------------------------

func (v *Vault) Delete(entityType, id string) error {
	path, err := v.filePath(entityType, id)
	if err != nil {
		return err
	}

	// First check if the entity exists in the database.
	var exists bool
	checkErr := v.db.QueryRow(`SELECT EXISTS(SELECT 1 FROM entities WHERE id=?)`, id).Scan(&exists)
	if checkErr != nil || !exists {
		return fmt.Errorf("%s %s: %w", entityType, id, ErrNotFound)
	}

	// Write archive file from database data for full frontmatter fidelity.
	dir, archiveErr := entityDir(entityType)
	if archiveErr == nil {
		if err := v.writeArchiveFile(entityType, id, dir); err != nil {
			return fmt.Errorf("archive %s: %w", id, err)
		}
	}

	// Delete from database
	tx, err := v.db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if _, err := tx.Stmt(v.stmt.deleteSubtasks).Exec(id); err != nil {
		return err
	}
	if _, err := tx.Stmt(v.stmt.deleteEntity).Exec(id); err != nil {
		return fmt.Errorf("delete entity: %w", err)
	}
	if err := tx.Commit(); err != nil {
		return err
	}

	// Remove original file if it exists.
	if _, err := os.Stat(path); err == nil {
		os.Remove(path)
	}

	// Remove from FTS index (best-effort).
	v.syncFTSDelete(id)

	return nil
}

// writeArchiveFile reads an entity from SQLite and writes it as an archived .md file.
func (v *Vault) writeArchiveFile(entityType, id, dir string) error {
	archiveDir := filepath.Join(v.root, DirArchive, dir)
	if err := os.MkdirAll(archiveDir, 0755); err != nil {
		return err
	}
	archivePath := filepath.Join(archiveDir, id+".md")

	// Read entity data from SQLite to build full frontmatter.
	row := v.stmt.getEntity.QueryRow(id)
	r, err := scanEntity(row)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil // entity gone, nothing to archive
		}
		return err
	}

	var frontmatterObj interface{}
	switch entityType {
	case model.TypeNote:
		frontmatterObj = r.toNote()
	case model.TypeTask:
		frontmatterObj = r.toTask()
	case model.TypeQuickTask:
		frontmatterObj = r.toQuickTask()
	case model.TypePerson:
		frontmatterObj = r.toPerson()
	default:
		return nil
	}

	fmBytes, err := yaml.Marshal(frontmatterObj)
	if err != nil {
		return fmt.Errorf("marshal frontmatter: %w", err)
	}

	normalized := normalizeBody(r.Body)

	var buf bytes.Buffer
	buf.WriteString("---\n")
	buf.Write(fmBytes)
	buf.WriteString("---\n")
	if normalized != "" {
		buf.WriteString(normalized)
	}

	tmpPath := archivePath + ".tmp"
	if err := os.WriteFile(tmpPath, buf.Bytes(), 0644); err != nil {
		os.Remove(tmpPath)
		return fmt.Errorf("write archive: %w", err)
	}
	return os.Rename(tmpPath, archivePath)
}

func (v *Vault) Exists(entityType, id string) (bool, error) {
	var count int
	err := v.db.QueryRow(`SELECT COUNT(*) FROM entities WHERE id=?`, id).Scan(&count)
	if err != nil {
		return false, err
	}
	return count > 0, nil
}

// ---------------------------------------------------------------------------
// Archive operations
// ---------------------------------------------------------------------------

func (v *Vault) filePath(entityType, id string) (string, error) {
	dir, err := entityDir(entityType)
	if err != nil {
		return "", err
	}
	if id == "" || strings.ContainsAny(id, "/\\") || id == "." || id == ".." {
		return "", fmt.Errorf("invalid id: %q", id)
	}
	path := filepath.Join(v.root, dir, id+".md")
	cleanRoot := filepath.Clean(v.root)
	cleanPath := filepath.Clean(path)
	if cleanPath != cleanRoot && !strings.HasPrefix(cleanPath, cleanRoot+string(filepath.Separator)) {
		return "", fmt.Errorf("invalid id: %q", id)
	}
	return path, nil
}

func (v *Vault) archiveFilePath(entityType, id string) (string, error) {
	dir, err := entityDir(entityType)
	if err != nil {
		return "", err
	}
	if id == "" || strings.ContainsAny(id, "/\\") || id == "." || id == ".." {
		return "", fmt.Errorf("invalid id: %q", id)
	}
	path := filepath.Join(v.root, DirArchive, dir, id+".md")
	cleanRoot := filepath.Clean(v.root)
	cleanPath := filepath.Clean(path)
	if cleanPath != cleanRoot && !strings.HasPrefix(cleanPath, cleanRoot+string(filepath.Separator)) {
		return "", fmt.Errorf("invalid id: %q", id)
	}
	return path, nil
}

func (v *Vault) listArchivedFiles(subdir string) ([]string, error) {
	dirPath := filepath.Join(v.root, DirArchive, subdir)
	entries, err := os.ReadDir(dirPath)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, err
	}
	var ids []string
	for _, entry := range entries {
		if entry.IsDir() || !strings.HasSuffix(entry.Name(), ".md") {
			continue
		}
		ids = append(ids, strings.TrimSuffix(entry.Name(), ".md"))
	}
	return ids, nil
}

func (v *Vault) readArchivedFile(entityType, id string, dest interface{}) (string, error) {
	path, err := v.archiveFilePath(entityType, id)
	if err != nil {
		return "", err
	}
	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return "", fmt.Errorf("%s %s: %w", entityType, id, ErrNotFound)
		}
		return "", fmt.Errorf("read %s: %w", path, err)
	}
	frontmatter, body, err := splitFrontmatter(data)
	if err != nil {
		return "", fmt.Errorf("parse frontmatter %s: %w", path, err)
	}
	if err := yaml.Unmarshal(frontmatter, dest); err != nil {
		return "", fmt.Errorf("unmarshal %s: %w", path, err)
	}
	return string(body), nil
}

func (v *Vault) ListArchivedIDs(entityType string) ([]string, error) {
	dir, err := entityDir(entityType)
	if err != nil {
		return nil, err
	}
	return v.listArchivedFiles(dir)
}

func (v *Vault) ReadArchivedNote(id string) (*model.Note, error) {
	var note model.Note
	body, err := v.readArchivedFile(model.TypeNote, id, &note)
	if err != nil {
		return nil, err
	}
	note.Body = body
	return &note, nil
}

func (v *Vault) ReadArchivedTask(id string) (*model.Task, error) {
	var task model.Task
	body, err := v.readArchivedFile(model.TypeTask, id, &task)
	if err != nil {
		return nil, err
	}
	task.Body = body
	return &task, nil
}

func (v *Vault) ReadArchivedQuickTask(id string) (*model.QuickTask, error) {
	var qt model.QuickTask
	_, err := v.readArchivedFile(model.TypeQuickTask, id, &qt)
	if err != nil {
		return nil, err
	}
	return &qt, nil
}

func (v *Vault) ReadArchivedPerson(id string) (*model.Person, error) {
	var person model.Person
	body, err := v.readArchivedFile(model.TypePerson, id, &person)
	if err != nil {
		return nil, err
	}
	person.Body = body
	return &person, nil
}

func (v *Vault) RestoreArchive(entityType, id string) error {
	switch entityType {
	case model.TypeNote:
		note, err := v.ReadArchivedNote(id)
		if err != nil {
			return err
		}
		if err := v.WriteNote(note); err != nil {
			return err
		}
	case model.TypeTask:
		task, err := v.ReadArchivedTask(id)
		if err != nil {
			return err
		}
		if err := v.WriteTask(task); err != nil {
			return err
		}
	case model.TypeQuickTask:
		qt, err := v.ReadArchivedQuickTask(id)
		if err != nil {
			return err
		}
		if err := v.WriteQuickTask(qt); err != nil {
			return err
		}
	case model.TypePerson:
		person, err := v.ReadArchivedPerson(id)
		if err != nil {
			return err
		}
		if err := v.WritePerson(person); err != nil {
			return err
		}
	default:
		return fmt.Errorf("unknown entity type: %s", entityType)
	}

	archivePath, err := v.archiveFilePath(entityType, id)
	if err != nil {
		return err
	}
	if err := os.Remove(archivePath); err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("remove archived %s: %w", archivePath, err)
	}
	return nil
}

// ---------------------------------------------------------------------------
// WikiLink resolution
// ---------------------------------------------------------------------------

func (v *Vault) ResolveWikiLink(query string) (entityType string, id string, title string, err error) {
	rows, err := v.db.Query(
		`SELECT id, type, title, name FROM entities WHERE title=? OR name=? OR id=?`,
		query, query, query,
	)
	if err != nil {
		return "", "", "", fmt.Errorf("wikilink %q: %w", query, ErrNotFound)
	}
	defer rows.Close()

	for rows.Next() {
		var eType, eID, eTitle, eName string
		if err := rows.Scan(&eID, &eType, &eTitle, &eName); err != nil {
			continue
		}
		displayTitle := eTitle
		if displayTitle == "" {
			displayTitle = eName
		}
		if displayTitle == query || eID == query {
			return eType, eID, displayTitle, nil
		}
	}

	return "", "", "", fmt.Errorf("wikilink %q: %w", query, ErrNotFound)
}

// ---------------------------------------------------------------------------
// Occurrence overrides
// ---------------------------------------------------------------------------

func (v *Vault) WriteOccurrenceOverride(parentID, date string, override *OccurrenceOverride) error {
	subtasksJSON := "[]"
	if override.Subtasks != nil {
		subtasksJSON = toJSON(override.Subtasks)
	}

	_, err := v.db.Exec(
		`INSERT OR REPLACE INTO overrides (parent_id, date, status, title, body, subtasks) VALUES (?,?,?,?,?,?)`,
		parentID, date, override.Status, override.Title, override.Body, subtasksJSON,
	)
	return err
}

func (v *Vault) ReadOccurrenceOverride(parentID, date string) (*OccurrenceOverride, error) {
	var status, title, body, subtasksJSON string
	err := v.db.QueryRow(
		`SELECT status, title, body, subtasks FROM overrides WHERE parent_id=? AND date=?`,
		parentID, date,
	).Scan(&status, &title, &body, &subtasksJSON)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, fmt.Errorf("read override: %w", err)
	}

	override := &OccurrenceOverride{
		Status: status,
		Title:  title,
		Body:   body,
	}
	if subtasksJSON != "" && subtasksJSON != "[]" {
		var subtasks []model.Subtask
		if err := json.Unmarshal([]byte(subtasksJSON), &subtasks); err == nil {
			override.Subtasks = subtasks
		}
	}
	return override, nil
}

// ---------------------------------------------------------------------------
// Habit completions
// ---------------------------------------------------------------------------

func (v *Vault) WriteHabitCompletion(habitID, date string, completed bool) error {
	_, err := v.db.Exec(
		`INSERT OR REPLACE INTO habit_completions (habit_id, date, completed) VALUES (?,?,?)`,
		habitID, date, completed,
	)
	return err
}

func (v *Vault) ReadHabitCompletion(habitID, date string) (bool, error) {
	var completed bool
	err := v.db.QueryRow(
		`SELECT completed FROM habit_completions WHERE habit_id=? AND date=?`,
		habitID, date,
	).Scan(&completed)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return false, nil
		}
		return false, fmt.Errorf("read habit completion: %w", err)
	}
	return completed, nil
}

// ---------------------------------------------------------------------------
// FTS index rebuild
// ---------------------------------------------------------------------------

// rebuildFTS re-populates the FTS index from all entities.
func (v *Vault) rebuildFTS() error {
	if _, err := v.db.Exec(`DELETE FROM entities_fts`); err != nil {
		return err
	}

	rows, err := v.db.Query(`SELECT id, title, name, body FROM entities`)
	if err != nil {
		return err
	}
	defer rows.Close()

	tx, err := v.db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	stmt, err := tx.Prepare(`INSERT INTO entities_fts (entity_id, title, body) VALUES (?,?,?)`)
	if err != nil {
		return err
	}
	defer stmt.Close()

	for rows.Next() {
		var id, title, name, body string
		if err := rows.Scan(&id, &title, &name, &body); err != nil {
			continue
		}
		searchTitle := title
		if searchTitle == "" {
			searchTitle = name
		}
		stmt.Exec(id, searchTitle, body)
	}

	return tx.Commit()
}

// ---------------------------------------------------------------------------
// Background sync: watches .md files for external edits
// ---------------------------------------------------------------------------

// fileRecord tracks the mtime of a single .md file.
type fileRecord struct {
	path  string
	mtime time.Time
}

// StartSync launches a background goroutine that periodically checks .md files
// for external edits and syncs them into SQLite. Callers should invoke this
// in a goroutine: go v.StartSync(ctx)
func (v *Vault) StartSync(stop <-chan struct{}) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	// Build initial record of all .md files.
	records := v.scanMDFiles()

	for {
		select {
		case <-stop:
			return
		case <-ticker.C:
			v.syncPass(records)
		}
	}
}

// syncPass checks all .md files for changes and syncs modified ones.
func (v *Vault) syncPass(records map[string]*fileRecord) {
	current := v.scanMDFiles()

	for id, rec := range current {
		old, exists := records[id]
		if exists && old.mtime.Equal(rec.mtime) {
			continue // unchanged
		}

		// File changed or is new -- re-import.
		if err := v.syncMDFile(id, rec); err != nil {
			log.Printf("sync %s: %v", id, err)
		}
	}

	// Update records for next pass.
	for k, v := range current {
		records[k] = v
	}
}

// scanMDFiles walks the vault directories and records all .md files.
func (v *Vault) scanMDFiles() map[string]*fileRecord {
	records := make(map[string]*fileRecord)

	dirs := []struct {
		dir  string
		kind string
	}{
		{DirNotes, model.TypeNote},
		{DirTasks, ""}, // tasks + habits
		{DirQuickTasks, model.TypeQuickTask},
		{DirPeople, model.TypePerson},
	}

	for _, d := range dirs {
		entries, err := os.ReadDir(filepath.Join(v.root, d.dir))
		if err != nil {
			continue
		}
		for _, entry := range entries {
			if entry.IsDir() || !strings.HasSuffix(entry.Name(), ".md") {
				continue
			}
			id := strings.TrimSuffix(entry.Name(), ".md")
			info, err := entry.Info()
			if err != nil {
				continue
			}
			fullPath := filepath.Join(v.root, d.dir, entry.Name())
			records[id] = &fileRecord{
				path:  fullPath,
				mtime: info.ModTime(),
			}
		}
	}

	return records
}

// syncMDFile re-imports a single .md file into SQLite.
func (v *Vault) syncMDFile(id string, rec *fileRecord) error {
	data, err := os.ReadFile(rec.path)
	if err != nil {
		return err
	}

	frontmatter, bodyBytes, err := splitFrontmatter(data)
	if err != nil {
		return fmt.Errorf("parse frontmatter: %w", err)
	}

	// Determine entity type from the file's directory.
	dir := filepath.Base(filepath.Dir(rec.path))
	body := string(bodyBytes)

	switch dir {
	case DirNotes:
		var note model.Note
		if err := yaml.Unmarshal(frontmatter, &note); err != nil {
			return err
		}
		note.Body = body
		note.ID = id
		return v.WriteNote(&note)

	case DirTasks:
		// Could be task or habit -- try task first.
		var task model.Task
		if err := yaml.Unmarshal(frontmatter, &task); err != nil {
			return err
		}
		task.Body = body
		task.ID = id
		if task.Type == model.TypeHabit {
			var habit model.Habit
			if err := yaml.Unmarshal(frontmatter, &habit); err != nil {
				return err
			}
			habit.Body = body
			habit.ID = id
			return v.WriteHabit(&habit)
		}
		return v.WriteTask(&task)

	case DirQuickTasks:
		var qt model.QuickTask
		if err := yaml.Unmarshal(frontmatter, &qt); err != nil {
			return err
		}
		qt.ID = id
		return v.WriteQuickTask(&qt)

	case DirPeople:
		var person model.Person
		if err := yaml.Unmarshal(frontmatter, &person); err != nil {
			return err
		}
		person.Body = body
		person.ID = id
		return v.WritePerson(&person)
	}

	return nil
}

// ---------------------------------------------------------------------------
// Stats / health metrics
// ---------------------------------------------------------------------------

// Stats returns a map of vault metrics for monitoring.
func (v *Vault) Stats() map[string]interface{} {
	stats := map[string]interface{}{
		"database": v.root,
	}

	var total int
	v.db.QueryRow(`SELECT COUNT(*) FROM entities`).Scan(&total)
	stats["total_entities"] = total

	var ftsCount int
	v.db.QueryRow(`SELECT COUNT(*) FROM entities_fts`).Scan(&ftsCount)
	stats["fts_entries"] = ftsCount

	rows, err := v.db.Query(`SELECT type, COUNT(*) FROM entities GROUP BY type ORDER BY type`)
	if err == nil {
		defer rows.Close()
		typeCounts := make(map[string]int)
		for rows.Next() {
			var eType string
			var count int
			if err := rows.Scan(&eType, &count); err == nil {
				typeCounts[eType] = count
			}
		}
		stats["entities_by_type"] = typeCounts
	}

	var dbSize int64
	if fi, err := os.Stat(filepath.Join(v.root, dbFilename)); err == nil {
		dbSize = fi.Size()
	}
	stats["database_size_bytes"] = dbSize

	var mdCount int
	mdCount = len(v.scanMDFiles())
	stats["md_files"] = mdCount

	return stats
}

// ---------------------------------------------------------------------------
// Recurrence / occurrence helpers (unchanged from file-based version)
// ---------------------------------------------------------------------------

type OccurrenceOverride struct {
	Status   string          `json:"status,omitempty"`
	Title    string          `json:"title,omitempty"`
	Body     string          `json:"body,omitempty"`
	Subtasks []model.Subtask `json:"subtasks,omitempty"`
}

func DateMatchesRecurrence(date time.Time, template *model.Task) bool {
	if template.Recurrence == nil || template.StartDate == nil {
		return false
	}

	dateStart := date.Truncate(24 * time.Hour)
	startDate := template.StartDate.Truncate(24 * time.Hour)

	if dateStart.Before(startDate) {
		return false
	}
	if template.EndDate != nil && dateStart.After(template.EndDate.Truncate(24*time.Hour)) {
		return false
	}

	rec := template.Recurrence
	switch rec.Type {
	case "daily":
		interval := rec.Interval
		if interval < 1 {
			interval = 1
		}
		daysSince := int(dateStart.Sub(startDate).Hours() / 24)
		return daysSince >= 0 && daysSince%interval == 0

	case "weekly":
		interval := rec.Interval
		if interval < 1 {
			interval = 1
		}
		if len(rec.DaysOfWeek) > 0 {
			wd := int(date.Weekday())
			matched := false
			for _, d := range rec.DaysOfWeek {
				if wd == d {
					matched = true
					break
				}
			}
			if !matched {
				return false
			}
			daysSince := int(dateStart.Sub(startDate).Hours() / 24)
			return daysSince >= 0 && daysSince%(7*interval) == 0
		}
		daysSince := int(dateStart.Sub(startDate).Hours() / 24)
		return daysSince >= 0 && daysSince%(7*interval) == 0

	case "monthly":
		interval := rec.Interval
		if interval < 1 {
			interval = 1
		}
		monthsSince := (date.Year()-template.StartDate.Year())*12 +
			int(date.Month()) - int(template.StartDate.Month())
		return monthsSince >= 0 && monthsSince%interval == 0

	case "yearly":
		interval := rec.Interval
		if interval < 1 {
			interval = 1
		}
		yearsSince := date.Year() - template.StartDate.Year()
		return yearsSince >= 0 && yearsSince%interval == 0
	}

	return false
}

func ComputeDynamicOccurrence(template *model.Task, date string) *model.Task {
	occDate, err := time.Parse("2006-01-02", date)
	if err != nil {
		return nil
	}

	now := time.Now().UTC()

	occ := &model.Task{
		BaseEntity: model.BaseEntity{
			ID:        model.OccurrenceID(template.ID, date),
			Type:      model.TypeTask,
			Status:    model.StatusPending,
			Tags:      copySlice(template.Tags),
			Links:     copySlice(template.Links),
			CreatedAt: now,
			UpdatedAt: now,
		},
		Title:           template.Title,
		Icon:            template.Icon,
		Location:        template.Location,
		ParentID:        template.ID,
		IsTemplate:      false,
		OccurrenceDate:  date,
		DateMode:        "due_date",
		DueDate:         &occDate,
		TimeMode:        template.TimeMode,
		StartTime:       template.StartTime,
		EndTime:         template.EndTime,
		DurationMinutes: template.DurationMinutes,
		DueTime:         template.DueTime,
		Recurrence:      nil,
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
	return occ
}

func GenerateDatesInRange(start, end time.Time, rec *model.Recurrence) []string {
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
		if len(rec.DaysOfWeek) > 0 {
			dayMap := make(map[int]bool)
			for _, d := range rec.DaysOfWeek {
				dayMap[d] = true
			}
			for d := start; !d.After(end); d = d.AddDate(0, 0, 1) {
				wd := int(d.Weekday())
				if dayMap[wd] {
					dates = append(dates, d.Format("2006-01-02"))
				}
			}
		} else {
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

func ApplyOccurrenceOverride(occ *model.Task, override *OccurrenceOverride) {
	if override == nil {
		return
	}
	if override.Status != "" {
		occ.Status = model.EntityStatus(override.Status)
	}
	if override.Title != "" {
		occ.Title = override.Title
	}
	if override.Body != "" {
		occ.Body = override.Body
	}
	if override.Subtasks != nil {
		occ.Subtasks = override.Subtasks
	}
}
