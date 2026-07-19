package vault

import (
	"bytes"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"

	"github.com/koreicc/garou-second-brain/backend/internal/model"
	"gopkg.in/yaml.v3"
)

type Vault struct {
	root  string
	locks sync.Map
}

const (
	DirNotes      = "notes"
	DirTasks      = "tasks"
	DirQuickTasks = "quick-tasks"
	DirPeople     = "people"
	DirArchive    = "archive"
)

func New(root string) *Vault {
	return &Vault{root: root}
}

func (v *Vault) Init() error {
	dirs := []string{DirNotes, DirTasks, DirQuickTasks, DirPeople, DirArchive}
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

	body = bytes.TrimLeft(body, " \t\r\n")
	return frontmatter, body, nil
}

func (v *Vault) read(entityType, id string, dest interface{}) (body string, err error) {
	path, err := v.filePath(entityType, id)
	if err != nil {
		return "", err
	}

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
		tasks = append(tasks, t)
	}
	return tasks, nil
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

	if err := os.Remove(path); err != nil {
		if os.IsNotExist(err) {
			return fmt.Errorf("%s %s: %w", entityType, id, ErrNotFound)
		}
		return fmt.Errorf("delete %s: %w", path, err)
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
