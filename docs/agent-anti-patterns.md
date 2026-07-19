# AI Agent Anti-Patterns -- Go Backend, API, and Data Layer

A reference of patterns that AI coding assistants (Claude Code, Cursor, Codex, Copilot,
Gemini, etc.) consistently generate incorrectly in Go backend code, REST API design, and
Markdown/YAML data layer development.

Root cause: AI models are trained on code corpuses that lag 12-24 months behind current
best practices. Tutorials and pre-2023 Stack Overflow answers are overrepresented in
training data. Agents lack project context and default to the most common pattern, not
the most correct one.

---

## Quick Reference Table

| # | Banned (WRONG) | Correct (RIGHT) | Category |
|---|---|---|---|
| 1 | `_ = doThing()` to silence errors | Always check and wrap errors | Error handling |
| 2 | `return err` (bare, no context) | `return fmt.Errorf("context: %w", err)` | Error wrapping |
| 3 | `err.Error() == "not found"` string compare | `errors.Is(err, ErrNotFound)` | Error comparison |
| 4 | `go doWork()` with no exit path | `errgroup.Group` with context cancellation | Goroutine lifecycle |
| 5 | `ctx := context.Background()` in handler | `ctx := r.Context()` | Context propagation |
| 6 | `var db *sql.DB` as package global | Dependencies on `Server` struct, constructor injection | Global state |
| 7 | `sync.Mutex` embedded in struct | Named field `mu sync.Mutex` (private) | Mutex embedding |
| 8 | Named return values `(n int, err error)` | Unnamed returns except for defer-modified errors | Function signatures |
| 9 | Sorting/filtering in handler inline | Pre-processed in service layer before handler | API architecture |
| 10 | Watching individual files with fsnotify | Watch parent directory, filter by Event.Name | File watching |
| 11 | No `defer watcher.Close()` after NewWatcher | `defer watcher.Close()` immediately after creation | Resource cleanup |
| 12 | Writing to watched file in same process | Ignore own writes or debounce events | Event loops |
| 13 | `os.ReadFile` + manual YAML parse | Proper frontmatter-aware parser (goldmark-frontmatter) | Markdown parsing |
| 14 | No file locking on vault writes | `syscall.Flock` or `filemutex` for concurrent access | File locking |
| 15 | No idempotency on POST endpoints | `Idempotency-Key` header support, 24h cache | API design |
| 16 | Generic error messages (`"bad request"`) | Specific, actionable error details + suggestions | Error responses |
| 17 | `init()` for setup logic | Explicit `main()` wiring, constructor functions | Initialization |
| 18 | No `go test -race` in CI | Always run `go test -race ./...` | Testing |
| 19 | Interface defined in producer package | Interface defined at consumer, 1-3 methods | Interface design |
| 20 | No rate limit headers on responses | `X-RateLimit-Remaining`, `Retry-After` on every response | Rate limiting |
| 21 | `time.After` in hot loop | `time.NewTimer` + `Reset` for long-running loops | Timer efficiency |
| 22 | `wg.Add()` called inside goroutine | `wg.Add()` before launching goroutine | WaitGroup misuse |
| 23 | `panic` for regular error cases | `return error` from functions | Error philosophy |
| 24 | Endpoint name inconsistency (`getUser`, `fetch-user`, `/users/GET`) | Consistent plural nouns, standard HTTP verbs | API naming |
| 25 | No `Content-Type` on responses | Always set `Content-Type: application/json` | HTTP responses |

---

## Category 1: Error Handling

The single most common source of un-idiomatic AI-generated Go code.

### 1.1 Ignoring Errors with Blank Identifier

```go
// WRONG -- error silently discarded
result, _ := db.QueryContext(ctx, "SELECT ...")
json.NewDecoder(r.Body).Decode(&req) // missing error check

// RIGHT -- every error checked
result, err := db.QueryContext(ctx, "SELECT ...")
if err != nil {
    return fmt.Errorf("query users: %w", err)
}

err = json.NewDecoder(r.Body).Decode(&req)
if err != nil {
    http.Error(w, "invalid JSON body", http.StatusBadRequest)
    return
}
```

### 1.2 Bare Error Returns Without Context

```go
// WRONG -- loses all context, caller can't identify origin
func GetUser(id int) (*User, error) {
    row, err := db.Query("SELECT ...", id)
    if err != nil {
        return nil, err  // no context added
    }
    return scanUser(row)
}

// RIGHT -- wrap with context
func GetUser(ctx context.Context, id int) (*User, error) {
    row, err := db.QueryContext(ctx, "SELECT ...", id)
    if err != nil {
        return nil, fmt.Errorf("get user %d: %w", id, err)
    }
    return scanUser(row)
}
```

### 1.3 String Comparison Instead of errors.Is / errors.As

```go
// WRONG -- breaks if error message changes, fragile
if err.Error() == "not found" { ... }
if strings.Contains(err.Error(), "timeout") { ... }

// RIGHT -- use sentinel errors and type checking
if errors.Is(err, ErrUserNotFound) { ... }

var timeoutErr *TimeoutError
if errors.As(err, &timeoutErr) { ... }
```

### 1.4 panic for Recoverable Errors

```go
// WRONG -- panic should be reserved for truly impossible states
func mustParse(data string) *Config {
    cfg, err := parseConfig(data)
    if err != nil {
        panic(err) // a bad config is not "impossible"
    }
    return cfg
}

// RIGHT -- return error, let caller decide
func parseConfig(data string) (*Config, error) {
    cfg, err := parseConfig(data)
    if err != nil {
        return nil, fmt.Errorf("parse config: %w", err)
    }
    return cfg, nil
}
```

### 1.5 No Error Check on Deferred Close

```go
// WRONG -- close error silently lost
f, _ := os.Open(path)
defer f.Close()

// RIGHT -- check close error for meaningful resources
f, err := os.Open(path)
if err != nil { return err }
defer func() {
    if cerr := f.Close(); cerr != nil {
        log.Printf("error closing file: %v", cerr)
    }
}()
```

**Why agents get it wrong:** Pre-2019 Go code and simplified tutorial examples routinely
use `_` to discard errors for brevity. Training data over-represents these simplified
examples. The `%w` wrapping convention (Go 1.13, 2019) is relatively recent in training
corpus terms.

---

## Category 2: Goroutines and Concurrency

### 2.1 Fire-and-Forget Goroutines (No Exit Path)

```go
// WRONG -- goroutine has no way to exit, leaks forever
func (s *Server) StartWorkers() {
    go func() {
        for {
            processItem() // blocks forever, no cancellation
        }
    }()
}

// RIGHT -- errgroup with context cancellation
func (s *Server) StartWorkers(ctx context.Context) error {
    g, gctx := errgroup.WithContext(ctx)
    g.Go(func() error {
        for {
            select {
            case <-gctx.Done():
                return gctx.Err()
            default:
                if err := processItem(gctx); err != nil {
                    return err
                }
            }
        }
    })
    return g.Wait()
}
```

### 2.2 Missing ctx.Done() in select

```go
// WRONG -- select blocks forever even after cancellation
select {
case msg := <-ch:
    handle(msg)
case err := <-errCh:
    return err
}

// RIGHT -- always include ctx.Done() in select
select {
case msg := <-ch:
    handle(msg)
case err := <-errCh:
    return err
case <-ctx.Done():
    return ctx.Err()
}
```

### 2.3 wg.Add() Inside Goroutine

```go
// WRONG -- race condition, Wait() may return before all goroutines add
var wg sync.WaitGroup
for _, item := range items {
    go func() {
        wg.Add(1) // Add called inside goroutine
        defer wg.Done()
        process(item)
    }()
}
wg.Wait()

// RIGHT -- Add before launching goroutine
var wg sync.WaitGroup
for _, item := range items {
    wg.Add(1) // Add before goroutine starts
    go func() {
        defer wg.Done()
        process(item)
    }()
}
wg.Wait()
```

### 2.4 Unbounded Goroutine Spawning

```go
// WRONG -- no limit on concurrent goroutines, OOM risk
for _, item := range items {
    go process(item) // spawns n goroutines immediately
}

// RIGHT -- limit concurrency with errgroup.SetLimit
g, ctx := errgroup.WithContext(ctx)
g.SetLimit(10) // max 10 concurrent
for _, item := range items {
    item := item
    g.Go(func() error {
        return process(ctx, item)
    })
}
if err := g.Wait(); err != nil { return err }
```

### 2.5 Channel Direction Omitted

```go
// WRONG -- bidirectional channel where send-only is clearer
func worker(ch chan Item) { ... }

// RIGHT -- specify channel direction for clarity and safety
func worker(ch <-chan Item) { ... }  // receives only
func producer(ch chan<- Item) { ... } // sends only
```

### 2.6 time.After in Hot Loop

```go
// WRONG -- allocates a new timer every iteration
for {
    select {
    case <-time.After(5 * time.Second):
        doPeriodicWork()
    case <-ctx.Done():
        return
    }
}

// RIGHT -- reuse timer
timer := time.NewTimer(5 * time.Second)
defer timer.Stop()
for {
    timer.Reset(5 * time.Second)
    select {
    case <-timer.C:
        doPeriodicWork()
    case <-ctx.Done():
        return
    }
}
```

**Why agents get it wrong:** Concurrency patterns are harder for LLMs because correct
code requires reasoning about runtime behavior (liveness, deadlock, leak) that isn't
visible in the static text. Training examples often omit error paths and cancellation
handling for brevity.

---

## Category 3: Context Propagation

### 3.1 context.Background() Inside HTTP Handlers

```go
// WRONG -- loses request cancellation, deadlines, and tracing context
func (s *Server) handleGetUser(w http.ResponseWriter, r *http.Request) {
    ctx := context.Background() // WRONG! Request has its own context
    user, err := s.db.GetUser(ctx, id)
}

// RIGHT -- use request context
func (s *Server) handleGetUser(w http.ResponseWriter, r *http.Request) {
    ctx := r.Context()
    user, err := s.db.GetUser(ctx, id)
}
```

### 3.2 Context Missing as First Parameter

```go
// WRONG -- no context parameter, can't propagate cancellation
func GetUser(id int) (*User, error) {
    return db.Query("SELECT ...", id) // can't be cancelled
}

// RIGHT -- context is first parameter
func GetUser(ctx context.Context, id int) (*User, error) {
    return db.QueryContext(ctx, "SELECT ...", id)
}
```

### 3.3 Context Stored on Struct

```go
// WRONG -- context stored on struct, wrong scope
type Service struct {
    ctx context.Context // should not be stored
    db  *sql.DB
}

// RIGHT -- context passed through function parameters
type Service struct {
    db *sql.DB
}
func (s *Service) GetUser(ctx context.Context, id int) (*User, error) { ... }
```

**Why agents get it wrong:** `context.Context` became standard in Go 1.7 (2016), but
pre-1.7 training examples don't use it. Storing context on a struct was a common
anti-pattern in early Go adoption that still appears in some tutorials.

---

## Category 4: HTTP Handlers and Server Design

### 4.1 Global State Instead of Dependency Injection

```go
// WRONG -- package-level globals, untestable
var db *sql.DB

func init() {
    var err error
    db, err = sql.Open("postgres", os.Getenv("DATABASE_URL"))
    if err != nil {
        log.Fatal(err)
    }
}

func handleGetUser(w http.ResponseWriter, r *http.Request) {
    // uses global db
}

// RIGHT -- Server struct with explicit dependencies
type Server struct {
    db     *sql.DB
    logger *slog.Logger
}

func NewServer(db *sql.DB, logger *slog.Logger) *Server {
    return &Server{db: db, logger: logger}
}

func (s *Server) handleGetUser(w http.ResponseWriter, r *http.Request) {
    ctx := r.Context()
    user, err := s.db.GetUser(ctx, id)
    if err != nil { s.writeError(w, err); return }
    s.writeJSON(w, user)
}
```

### 4.2 init() Overuse

```go
// WRONG -- implicit initialization, hard to test, error handling limited
var config *Config
func init() {
    cfg, err := loadConfig()
    if err != nil {
        log.Fatal(err)
    }
    config = cfg
}

// RIGHT -- explicit initialization in main
func main() {
    cfg, err := loadConfig()
    if err != nil {
        log.Fatalf("load config: %v", err)
    }
    db, err := sql.Open("postgres", cfg.DatabaseURL)
    if err != nil {
        log.Fatalf("open database: %v", err)
    }
    defer db.Close()
    srv := NewServer(db, slog.Default())
    log.Fatal(http.ListenAndServe(":8080", srv.Routes()))
}
```

### 4.3 No Content-Type on JSON Responses

```go
// WRONG -- clients may misinterpret response body
w.WriteHeader(http.StatusOK)
json.NewEncoder(w).Encode(data)

// RIGHT -- set Content-Type before writing
w.Header().Set("Content-Type", "application/json")
w.WriteHeader(http.StatusOK)
json.NewEncoder(w).Encode(data)
```

### 4.4 Missing HTTP Method and Path Validation

```go
// WRONG -- single handler for all methods on a path
func (s *Server) handleOrder(w http.ResponseWriter, r *http.Request) {
    // must manually check r.Method
}

// RIGHT -- explicit routing per method
mux := http.NewServeMux()
mux.HandleFunc("POST /orders", s.createOrder)
mux.HandleFunc("GET /orders/{id}", s.getOrder)
mux.HandleFunc("PUT /orders/{id}", s.updateOrder)
```

**Why agents get it wrong:** Early Go HTTP tutorials used the old `http.HandleFunc("/path", fn)`
pattern which handled all methods. The Go 1.22 method-based routing (2024) is recent.
`init()` functions appear in many standard library examples for driver registration.

---

## Category 5: File Watching with fsnotify

### 5.1 Watching Individual Files Instead of Directories

```go
// WRONG -- editors write to temp files then rename; watcher on original file loses it
err := watcher.Add("/path/to/vault/notes.md")
// When editor saves: writes to notes.md.tmp -> renames to notes.md
// Watcher on original notes.md descriptor is now orphaned

// RIGHT -- watch directory, filter by filename
err := watcher.Add("/path/to/vault/")
// Handle rename/move events by re-adding the file if needed
```

### 5.2 Missing watcher.Close() Defer

```go
// WRONG -- watcher file descriptors leak
watcher, err := fsnotify.NewWatcher()
if err != nil { return err }
// watcher.Close() never called

// RIGHT -- defer close immediately after creation
watcher, err := fsnotify.NewWatcher()
if err != nil { return fmt.Errorf("new watcher: %w", err) }
defer watcher.Close()
```

### 5.3 No Handling of Own Writes

```go
// WRONG -- app modifies a file, watcher triggers, infinite loop
// For vault notes: syncing a note back triggers another event

// RIGHT -- debounce or skip events from own PID
var lastWrite sync.Map // track filenames + timestamps

go func() {
    for {
        select {
        case event, ok := <-watcher.Events:
            if !ok { return }
            // Debounce: skip if we wrote this file within 100ms
            if isOwnWrite(event.Name) { continue }
            
            switch {
            case event.Has(fsnotify.Write):
                // File modified, re-index
                handleVaultFileChange(event.Name)
            case event.Has(fsnotify.Create):
                // New file created
                handleVaultFileCreate(event.Name)
            case event.Has(fsnotify.Remove):
                // File deleted
                handleVaultFileDelete(event.Name)
            }
        case err, ok := <-watcher.Errors:
            if !ok { return }
            log.Printf("watcher error: %v", err)
        }
    }
}()
```

### 5.4 No Event Debouncing

```go
// WRONG -- processes every event immediately, overload on bulk operations
case event := <-watcher.Events:
    processEvent(event) // may run 100x for a git checkout

// RIGHT -- debounce events per file
type debouncedEvent struct {
    name string
    op   fsnotify.Op
    time time.Time
}

debounce := func(events <-chan fsnotify.Event, d time.Duration) <-chan fsnotify.Event {
    out := make(chan fsnotify.Event)
    go func() {
        var timer *time.Timer
        var pending fsnotify.Event
        for {
            select {
            case event, ok := <-events:
                if !ok { return }
                pending = event
                if timer == nil {
                    timer = time.AfterFunc(d, func() {
                        out <- pending
                    })
                } else {
                    timer.Reset(d)
                }
            }
        }
    }()
    return out
}
```

### 5.5 Missing Recursive Directory Watching

```go
// WRONG -- only watches top-level directory, misses subdirectory changes
watcher.Add("/path/to/vault")
// Adding a file to /path/to/vault/subdir/ does not trigger event

// RIGHT -- recursively add all subdirectories
func addRecursive(watcher *fsnotify.Watcher, root string) error {
    return filepath.WalkDir(root, func(path string, d os.DirEntry, err error) error {
        if err != nil { return err }
        if d.IsDir() {
            return watcher.Add(path)
        }
        return nil
    })
}
```

### 5.6 Not Handling Rename Events for Watched Files

```go
// WRONG -- rename breaks the watch, no re-add logic
func handleEvent(event fsnotify.Event) {
    switch {
    case event.Has(fsnotify.Rename):
        // file was moved -- if it was a watched note, we need to re-index
        // and potentially re-add the new path
        log.Printf("file renamed: %s", event.Name)
        // TODO: re-add watch to new location?
    }
}

// RIGHT -- handle rename by re-adding the file at its new location
func handleEvent(watcher *fsnotify.Watcher, event fsnotify.Event) {
    if event.Has(fsnotify.Rename) {
        // If a watched vault file was renamed, re-add the watch
        // This depends on the editor's behavior (atomic saves)
        // Usually the renamed version is the same file at a new path
    }
}
```

**Why agents get it wrong:** Most training examples use the simple fsnotify example
from the README which watches a single file or directory and prints events. Production
patterns (debouncing, recursive watching, rename handling, own-write filtering) are
rarely covered in tutorials.

---

## Category 6: File Locking and Vault Integrity

### 6.1 No File Locking on Concurrent Vault Access

```go
// WRONG -- two processes/goroutines writing the same .md file corrupts it
func WriteNote(path string, content []byte) error {
    return os.WriteFile(path, content, 0644)
}

// RIGHT -- acquire file lock before writing
import "golang.org/x/sys/unix"

func WriteNote(path string, content []byte) error {
    f, err := os.OpenFile(path, os.O_RDWR|os.O_CREATE, 0644)
    if err != nil {
        return fmt.Errorf("open note: %w", err)
    }
    defer f.Close()

    // Acquire exclusive lock
    if err := unix.Flock(int(f.Fd()), unix.LOCK_EX); err != nil {
        return fmt.Errorf("lock note: %w", err)
    }
    defer unix.Flock(int(f.Fd()), unix.LOCK_UN)

    // Truncate and write
    if err := f.Truncate(0); err != nil {
        return fmt.Errorf("truncate note: %w", err)
    }
    if _, err := f.Seek(0, 0); err != nil {
        return fmt.Errorf("seek note: %w", err)
    }
    if _, err := f.Write(content); err != nil {
        return fmt.Errorf("write note: %w", err)
    }
    return nil
}
```

### 6.2 Read-Modify-Write Without Lock

```go
// WRONG -- race: reads note, another process writes, then we write stale data
note, _ := os.ReadFile(path)
updated := updateFrontmatter(note, "status: completed")
os.WriteFile(path, updated, 0644) // may overwrite another writer's changes

// RIGHT -- lock covers the entire read-modify-write cycle
func UpdateNoteStatus(path string, status string) error {
    f, err := os.OpenFile(path, os.O_RDWR, 0644)
    if err != nil { return err }
    defer f.Close()

    if err := syscall.Flock(int(f.Fd()), syscall.LOCK_EX); err != nil {
        return err
    }
    defer syscall.Flock(int(f.Fd()), syscall.LOCK_UN)

    data, err := io.ReadAll(f)
    if err != nil { return err }

    updated := updateFrontmatter(data, "status", status)

    f.Truncate(0)
    f.Seek(0, 0)
    _, err = f.Write(updated)
    return err
}
```

### 6.3 Blocking Lock Without Timeout

```go
// WRONG -- may block forever if another process holds the lock
syscall.Flock(int(f.Fd()), syscall.LOCK_EX)

// RIGHT -- try lock with timeout or non-blocking attempt
func tryLock(f *os.File, timeout time.Duration) error {
    deadline := time.Now().Add(timeout)
    for time.Now().Before(deadline) {
        err := syscall.Flock(int(f.Fd()), syscall.LOCK_EX|syscall.LOCK_NB)
        if err == nil {
            return nil
        }
        if err != syscall.EWOULDBLOCK {
            return err
        }
        time.Sleep(10 * time.Millisecond)
    }
    return fmt.Errorf("timeout waiting for lock on %s", f.Name())
}
```

**Why agents get it wrong:** File locking is OS-specific and rarely appears in Go
tutorials. Most training examples read and write files without any synchronization.
Agents assume single-process access.

---

## Category 7: Markdown and YAML Frontmatter

### 7.1 Manual Frontmatter Parsing Instead of Library

```go
// WRONG -- brittle string splitting, breaks on complex YAML
func parseFrontmatter(data []byte) (map[string]string, []byte, error) {
    parts := strings.SplitN(string(data), "---", 3)
    if len(parts) < 3 {
        return nil, nil, fmt.Errorf("invalid frontmatter")
    }
    // manual YAML parsing is fragile
    frontmatter := make(map[string]string)
    for _, line := range strings.Split(parts[1], "\n") {
        kv := strings.SplitN(line, ":", 2)
        if len(kv) == 2 {
            frontmatter[strings.TrimSpace(kv[0])] = strings.TrimSpace(kv[1])
        }
    }
    return frontmatter, []byte(parts[2]), nil
}

// RIGHT -- use a proper frontmatter parser
import (
    "go.abhg.dev/goldmark/frontmatter"
    "github.com/yuin/goldmark"
    "github.com/yuin/goldmark/parser"
)

func parseNote(data []byte) (*Note, error) {
    var fm struct {
        ID        string   `yaml:"id"`
        Type      string   `yaml:"type"`
        Status    string   `yaml:"status"`
        CreatedAt string   `yaml:"created_at"`
        UpdatedAt string   `yaml:"updated_at"`
        Tags      []string `yaml:"tags"`
    }

    md := goldmark.New(
        goldmark.WithExtensions(
            &frontmatter.Extender{},
        ),
        goldmark.WithParserOptions(
            parser.WithASTTransformers(
                util.Prioritized(&frontmatter.ASTTransformer{}, 100),
            ),
        ),
    )

    ctx := parser.NewContext()
    doc := md.Parser().Parse(text.NewReader(data), parser.WithContext(ctx))
    
    if fmData := frontmatter.Get(ctx); fmData != nil {
        if err := fmData.Decode(&fm); err != nil {
            return nil, fmt.Errorf("decode frontmatter: %w", err)
        }
    }

    // Render body
    var body bytes.Buffer
    if err := md.Renderer().Render(&body, data, doc); err != nil {
        return nil, fmt.Errorf("render body: %w", err)
    }

    return &Note{
        ID:        fm.ID,
        Type:      fm.Type,
        Status:    fm.Status,
        CreatedAt: fm.CreatedAt,
        UpdatedAt: fm.UpdatedAt,
        Tags:      fm.Tags,
        Body:      body.String(),
    }, nil
}
```

### 7.2 No Validation of Required Frontmatter Fields

```go
// WRONG -- missing field validation, silently stores incomplete notes
type Note struct {
    ID     string `yaml:"id"`
    Status string `yaml:"status"`
}

// RIGHT -- validate required fields after parsing
func validateNote(note *Note) error {
    var errs []error
    
    if note.ID == "" {
        errs = append(errs, errors.New("note.id is required"))
    }
    if note.Type == "" {
        errs = append(errs, errors.New("note.type is required"))
    }
    if note.Status == "" {
        errs = append(errs, errors.New("note.status is required"))
    }
    if note.UpdatedAt == "" {
        errs = append(errs, errors.New("note.updated_at is required"))
    }

    if len(errs) > 0 {
        return fmt.Errorf("note validation failed: %w", errors.Join(errs...))
    }
    return nil
}
```

### 7.3 Encoding YAML with Wrong Types

```go
// WRONG -- times serialized as strings, no standard format
type Note struct {
    UpdatedAt string `yaml:"updated_at"` // no format guarantee
}

// RIGHT -- use proper types with custom marshaling
type Note struct {
    UpdatedAt time.Time `yaml:"updated_at"`
}

// Serialize to YAML
out, err := yaml.Marshal(&note)
// Produces: updated_at: 2025-01-01T10:00:00Z
```

**Why agents get it wrong:** Markdown frontmatter is a niche format. Most Go YAML
tutorials focus on standalone YAML files, not embedded frontmatter. Agents default
to simple string splitting because it's the first thing that comes to mind.

---

## Category 8: API Design for AI Agents

### 8.1 Generic Error Messages

```go
// WRONG -- agent cannot self-correct from generic messages
{"error": "bad request", "code": 400}

// RIGHT -- actionable error details
{
    "error": {
        "title": "Invalid Request",
        "detail": "Field 'customer_id' is required. Provide a valid UUID.",
        "status": 422,
        "field": "customer_id",
        "recovery_action": "include customer_id in request body"
    }
}
```

### 8.2 No Idempotency on State-Mutating Endpoints

```go
// WRONG -- agent retry creates duplicate records
func (s *Server) createOrder(w http.ResponseWriter, r *http.Request) {
    var req CreateOrderRequest
    // ... process and insert
    db.Exec("INSERT INTO orders ...") // duplicate on retry!
}

// RIGHT -- idempotency key prevents duplicate processing
func (s *Server) createOrder(w http.ResponseWriter, r *http.Request) {
    idempotencyKey := r.Header.Get("Idempotency-Key")
    if idempotencyKey == "" {
        s.writeError(w, "Idempotency-Key header required", http.StatusBadRequest)
        return
    }

    // Check if already processed
    existing, err := s.store.GetByIdempotencyKey(r.Context(), idempotencyKey)
    if err == nil {
        s.writeJSON(w, existing)
        return
    }

    // Process and cache response with idempotency key
    order, err := s.processOrder(r.Context(), req)
    if err != nil { s.writeError(w, err); return }
    
    s.store.CacheResponse(idempotencyKey, order, 24*time.Hour)
    s.writeJSON(w, order, http.StatusCreated)
}
```

### 8.3 Inconsistent Endpoint Naming

```go
// WRONG -- mixed conventions confuse agent endpoint selection
GET /user-list          // kebab-case, not RESTful
POST /createUser        // verb in URL
GET /fetch_user/123     // snake_case, GET with verb
DELETE /delete-user/456 // verb in URL

// RIGHT -- consistent plural nouns, HTTP verbs express action
GET    /users          // list all
GET    /users/{id}     // get one
POST   /users          // create
PUT    /users/{id}     // replace
DELETE /users/{id}     // delete
PATCH  /users/{id}     // partial update
```

### 8.4 No Rate Limit Headers

```go
// WRONG -- agent discovers limits only via 429 responses
func rateLimitMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        if isRateLimited(r) {
            http.Error(w, "rate limit exceeded", http.StatusTooManyRequests)
            return
        }
        next.ServeHTTP(w, r)
    })
}

// RIGHT -- proactive headers let agents self-pace
func rateLimitMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        remaining, resetAt := getRateLimitInfo(r)
        
        w.Header().Set("X-RateLimit-Remaining", strconv.Itoa(remaining))
        w.Header().Set("X-RateLimit-Reset", fmt.Sprintf("%d", resetAt.Unix()))
        
        if remaining <= 0 {
            w.Header().Set("Retry-After", strconv.Itoa(int(time.Until(resetAt).Seconds())))
            http.Error(w, `{"error":"rate limit exceeded","retry_after":`+
                strconv.Itoa(int(time.Until(resetAt).Seconds()))+`}`, 
                http.StatusTooManyRequests)
            return
        }
        next.ServeHTTP(w, r)
    })
}
```

### 8.5 Missing operationId in OpenAPI Spec

```yaml
# WRONG -- auto-generated operationId, agent cannot distinguish endpoints
paths:
  /users/{id}:
    get:
      operationId: users_get_0  # meaningless to agent
      description: Get user

# RIGHT -- descriptive operationId, concise description
paths:
  /users/{id}:
    get:
      operationId: getUserById
      description: >
        Retrieve a user by their unique ID. Returns full user profile
        including name, email, role, and status. Used for profile display
        and permission checks.
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
```

**Why agents get it wrong:** Traditional API design optimizes for human developers
who read docs and write clients. AI agents need different contracts: they parse
`operationId` as function names, treat error bodies as instructions, and cannot
infer conventions from context.

---

## Category 9: Testing

### 9.1 No Table-Driven Tests

```go
// WRONG -- repetitive, hard to add cases, hard to read
func TestAdd(t *testing.T) {
    if got := Add(2, 3); got != 5 {
        t.Errorf("Add(2,3) = %d, want 5", got)
    }
    if got := Add(0, 0); got != 0 {
        t.Errorf("Add(0,0) = %d, want 0", got)
    }
    if got := Add(-1, 1); got != 0 {
        t.Errorf("Add(-1,1) = %d, want 0", got)
    }
}

// RIGHT -- table-driven, easy to extend
func TestAdd(t *testing.T) {
    cases := []struct {
        name string
        a, b int
        want int
    }{
        {"zero sum", 0, 0, 0},
        {"positive", 2, 3, 5},
        {"negative", -1, 1, 0},
        {"large", 1000, 2000, 3000},
    }
    for _, tc := range cases {
        t.Run(tc.name, func(t *testing.T) {
            if got := Add(tc.a, tc.b); got != tc.want {
                t.Fatalf("Add(%d,%d) = %d, want %d", tc.a, tc.b, got, tc.want)
            }
        })
    }
}
```

### 9.2 No Race Detector in CI

```go
// WRONG -- CI only runs basic test
// CI script:
go test ./...

// RIGHT -- always run with race detector for concurrent code
// CI script:
go test -race ./... -count=1
```

### 9.3 Mocking *sql.DB Instead of Repository Interface

```go
// WRONG -- mocks the DB directly, brittle, tests implementation not behavior
type mockDB struct {
    *sql.DB // brittle, follows DB structure not behavior
}

// RIGHT -- define small interface at consumer, mock that
type userRepository interface {
    GetByID(ctx context.Context, id int) (*User, error)
    Save(ctx context.Context, user *User) error
}

type mockRepo struct {
    users map[int]*User
}

func (m *mockRepo) GetByID(ctx context.Context, id int) (*User, error) {
    if u, ok := m.users[id]; ok {
        return u, nil
    }
    return nil, ErrUserNotFound
}
```

### 9.4 No t.Helper() in Test Helper Functions

```go
// WRONG -- failure points to helper line, not test case
func assertUser(t *testing.T, got, want *User) {
    if got.ID != want.ID {
        t.Errorf("ID = %d, want %d", got.ID, want.ID)
    }
}

// RIGHT -- t.Helper() reports call site on failure
func assertUser(t *testing.T, got, want *User) {
    t.Helper()
    if got.ID != want.ID {
        t.Errorf("ID = %d, want %d", got.ID, want.ID)
    }
}
```

### 9.5 No t.Cleanup() for Test Resources

```go
// WRONG -- manual cleanup at end of function, skipped on early failure
func TestWithDB(t *testing.T) {
    db := setupTestDB()
    // ... test ...
    db.Close() // skipped if test fails before this line
}

// RIGHT -- deferred cleanup runs even on failure
func TestWithDB(t *testing.T) {
    db := setupTestDB()
    t.Cleanup(func() { db.Close() })
    // ... test ...
}
```

---

## Category 10: Interface Design and Struct Patterns

### 10.1 Interface Defined in Producer Package

```go
// WRONG -- interface defined next to implementation, caller forced to import it
package repository

type UserRepository interface {
    GetByID(ctx context.Context, id int) (*User, error)
    Save(ctx context.Context, user *User) error
}

type postgresRepo struct { ... }
func NewPostgresRepo(db *sql.DB) UserRepository { return &postgresRepo{db: db} }

// RIGHT -- interface defined at consumer, small, 1-3 methods
package service

type userRepo interface {
    GetByID(ctx context.Context, id int) (*User, error)
}

func NewUserService(repo userRepo) *UserService {
    return &UserService{repo: repo}
}
```

### 10.2 Interface with Too Many Methods

```go
// WRONG -- bloated interface, hard to implement and mock
type Service interface {
    GetUser(ctx context.Context, id int) (*User, error)
    ListUsers(ctx context.Context) ([]*User, error)
    CreateUser(ctx context.Context, user *User) error
    UpdateUser(ctx context.Context, user *User) error
    DeleteUser(ctx context.Context, id int) error
    GetUserByEmail(ctx context.Context, email string) (*User, error)
    GetUserStats(ctx context.Context, id int) (*Stats, error)
}

// RIGHT -- small, focused interfaces
type userReader interface {
    GetByID(ctx context.Context, id int) (*User, error)
}

type userWriter interface {
    Save(ctx context.Context, user *User) error
}
```

### 10.3 sync.Mutex Embedded Publicly

```go
// WRONG -- embedding exposes Lock/Unlock to all callers
type Cache struct {
    sync.Mutex          // Lock() and Unlock() are now public
    data map[string]string
}

// RIGHT -- named field, mutex stays private
type Cache struct {
    mu   sync.Mutex
    data map[string]string
}
```

### 10.4 Named Return Values (Except Defer-Modified)

```go
// WRONG -- naked return hides what's returned
func parse(s string) (doc *Doc, err error) {
    doc, err = parseInternal(s)
    return // what is returned? doc and err but reader must scan function
}

// RIGHT -- explicit returns, no named params
func parse(s string) (*Doc, error) {
    doc, err := parseInternal(s)
    if err != nil {
        return nil, fmt.Errorf("parse: %w", err)
    }
    return doc, nil
}

// ACCEPTABLE -- named return for defer-modified errors
func process(ctx context.Context) (err error) {
    defer func() {
        if err != nil {
            err = fmt.Errorf("process: %w", err)
        }
    }()
    return doWork(ctx)
}
```

---

## Why AI Agents Produce These Patterns

| Root Cause | Explanation |
|---|---|
| Training data lag | Most models trained on Go code from 2018-2023. Go 1.22 routing patterns (2024), structured logging (Go 1.21, 2023), and `%w` wrapping (Go 1.13, 2019) are recent or underrepresented. |
| Tutorial bias | Training data over-represents simplified blog examples and Stack Overflow answers. `init()` usage, package globals, and ignored errors are common in quick-start guides. |
| Concurrency blind spots | Correct concurrent code requires reasoning about runtime behavior (liveness, leaks, deadlocks) that is invisible in static source text. Training examples omit cleanup paths. |
| Platform ignorance | File locking, fsnotify edge cases, and OS-specific behavior are niche topics. Most training data treats file I/O as trivially safe. |
| No project context | Agents generate code without awareness of the project's established patterns, dependency choices, or architecture conventions. |

---

## How to Prevent These in AI-Generated Code

1. **Load this reference before code review** -- grep for each WRONG pattern column
   after any agent-generated code block.

2. **Use project-level rules** -- The project's AGENTS.md already bans these patterns.
   The agent MUST read AGENTS.md before generating any code.

3. **Run validation early** -- Always run `go vet ./...`, `go test -race ./...`, and
   `staticcheck ./...` on agent-generated Go code before committing.

4. **Review error handling first** -- Spend 30 seconds scanning for `_` in error
   positions and bare `return err` without wrapping. This catches the most common
   and most dangerous AI mistakes.

5. **Request concurrent code review** -- Agent-generated goroutines, channels, and
   `sync` primitives need extra scrutiny. Check for leak paths, cancellation,
   and proper `select` with `ctx.Done()`.

6. **Pin dependencies** -- Define all Go module versions in `go.mod`. Do not let
   the agent guess or use untagged pseudo-versions.

---

## See Also

- [agent-anti-patterns.md (Compose)](../.pi/agent/skills/compose-skill/references/agent-anti-patterns.md) -- Kotlin/Jetpack Compose AI anti-patterns
- AGENTS.md (project root) -- Project-level agent rules and configuration
