package vault

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/google/uuid"
	"github.com/koreicc/garou-second-brain/backend/internal/model"
	"gopkg.in/yaml.v3"
)

type Vault struct {
	root        string
	locks       sync.Map
	lockCleanup chan string
}

const (
	DirNotes      = "notes"
	DirTasks      = "tasks"
	DirQuickTasks = "quick-tasks"
	DirPeople     = "people"
	DirArchive    = "archive"
	DirOverrides  = "overrides"
)

func New(root string) *Vault {
	v := &Vault{
		root:        root,
		lockCleanup: make(chan string, 100),
	}
	go v.cleanupLocks()
	return v
}

// cleanupLocks removes unused locks from memory to prevent memory leaks
func (v *Vault) cleanupLocks() {
	for path := range v.lockCleanup {
		v.locks.Delete(path)
	}
}

func (v *Vault) Init() error {
	dirs := []string{DirNotes, DirTasks, DirQuickTasks, DirPeople, DirArchive, DirOverrides}
	for _, d := range dirs {
		path := filepath.Join(v.root, d)
		if err := os.MkdirAll(path, 0755); err != nil {
			return fmt.Errorf("create vault dir %s: %w", d, err)
		}
	}
	return nil
}

func (v *Vault) Root() string {
	return v.root
}

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
	default:
		return "", fmt.Errorf("unknown entity type: %s", entityType)
	}
}

func (v *Vault) filePath(entityType, id string) (string, error) {
	dir, err := entityDir(entityType)
	if err != nil {
		return "", err
	}
	if id == "" {
		return "", fmt.Errorf("invalid id: %q", id)
	}
	// Reject path separators and traversal segments so an id cannot escape the
	// vault directory (e.g. "../../../etc/passwd").
	if strings.ContainsAny(id, "/\\") || id == "." || id == ".." {
		return "", fmt.Errorf("invalid id: %q", id)
	}
	path := filepath.Join(v.root, dir, id+".md")
	// Defense in depth: confirm the resolved path stays inside the vault root.
	cleanRoot := filepath.Clean(v.root)
	cleanPath := filepath.Clean(path)
	if cleanPath != cleanRoot && !strings.HasPrefix(cleanPath, cleanRoot+string(filepath.Separator)) {
		return "", fmt.Errorf("invalid id: %q", id)
	}
	return path, nil
}

func (v *Vault) getFileLock(path string) *sync.Mutex {
	mu, _ := v.locks.LoadOrStore(path, &sync.Mutex{})
	return mu.(*sync.Mutex)
}

var ErrNotFound = fmt.Errorf("not found")

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

	// Handle closing delimiter with either \n--- (Unix/Mac) or \r\n--- (Windows).
	// Search for \n--- first; this also matches \r\n--- since the \n is the second byte.
	// Then strip a trailing \r from frontmatter if present (from Windows line endings).
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

	// Strip trailing \r from frontmatter (Windows \r\n line endings).
	// This only removes a trailing carriage return from the entire frontmatter slice,
	// not from each line — so multiline YAML is not collapsed.
	if len(frontmatter) > 0 && frontmatter[len(frontmatter)-1] == '\r' {
		frontmatter = frontmatter[:len(frontmatter)-1]
	}

	body = bytes.TrimLeft(body, " \t\r\n")
	return frontmatter, body, nil
}

func (v *Vault) read(entityType, id string, dest interface{}) (body string, err error) {
	path, err := v.filePath(entityType, id)
	if err != nil {
		return "", err
	}
	return v.readFile(path, entityType, id, dest)
}

func (v *Vault) readFile(path, entityType, id string, dest interface{}) (body string, err error) {
	mu := v.getFileLock(path)
	mu.Lock()
	defer mu.Unlock()

	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return "", fmt.Errorf("%s %s: %w", entityType, id, ErrNotFound)
		}
		return "", fmt.Errorf("read %s: %w", path, err)
	}

	frontmatter, bodyBytes, err := splitFrontmatter(data)
	if err != nil {
		return "", fmt.Errorf("parse frontmatter %s: %w", path, err)
	}

	if err := yaml.Unmarshal(frontmatter, dest); err != nil {
		return "", fmt.Errorf("unmarshal %s: %w", path, err)
	}

	return string(bodyBytes), nil
}

func (v *Vault) write(entityType, id string, frontmatter interface{}, body string) error {
	path, err := v.filePath(entityType, id)
	if err != nil {
		return err
	}

	mu := v.getFileLock(path)
	mu.Lock()
	defer mu.Unlock()

	dir := filepath.Dir(path)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return fmt.Errorf("ensure dir %s: %w", dir, err)
	}

	fmBytes, err := yaml.Marshal(frontmatter)
	if err != nil {
		return fmt.Errorf("marshal %s: %w", id, err)
	}

	var buf bytes.Buffer
	buf.WriteString("---\n")
	buf.Write(fmBytes)
	buf.WriteString("---\n")
	if body != "" {
		buf.WriteString(body)
		if !strings.HasSuffix(body, "\n") {
			buf.WriteString("\n")
		}
	}

	tmpPath := path + ".tmp"
	if err := os.WriteFile(tmpPath, buf.Bytes(), 0644); err != nil {
		return fmt.Errorf("write temp %s: %w", tmpPath, err)
	}
	if err := os.Rename(tmpPath, path); err != nil {
		os.Remove(tmpPath)
		return fmt.Errorf("rename %s -> %s: %w", tmpPath, path, err)
	}

	return nil
}

func (v *Vault) listFiles(subdir string) ([]string, error) {
	dirPath := filepath.Join(v.root, subdir)
	entries, err := os.ReadDir(dirPath)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, fmt.Errorf("list %s: %w", dirPath, err)
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

// archiveFilePath returns the path to an archived entity file.
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

// readArchived reads an archived entity and unmarshals its frontmatter.
func (v *Vault) readArchived(entityType, id string, dest interface{}) (body string, err error) {
	path, err := v.archiveFilePath(entityType, id)
	if err != nil {
		return "", err
	}
	return v.readFile(path, entityType, id, dest)
}

// ListArchivedIDs returns the IDs of archived entities of the given type.
func (v *Vault) ListArchivedIDs(entityType string) ([]string, error) {
	dir, err := entityDir(entityType)
	if err != nil {
		return nil, err
	}
	subdir := filepath.Join(DirArchive, dir)
	return v.listFiles(subdir)
}

// ReadArchivedNote reads a note from the archive.
func (v *Vault) ReadArchivedNote(id string) (*model.Note, error) {
	var note model.Note
	body, err := v.readArchived(model.TypeNote, id, &note)
	if err != nil {
		return nil, err
	}
	note.Body = body
	return &note, nil
}

// ReadArchivedTask reads a task from the archive.
func (v *Vault) ReadArchivedTask(id string) (*model.Task, error) {
	var task model.Task
	body, err := v.readArchived(model.TypeTask, id, &task)
	if err != nil {
		return nil, err
	}
	task.Body = body
	return &task, nil
}

// ReadArchivedQuickTask reads a quick task from the archive.
func (v *Vault) ReadArchivedQuickTask(id string) (*model.QuickTask, error) {
	var qt model.QuickTask
	_, err := v.readArchived(model.TypeQuickTask, id, &qt)
	if err != nil {
		return nil, err
	}
	return &qt, nil
}

// ReadArchivedPerson reads a person from the archive.
func (v *Vault) ReadArchivedPerson(id string) (*model.Person, error) {
	var person model.Person
	body, err := v.readArchived(model.TypePerson, id, &person)
	if err != nil {
		return nil, err
	}
	person.Body = body
	return &person, nil
}

// RestoreArchive restores an archived entity back to its original location.
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

	// Remove from archive after successful restore
	archivePath, err := v.archiveFilePath(entityType, id)
	if err != nil {
		return fmt.Errorf("archive path: %w", err)
	}
	if err := os.Remove(archivePath); err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("remove archived %s: %w", archivePath, err)
	}

	return nil
}

// ResolveWikiLink finds an entity by its title or ID across all entity types.
// Returns the entity type, ID, and display title if found.
func (v *Vault) ResolveWikiLink(query string) (entityType string, id string, title string, err error) {
	notes, err := v.ListNotes()
	if err == nil {
		for _, n := range notes {
			if strings.EqualFold(n.Title, query) || n.ID == query {
				return model.TypeNote, n.ID, n.Title, nil
			}
		}
	}

	tasks, err := v.ListTasks()
	if err == nil {
		for _, t := range tasks {
			if strings.EqualFold(t.Title, query) || t.ID == query {
				return model.TypeTask, t.ID, t.Title, nil
			}
		}
	}

	qts, err := v.ListQuickTasks()
	if err == nil {
		for _, qt := range qts {
			if strings.EqualFold(qt.Title, query) || qt.ID == query {
				return model.TypeQuickTask, qt.ID, qt.Title, nil
			}
		}
	}

	people, err := v.ListPeople()
	if err == nil {
		for _, p := range people {
			if strings.EqualFold(p.Name, query) || p.ID == query {
				return model.TypePerson, p.ID, p.Name, nil
			}
		}
	}

	return "", "", "", fmt.Errorf("wikilink %q: %w", query, ErrNotFound)
}

func (v *Vault) ReadNote(id string) (*model.Note, error) {
	var note model.Note
	body, err := v.read(model.TypeNote, id, &note)
	if err != nil {
		return nil, err
	}
	note.Body = body
	return &note, nil
}

func (v *Vault) ReadTask(id string) (*model.Task, error) {
	var task model.Task
	body, err := v.read(model.TypeTask, id, &task)
	if err != nil {
		return nil, err
	}
	task.Body = body
	return &task, nil
}

func (v *Vault) ReadQuickTask(id string) (*model.QuickTask, error) {
	var qt model.QuickTask
	_, err := v.read(model.TypeQuickTask, id, &qt)
	if err != nil {
		return nil, err
	}
	return &qt, nil
}

func (v *Vault) ReadPerson(id string) (*model.Person, error) {
	var person model.Person
	body, err := v.read(model.TypePerson, id, &person)
	if err != nil {
		return nil, err
	}
	person.Body = body
	return &person, nil
}

func (v *Vault) ListNotes() ([]*model.Note, error) {
	files, err := v.listFiles(DirNotes)
	if err != nil {
		return nil, err
	}
	var notes []*model.Note
	for _, id := range files {
		n, err := v.ReadNote(id)
		if err != nil {
			continue
		}
		notes = append(notes, n)
	}
	return notes, nil
}

func (v *Vault) ListTasks() ([]*model.Task, error) {
	files, err := v.listFiles(DirTasks)
	if err != nil {
		return nil, err
	}
	var tasks []*model.Task
	for _, id := range files {
		t, err := v.ReadTask(id)
		if err != nil {
			continue
		}
		// Filter out old occurrence .md files (non-template tasks with a parent)
		if !t.IsTemplate && t.ParentID != "" {
			continue
		}
		tasks = append(tasks, t)
	}
	return tasks, nil
}

// ListTemplates returns only template tasks (tasks with is_template=true).
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

// ListTasksByDate returns tasks that occur on the given date.
// This includes standalone tasks matching by due/range date AND dynamically
// computed occurrences from templates whose recurrence pattern covers the date.
// date should be in "YYYY-MM-DD" format.
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

	// 1. Standalone tasks (non-template, no parent) whose due date or range covers the date
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

	// 2. Templates - compute dynamic occurrences (filter from same all slice)
	for _, t := range all {
		if t.IsTemplate && DateMatchesRecurrence(parsedDate, t) {
			occ := ComputeDynamicOccurrence(t, date)
			if occ != nil {
				// Apply any per-occurrence overrides
				if override, _ := v.ReadOccurrenceOverride(t.ID, date); override != nil {
					ApplyOccurrenceOverride(occ, override)
				}
				matches = append(matches, occ)
			}
		}
	}

	return matches, nil
}

// ListTasksByParent dynamically computes occurrences for a template task
// from its recurrence pattern and date range, if defined.
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

	// For indefinite recurrence (no end date), return empty to avoid infinite generation
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

func (v *Vault) ListQuickTasks() ([]*model.QuickTask, error) {
	files, err := v.listFiles(DirQuickTasks)
	if err != nil {
		return nil, err
	}
	var qts []*model.QuickTask
	for _, id := range files {
		qt, err := v.ReadQuickTask(id)
		if err != nil {
			continue
		}
		qts = append(qts, qt)
	}
	return qts, nil
}

func (v *Vault) ListPeople() ([]*model.Person, error) {
	files, err := v.listFiles(DirPeople)
	if err != nil {
		return nil, err
	}
	var people []*model.Person
	for _, id := range files {
		p, err := v.ReadPerson(id)
		if err != nil {
			continue
		}
		people = append(people, p)
	}
	return people, nil
}

func (v *Vault) WriteNote(note *model.Note) error {
	return v.write(model.TypeNote, note.ID, note, note.Body)
}

func (v *Vault) WriteTask(task *model.Task) error {
	return v.write(model.TypeTask, task.ID, task, task.Body)
}

func (v *Vault) WriteQuickTask(qt *model.QuickTask) error {
	return v.write(model.TypeQuickTask, qt.ID, qt, "")
}

func (v *Vault) WritePerson(person *model.Person) error {
	return v.write(model.TypePerson, person.ID, person, person.Body)
}

func (v *Vault) Delete(entityType, id string) error {
	path, err := v.filePath(entityType, id)
	if err != nil {
		return err
	}

	mu := v.getFileLock(path)
	mu.Lock()
	defer mu.Unlock()

	// Read the file content
	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return fmt.Errorf("%s %s: %w", entityType, id, ErrNotFound)
		}
		return fmt.Errorf("read %s: %w", path, err)
	}

	// Archive FIRST (before deleting original) to prevent data loss on crash.
	// If archiving fails, the original file is preserved.
	dir, archiveErr := entityDir(entityType)
	if archiveErr == nil {
		archiveDir := filepath.Join(v.root, DirArchive, dir)
		if err := os.MkdirAll(archiveDir, 0755); err != nil {
			archiveErr = fmt.Errorf("create archive dir: %w", err)
		} else {
			archivePath := filepath.Join(archiveDir, id+".md")
			tmpPath := archivePath + ".tmp"
			if err := os.WriteFile(tmpPath, data, 0644); err != nil {
				os.Remove(tmpPath)
				archiveErr = fmt.Errorf("write archive temp: %w", err)
			} else if err := os.Rename(tmpPath, archivePath); err != nil {
				os.Remove(tmpPath)
				archiveErr = fmt.Errorf("rename archive: %w", err)
			}
		}
	}

	// If archiving failed, return error without deleting the original file.
	if archiveErr != nil {
		select {
		case v.lockCleanup <- path:
		default:
		}
		return fmt.Errorf("archive before delete %s: %w", id, archiveErr)
	}

	// Archive succeeded, now delete original.
	if err := os.Remove(path); err != nil {
		select {
		case v.lockCleanup <- path:
		default:
		}
		return fmt.Errorf("delete %s: %w", path, err)
	}

	// Clean up lock entry for deleted file.
	select {
	case v.lockCleanup <- path:
	default:
	}

	return nil
}

func (v *Vault) Exists(entityType, id string) (bool, error) {
	path, err := v.filePath(entityType, id)
	if err != nil {
		return false, err
	}
	_, err = os.Stat(path)
	if err == nil {
		return true, nil
	}
	if os.IsNotExist(err) {
		return false, nil
	}
	return false, fmt.Errorf("stat %s: %w", path, err)
}

// --------------------------------------------------------------------------
// Dynamic occurrence computation helpers
// --------------------------------------------------------------------------

// DateMatchesRecurrence checks whether a given date falls within a template's
// recurrence pattern (daily/weekly/monthly/yearly with interval and days_of_week).
func DateMatchesRecurrence(date time.Time, template *model.Task) bool {
	if template.Recurrence == nil || template.StartDate == nil {
		return false
	}

	dateStart := date.Truncate(24 * time.Hour)
	startDate := template.StartDate.Truncate(24 * time.Hour)

	// Check date is within template's date range
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
			// Check interval: only match every Nth week
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

// ComputeDynamicOccurrence creates a virtual Task object from a template for a specific date.
// No file is written to disk; the occurrence is computed on-the-fly.
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

// GenerateDatesInRange returns all date strings from start to end (inclusive)
// matching the recurrence pattern.
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

// resetSubtasks creates a new subtask slice with fresh IDs from a template's subtasks.
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

// OccurrenceOverride stores per-occurrence field overrides.
type OccurrenceOverride struct {
	Status  string           `json:"status,omitempty"`
	Title   string           `json:"title,omitempty"`
	Body    string           `json:"body,omitempty"`
	Subtasks []model.Subtask `json:"subtasks,omitempty"`
}

// occurrenceOverridePath returns the file path for an occurrence override.
func (v *Vault) occurrenceOverridePath(parentID, date string) string {
	return filepath.Join(v.root, DirOverrides, parentID+"_"+date+".json")
}

// WriteOccurrenceOverride saves field overrides for a specific occurrence.
func (v *Vault) WriteOccurrenceOverride(parentID, date string, override *OccurrenceOverride) error {
	path := v.occurrenceOverridePath(parentID, date)
	data, err := json.MarshalIndent(override, "", "  ")
	if err != nil {
		return fmt.Errorf("marshal override: %w", err)
	}
	return os.WriteFile(path, data, 0644)
}

// ReadOccurrenceOverride reads field overrides for a specific occurrence.
// Returns nil, nil if no override exists.
func (v *Vault) ReadOccurrenceOverride(parentID, date string) (*OccurrenceOverride, error) {
	path := v.occurrenceOverridePath(parentID, date)
	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, fmt.Errorf("read override: %w", err)
	}
	var override OccurrenceOverride
	if err := json.Unmarshal(data, &override); err != nil {
		return nil, fmt.Errorf("unmarshal override: %w", err)
	}
	return &override, nil
}

// ApplyOccurrenceOverride applies stored overrides to a computed occurrence.
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
