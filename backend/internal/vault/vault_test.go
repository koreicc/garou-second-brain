package vault

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/google/uuid"
	"github.com/koreicc/garou-second-brain/backend/internal/model"
)

func setupTestVault(t *testing.T) *Vault {
	t.Helper()
	dir, err := os.MkdirTemp("", "vault-test-*")
	if err != nil {
		t.Fatalf("create temp dir: %v", err)
	}
	t.Cleanup(func() { os.RemoveAll(dir) })

	v := New(dir)
	if err := v.Init(); err != nil {
		t.Fatalf("init vault: %v", err)
	}
	return v
}

func TestInitCreatesDirectories(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	expectedDirs := []string{DirNotes, DirTasks, DirQuickTasks, DirPeople, DirArchive}
	for _, d := range expectedDirs {
		path := filepath.Join(v.root, d)
		info, err := os.Stat(path)
		if err != nil {
			t.Fatalf("expected dir %s: %v", d, err)
		}
		if !info.IsDir() {
			t.Fatalf("%s is not a directory", d)
		}
	}
}

func TestWriteAndReadNote(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	note := model.NewNote(uuid.NewString(), "Test Note")
	note.Body = "This is the body content."

	err := v.WriteNote(note)
	if err != nil {
		t.Fatalf("WriteNote: %v", err)
	}

	got, err := v.ReadNote(note.ID)
	if err != nil {
		t.Fatalf("ReadNote: %v", err)
	}

	if got.ID != note.ID {
		t.Fatalf("ID = %q, want %q", got.ID, note.ID)
	}
	if got.Title != note.Title {
		t.Fatalf("Title = %q, want %q", got.Title, note.Title)
	}
	// Body gets a trailing newline added by the vault writer
	expectedBody := note.Body
	if !strings.HasSuffix(expectedBody, "\n") {
		expectedBody += "\n"
	}
	if got.Body != expectedBody {
		t.Fatalf("Body = %q, want %q", got.Body, expectedBody)
	}
	if got.Status != note.Status {
		t.Fatalf("Status = %q, want %q", got.Status, note.Status)
	}
	if got.Type != model.TypeNote {
		t.Fatalf("Type = %q, want %q", got.Type, model.TypeNote)
	}
}

func TestWriteAndReadTask(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	now := model.NewTask(uuid.NewString(), "Test Task")
	now.Body = "Task body content."
	now.Location = "Home"
	now.Icon = "edit-note"

	err := v.WriteTask(now)
	if err != nil {
		t.Fatalf("WriteTask: %v", err)
	}

	got, err := v.ReadTask(now.ID)
	if err != nil {
		t.Fatalf("ReadTask: %v", err)
	}

	if got.ID != now.ID {
		t.Fatalf("ID = %q, want %q", got.ID, now.ID)
	}
	if got.Title != now.Title {
		t.Fatalf("Title = %q, want %q", got.Title, now.Title)
	}
	if got.Location != now.Location {
		t.Fatalf("Location = %q, want %q", got.Location, now.Location)
	}
	if got.Icon != now.Icon {
		t.Fatalf("Icon = %q, want %q", got.Icon, now.Icon)
	}
	// Body gets a trailing newline added by the vault writer
	expectedBody := now.Body
	if !strings.HasSuffix(expectedBody, "\n") {
		expectedBody += "\n"
	}
	if got.Body != expectedBody {
		t.Fatalf("Body = %q, want %q", got.Body, expectedBody)
	}
}

func TestWriteAndReadQuickTask(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	qt := model.NewQuickTask(uuid.NewString(), "Buy milk")

	err := v.WriteQuickTask(qt)
	if err != nil {
		t.Fatalf("WriteQuickTask: %v", err)
	}

	got, err := v.ReadQuickTask(qt.ID)
	if err != nil {
		t.Fatalf("ReadQuickTask: %v", err)
	}

	if got.ID != qt.ID {
		t.Fatalf("ID = %q, want %q", got.ID, qt.ID)
	}
	if got.Title != qt.Title {
		t.Fatalf("Title = %q, want %q", got.Title, qt.Title)
	}
	if got.Status != model.QuickTaskStatusPending {
		t.Fatalf("Status = %q, want %q", got.Status, model.QuickTaskStatusPending)
	}
}

func TestWriteAndReadPerson(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	person := model.NewPerson(uuid.NewString(), "John Doe")
	person.Notes = "Met at conference."
	person.Contacts = []model.Contact{
		{Type: "phone", Value: "+90 555 123 4567", Label: "Personal"},
	}
	person.SocialLinks = []model.SocialLink{
		{Platform: "github", URL: "https://github.com/johndoe"},
	}
	person.Body = "Additional body content."

	err := v.WritePerson(person)
	if err != nil {
		t.Fatalf("WritePerson: %v", err)
	}

	got, err := v.ReadPerson(person.ID)
	if err != nil {
		t.Fatalf("ReadPerson: %v", err)
	}

	if got.ID != person.ID {
		t.Fatalf("ID = %q, want %q", got.ID, person.ID)
	}
	if got.Name != person.Name {
		t.Fatalf("Name = %q, want %q", got.Name, person.Name)
	}
	if got.Notes != person.Notes {
		t.Fatalf("Notes = %q, want %q", got.Notes, person.Notes)
	}
	// Body gets a trailing newline added by the vault writer
	expectedBody := person.Body
	if !strings.HasSuffix(expectedBody, "\n") {
		expectedBody += "\n"
	}
	if got.Body != expectedBody {
		t.Fatalf("Body = %q, want %q", got.Body, expectedBody)
	}
	if len(got.Contacts) != 1 {
		t.Fatalf("len(Contacts) = %d, want 1", len(got.Contacts))
	}
	if got.Contacts[0].Value != "+90 555 123 4567" {
		t.Fatalf("Contact.Value = %q, want %q", got.Contacts[0].Value, "+90 555 123 4567")
	}
	if len(got.SocialLinks) != 1 {
		t.Fatalf("len(SocialLinks) = %d, want 1", len(got.SocialLinks))
	}
}

func TestListNotes(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	notes := []*model.Note{
		model.NewNote(uuid.NewString(), "Note 1"),
		model.NewNote(uuid.NewString(), "Note 2"),
		model.NewNote(uuid.NewString(), "Note 3"),
	}

	for _, n := range notes {
		if err := v.WriteNote(n); err != nil {
			t.Fatalf("WriteNote: %v", err)
		}
	}

	got, err := v.ListNotes()
	if err != nil {
		t.Fatalf("ListNotes: %v", err)
	}

	if len(got) != len(notes) {
		t.Fatalf("len(ListNotes) = %d, want %d", len(got), len(notes))
	}

	ids := make(map[string]bool)
	for _, n := range got {
		ids[n.ID] = true
	}
	for _, n := range notes {
		if !ids[n.ID] {
			t.Fatalf("note %s not found in list", n.ID)
		}
	}
}

func TestListTasks(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	tasks := []*model.Task{
		model.NewTask(uuid.NewString(), "Task 1"),
		model.NewTask(uuid.NewString(), "Task 2"),
	}

	for _, tk := range tasks {
		if err := v.WriteTask(tk); err != nil {
			t.Fatalf("WriteTask: %v", err)
		}
	}

	got, err := v.ListTasks()
	if err != nil {
		t.Fatalf("ListTasks: %v", err)
	}

	if len(got) != len(tasks) {
		t.Fatalf("len(ListTasks) = %d, want %d", len(got), len(tasks))
	}
}

func TestListQuickTasks(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	qts := []*model.QuickTask{
		model.NewQuickTask(uuid.NewString(), "QT 1"),
		model.NewQuickTask(uuid.NewString(), "QT 2"),
		model.NewQuickTask(uuid.NewString(), "QT 3"),
		model.NewQuickTask(uuid.NewString(), "QT 4"),
	}

	for _, q := range qts {
		if err := v.WriteQuickTask(q); err != nil {
			t.Fatalf("WriteQuickTask: %v", err)
		}
	}

	got, err := v.ListQuickTasks()
	if err != nil {
		t.Fatalf("ListQuickTasks: %v", err)
	}

	if len(got) != len(qts) {
		t.Fatalf("len(ListQuickTasks) = %d, want %d", len(got), len(qts))
	}
}

func TestListPeople(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	people := []*model.Person{
		model.NewPerson(uuid.NewString(), "Alice"),
		model.NewPerson(uuid.NewString(), "Bob"),
	}

	for _, p := range people {
		if err := v.WritePerson(p); err != nil {
			t.Fatalf("WritePerson: %v", err)
		}
	}

	got, err := v.ListPeople()
	if err != nil {
		t.Fatalf("ListPeople: %v", err)
	}

	if len(got) != len(people) {
		t.Fatalf("len(ListPeople) = %d, want %d", len(got), len(people))
	}
}

func TestDeleteNote(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	note := model.NewNote(uuid.NewString(), "To Delete")
	if err := v.WriteNote(note); err != nil {
		t.Fatalf("WriteNote: %v", err)
	}

	err := v.Delete(model.TypeNote, note.ID)
	if err != nil {
		t.Fatalf("Delete: %v", err)
	}

	_, err = v.ReadNote(note.ID)
	if err == nil {
		t.Fatal("expected error reading deleted note, got nil")
	}
	if !strings.Contains(err.Error(), "not found") {
		t.Fatalf("expected 'not found' error, got %q", err.Error())
	}
}

func TestDeleteNotFound(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	err := v.Delete(model.TypeNote, "nonexistent-id")
	if err == nil {
		t.Fatal("expected error deleting nonexistent note, got nil")
	}
	if !strings.Contains(err.Error(), "not found") {
		t.Fatalf("expected 'not found' error, got %q", err.Error())
	}
}

func TestReadNotFound(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	_, err := v.ReadNote("nonexistent-id")
	if err == nil {
		t.Fatal("expected error, got nil")
	}
	if !strings.Contains(err.Error(), "not found") {
		t.Fatalf("expected 'not found' error, got %q", err.Error())
	}
}

func TestExists(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	note := model.NewNote(uuid.NewString(), "Existing")
	if err := v.WriteNote(note); err != nil {
		t.Fatalf("WriteNote: %v", err)
	}

	exists, err := v.Exists(model.TypeNote, note.ID)
	if err != nil {
		t.Fatalf("Exists: %v", err)
	}
	if !exists {
		t.Fatal("expected note to exist")
	}

	exists, err = v.Exists(model.TypeNote, "nonexistent-id")
	if err != nil {
		t.Fatalf("Exists: %v", err)
	}
	if exists {
		t.Fatal("expected note to not exist")
	}
}

func TestUpdateNote(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	note := model.NewNote(uuid.NewString(), "Original Title")
	note.Body = "Original body."
	if err := v.WriteNote(note); err != nil {
		t.Fatalf("WriteNote: %v", err)
	}

	note.Title = "Updated Title"
	note.Body = "Updated body."
	if err := v.WriteNote(note); err != nil {
		t.Fatalf("WriteNote (update): %v", err)
	}

	got, err := v.ReadNote(note.ID)
	if err != nil {
		t.Fatalf("ReadNote: %v", err)
	}

	if got.Title != "Updated Title" {
		t.Fatalf("Title = %q, want %q", got.Title, "Updated Title")
	}
	// Body gets a trailing newline added by the vault writer
	if got.Body != "Updated body.\n" {
		t.Fatalf("Body = %q, want %q", got.Body, "Updated body.\n")
	}
}

func TestWritePreservesFrontmatterTypes(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	note := model.NewNote(uuid.NewString(), "Type Test")
	note.Tags = []string{"tag1", "tag2"}
	note.Links = []string{"link-uuid-1"}
	if err := v.WriteNote(note); err != nil {
		t.Fatalf("WriteNote: %v", err)
	}

	got, err := v.ReadNote(note.ID)
	if err != nil {
		t.Fatalf("ReadNote: %v", err)
	}

	if len(got.Tags) != 2 {
		t.Fatalf("len(Tags) = %d, want 2", len(got.Tags))
	}
	if got.Tags[0] != "tag1" {
		t.Fatalf("Tags[0] = %q, want %q", got.Tags[0], "tag1")
	}
	if len(got.Links) != 1 {
		t.Fatalf("len(Links) = %d, want 1", len(got.Links))
	}
}

func TestEmptyVaultLists(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	notes, err := v.ListNotes()
	if err != nil {
		t.Fatalf("ListNotes on empty vault: %v", err)
	}
	if len(notes) != 0 {
		t.Fatalf("expected 0 notes, got %d", len(notes))
	}

	tasks, err := v.ListTasks()
	if err != nil {
		t.Fatalf("ListTasks on empty vault: %v", err)
	}
	if len(tasks) != 0 {
		t.Fatalf("expected 0 tasks, got %d", len(tasks))
	}

	qts, err := v.ListQuickTasks()
	if err != nil {
		t.Fatalf("ListQuickTasks on empty vault: %v", err)
	}
	if len(qts) != 0 {
		t.Fatalf("expected 0 quick tasks, got %d", len(qts))
	}

	people, err := v.ListPeople()
	if err != nil {
		t.Fatalf("ListPeople on empty vault: %v", err)
	}
	if len(people) != 0 {
		t.Fatalf("expected 0 people, got %d", len(people))
	}
}

func TestFileWrittenToDisk(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	note := model.NewNote(uuid.NewString(), "Disk Check")
	note.Body = "Check file on disk."
	if err := v.WriteNote(note); err != nil {
		t.Fatalf("WriteNote: %v", err)
	}

	path := filepath.Join(v.root, DirNotes, note.ID+".md")
	data, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("ReadFile: %v", err)
	}

	content := string(data)
	if !strings.HasPrefix(content, "---\n") {
		t.Fatal("file should start with ---")
	}
	if !strings.Contains(content, "title: Disk Check") {
		t.Fatal("file should contain title frontmatter")
	}
	if !strings.Contains(content, "Check file on disk.") {
		t.Fatal("file should contain body text")
	}
}

func TestWrongTypeReturnsError(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	// Write a note, then try to read it as a task
	note := model.NewNote(uuid.NewString(), "Wrong Type")
	if err := v.WriteNote(note); err != nil {
		t.Fatalf("WriteNote: %v", err)
	}

	// Reading as note should work
	_, err := v.ReadNote(note.ID)
	if err != nil {
		t.Fatalf("ReadNote: %v", err)
	}

	// Reading as task should fail because the file won't exist under tasks dir
	_, err = v.ReadTask(note.ID)
	if err == nil {
		t.Fatal("expected error reading note as task")
	}
	if !strings.Contains(err.Error(), "not found") {
		t.Fatalf("expected 'not found' error, got %q", err.Error())
	}
}

func TestConcurrentReadWrite(t *testing.T) {
	v := setupTestVault(t)

	note := model.NewNote(uuid.NewString(), "Concurrent")
	if err := v.WriteNote(note); err != nil {
		t.Fatalf("WriteNote: %v", err)
	}

	// Read and write concurrently to check locking
	done := make(chan bool, 2)

	go func() {
		for i := 0; i < 10; i++ {
			n := model.NewNote(uuid.NewString(), "Concurrent Write")
			_ = v.WriteNote(n)
		}
		done <- true
	}()

	go func() {
		for i := 0; i < 10; i++ {
			_, _ = v.ReadNote(note.ID)
		}
		done <- true
	}()

	<-done
	<-done

	// Verify the original note is still readable
	got, err := v.ReadNote(note.ID)
	if err != nil {
		t.Fatalf("ReadNote after concurrent access: %v", err)
	}
	if got.Title != "Concurrent" {
		t.Fatalf("Title = %q, want %q", got.Title, "Concurrent")
	}
}

func TestEntityDir(t *testing.T) {
	t.Parallel()

	tests := []struct {
		entityType string
		want       string
		wantErr    bool
	}{
		{model.TypeNote, DirNotes, false},
		{model.TypeTask, DirTasks, false},
		{model.TypeQuickTask, DirQuickTasks, false},
		{model.TypePerson, DirPeople, false},
		{"unknown", "", true},
	}

	for _, tc := range tests {
		tc := tc
		t.Run(tc.entityType, func(t *testing.T) {
			t.Parallel()
			got, err := entityDir(tc.entityType)
			if tc.wantErr {
				if err == nil {
					t.Fatal("expected error, got nil")
				}
				return
			}
			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
			if got != tc.want {
				t.Fatalf("entityDir(%q) = %q, want %q", tc.entityType, got, tc.want)
			}
		})
	}
}

func TestFileIgnoresNonMdFiles(t *testing.T) {
	t.Parallel()
	v := setupTestVault(t)

	// Write a note
	note := model.NewNote(uuid.NewString(), "Only Note")
	if err := v.WriteNote(note); err != nil {
		t.Fatalf("WriteNote: %v", err)
	}

	// Create a non-.md file in the notes directory
	path := filepath.Join(v.root, DirNotes, "readme.txt")
	if err := os.WriteFile(path, []byte("not a note"), 0644); err != nil {
		t.Fatalf("WriteFile: %v", err)
	}

	// List should return only the .md file
	notes, err := v.ListNotes()
	if err != nil {
		t.Fatalf("ListNotes: %v", err)
	}
	if len(notes) != 1 {
		t.Fatalf("expected 1 note, got %d", len(notes))
	}
	if notes[0].ID != note.ID {
		t.Fatalf("unexpected note ID: %q", notes[0].ID)
	}
}
