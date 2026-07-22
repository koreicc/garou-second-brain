# Second Brain Android - Overhaul Progress Summary

## Project Context

Second Brain System is a lightweight, local-first PKM (Personal Knowledge
Management) app. It tracks notes, tasks, quick tasks, and people via a
Markdown vault -- all human-readable .md files with YAML frontmatter.

- **Backend**: Go (echo) runs in Termux on Android device, localhost:8080
- **Frontend**: Kotlin Jetpack Compose app connecting to localhost API
- **Architecture**: MVI (StateFlow + sealed interface events), Ktor networking, Material3

## Overhaul Plan (docs/android-overhaul-plan.md)

The full plan is in the repo at `docs/android-overhaul-plan.md`. 7 phases total.
3 phases completed so far.

All work is on feature branches. NEVER commit to main.

---

## Phase 1 - Kritik Hata Duzeltmeleri (COMPLETED)
Branch: `feat/android-p1-critical-fixes`

### Changes Made

1. **Task Icon Display Fix** (`TaskListScreen.kt`, `DashboardScreen.kt`, `TaskDetailScreen.kt`)
   - `resolveIcon()` from `IconPicker.kt` is now used to render actual Material icons
   - Fallback to first letter of title when icon name unknown
   - Fix applied in all 3 screens where task icons appear

2. **Wikilink Navigation** (`NoteDetailScreen.kt`, `NoteDetailViewModel.kt`, `TaskDetailScreen.kt`, `TaskDetailViewModel.kt`, `AppNavigation.kt`)
   - Created `WikilinkNavigationTarget` data class (type + id)
   - Added `ResolveWikilink` event to both NoteDetail and TaskDetail ViewModels
   - Uses `searchRepository.search(target)` to find entities
   - If exactly 1 result: navigates to that entity's detail page
   - If 0 results: shows "No entity found" error
   - If multiple: shows "be more specific" error
   - Added navigation callbacks (`onNavigateToNote/Task/Person`) to detail screens

### Key Decisions
- Wikilink resolution uses the existing `searchRepository.search()` API
- No backend changes needed for P1

---

## Phase 2 - Bottom Bar + Navigasyon (COMPLETED)
Branch: `feat/android-p2-bottom-bar-redesign`

### Changes Made

1. **Screen.kt** - Routes updated:
   - Removed: `Search`, `NoteList`, `TaskList`, `PersonList`
   - Added: `Workspace`, `Settings`

2. **NavigationBar.kt** (NEW) - Floating pill + FAB style (Remember app inspired):
   - 3 tabs: Dashboard, Workspace, Settings
   - Floating pill with `RoundedCornerShape(28.dp)`, shadow elevation
   - FAB positioned next to pill, NOT inside it
   - Selected tab: icon + label with filled `primaryContainer` background
   - Unselected tabs: icon only with `onSurfaceVariant` color
   - Animated transitions: width 48dp->88dp, color, label fade with spring animation
   - Spring spec: `DampingRatioMediumBouncy`, `StiffnessLow`
   - Pill position: 28dp from bottom edge

3. **FabMenu.kt** (NEW) - FAB expansion menu:
   - 3 options: New Note, New Task, New Person
   - Animated overlay with semi-transparent backdrop
   - Each option is a Card with icon + label + subtitle
   - Positioned above the nav pill

4. **WorkspaceScreen.kt** (NEW) + **WorkspaceViewModel.kt** (NEW):
   - Unified entity management with 3 tabs (Notes/Tasks/People)
   - Global search bar at top (client-side filtering)
   - Delete confirmation dialog for each entity type
   - Uses `AppModule` for dependency injection
   - Pattern: Surface cards with left accent bar like original screens

5. **SettingsScreen.kt** (NEW) + **SettingsViewModel.kt** (NEW):
   - Server URL configuration
   - Dark mode (System/Light/Dark)
   - Color Source selection (Material You, Custom, 7 presets)
   - Palette Style selection (9 styles)
   - Gradient toggle, OLED mode, Shading slider
   - Settings NOT persisted yet (DataStore TODO)

6. **AppNavigation.kt** - Complete rewrite:
   - Box wrapper for Scaffold + FabMenuOverlay
   - showBottomBar only on main destinations
   - Workspace screen has its own internal tab navigation
   - Dashboard "See all" links navigate to Workspace

### Key Decisions
- No experimental Material3 APIs used (HorizontalFloatingToolbar not in our BOM)
- Custom pill implementation with Surface + Row
- Workspace replaces old NoteList/TaskList/PersonList + Search
- Old list screens still exist but are no longer top-level routes

---

## Phase 3 - Dashboard Planlayici + Rutinler (COMPLETED)
Branch: `feat/android-p3-dashboard-planner`

All 3 phases are on `feat/android-p3-dashboard-planner`.

### Changes Made

1. **DashboardViewModel.kt** - Complete rewrite:
   - New state: `greeting`, `dateString`, `routine: RoutineInfo?`, `routineTimeOfDay`, `todayTasks`, `quickNoteTitle`
   - Removed old: `noteCount`, `taskCount`, `personCount`, `recentNotes`, `recentTasks`
   - Routine detection: tag-based (`routine` + `morning-routine`/`evening-routine`)
   - Time-based display: morning (5-11), evening (17-21)
   - Today's tasks: pending/in-progress, date-filtered, max 5, sorted by end_date
   - Routine subtask toggling via `taskRepository.update()`
   - Complete Routine button marks all subtasks as complete

2. **DashboardScreen.kt** - Complete rewrite:
   - Sections: Greeting, Routine, Today's Tasks, Quick Task, Quick Note
   - Routine section: LinearProgressIndicator, subtask checkboxes, Complete button
   - Today's Tasks: compact cards with StatusBadge, clickable, "See all"
   - Quick Note: new input field matching Quick Task pattern
   - Removed: StatsRow, StatCard, old NoteCard/TaskCard
   - Function signature preserved for AppNavigation compatibility

### Key Decisions
- Routines = tasks with tags `routine` + `morning-routine`/`evening-routine`
- Subtasks of the routine task = checklist items
- No new backend endpoint needed (reuses existing task API)
- Quick note uses `noteRepository.create(CreateNoteRequest(title = ...))`

---

## Architecture Summary

### Navigation Structure
```
AppNavigation (Box)
  +-- Scaffold
  |    +-- SecondBrainBottomBar (floating pill + FAB)
  |    +-- NavHost
  |         +-- Dashboard (tab 0)
  |         +-- Workspace (tab 1) with internal Notes/Tasks/People tabs
  |         +-- Settings (tab 2)
  |         +-- NoteDetail / NoteEdit (push screens)
  |         +-- TaskDetail / TaskEdit (push screens)
  |         +-- PersonDetail / PersonEdit (push screens)
  +-- FabMenuOverlay (overlay, above scaffold)
```

### Key Files Map
```
ui/
  navigation/
    Screen.kt              - Route definitions (Dashboard, Workspace, Settings, detail/edit screens)
    AppNavigation.kt       - Root nav, scaffold, bottom bar, FAB overlay
    NavigationBar.kt       - Floating pill + FAB component
    FabMenu.kt             - FAB expansion menu overlay
  dashboard/
    DashboardScreen.kt     - Daily planner (greeting, routine, today tasks, quick task/note)
    DashboardViewModel.kt  - Time-aware state, routine logic, today's tasks
  workspace/
    WorkspaceScreen.kt     - Entity management (Notes/Tasks/People tabs + search)
    WorkspaceViewModel.kt  - Multi-entity state, delete confirmation
  settings/
    SettingsScreen.kt      - Theme and server config UI
    SettingsViewModel.kt   - Settings state (not persisted yet)
  notes/ (unchanged except P1 wikilink changes)
  tasks/ (unchanged except P1 icon + wikilink changes)
  people/ (unchanged)
  theme/ (unchanged + paletteStyleLabel helper)
  util/ (unchanged)
data/
  api/        - Ktor API service
  dto/        - Data transfer objects with SerialName mappings
  repository/ - Repository pattern with Result<T>
di/
  AppModule.kt - Singleton DI (manual, no framework)
domain/
  model/      - Domain models (Note, Task, Person, QuickTask, etc.)
```

---

## Phase 4 + 6 - LinkPicker + Linked Entities Display (COMPLETED)
Branch: `feat/android-p4-link-picker`

### Changes Made

1. **LinkedEntityInfo** (`domain/model/LinkedEntityInfo.kt`) (NEW)
   - Data class: id, type (note/task/person), title, subtitle, status
   - Used by LinkPicker and LinkedEntitiesView for unified entity representation

2. **LinkingRepository** (`data/repository/LinkingRepository.kt`) (NEW)
   - `resolveLinks(ids)`: fetches all entities in parallel, builds lookup map, returns matching entities
   - `getAllLinkableEntities()`: returns all notes/tasks/people as LinkedEntityInfo list
   - Registered in AppModule as singleton

3. **LinkPickerSheet** (`ui/common/LinkPickerSheet.kt`) (NEW)
   - ModalBottomSheet with 3 tab chips (Notes, Tasks, People)
   - Search bar per tab with entity-type icon
   - Checkmark selection on entities
   - Confirm button showing selection count
   - Loading state with CircularProgressIndicator
   - Used by all 3 edit screens (Note, Task, Person)

4. **LinkedEntitiesView** (`ui/common/LinkedEntitiesView.kt`) (NEW)
   - Horizontal LazyRow of entity chips with type-specific icons and colors
   - Uses LaunchedEffect + AppModule.linkingRepository to load linked entity info
   - Clickable chips navigate to the linked entity's detail screen
   - Used by all 3 detail screens (Note, Task, Person)

5. **NoteEditViewModel/Screen** - Updated
   - Added `links` field to NoteEditUiState
   - Added ShowLinkPicker/DismissLinkPicker/SetLinks events
   - Save requests include `links` field
   - UI: "Linked Entities" section (when links exist) + "Add/Edit Links" button

6. **TaskEditViewModel/Screen** - Updated
   - Added `links` field to TaskEditUiState
   - Added ShowLinkPicker/DismissLinkPicker/SetLinks events
   - Save requests include `links` field
   - UI: "Linked Entities" section + "Add/Edit Links" button (after Tags & Subtasks)

7. **PersonEditViewModel/Screen** - Updated
   - Added `links` field to PersonEditUiState
   - Added ShowLinkPicker/DismissLinkPicker/SetLinks events
   - Save requests include `links` field
   - UI: "Linked Entities" section + "Add/Edit Links" button

8. **NoteDetailScreen** - Updated
   - LinkedEntitiesView shown between tags and body

9. **TaskDetailScreen** - Updated
   - LinkedEntitiesView shown between info card and subtasks

10. **PersonDetailScreen** - Updated
    - Added navigation callbacks (onNavigateToNote/Task/Person)
    - LinkedEntitiesView shown between avatar and tags

11. **AppNavigation** - Updated
    - PersonDetailScreen now receives navigation callbacks for linked entity navigation

### Key Decisions
- No backend changes needed; links stored as UUID list in existing YAML frontmatter
- LinkingRepository fetches all entities in parallel (3 API calls) for O(1) lookup
- LinkPicker uses ModalBottomSheet (standard Material3, no experimental APIs)
- LinkedEntitiesView uses LazyRow for horizontal scrolling of linked entity chips
- Self-contained data loading in LinkedEntitiesView (LaunchedEffect + AppModule)
- Entity type icons: NoteAlt=tertiary, TaskAlt=primary, People=secondary

---

## Remaining Phases

### Phase 5 - Settings Persistence (NOT STARTED)
- Save settings to DataStore
- Connect ThemeState to saved settings

### Phase 7 - CI/CD (NOT STARTED)
- Release build signing
- Lint checks in CI
- APK artifact already works

---

## GitHub Actions Build
- Android Build workflow triggers on push to main, feat/**, fix/**
- Build takes ~3-4 minutes
- APK artifact: `app-debug` (download with `gh run download <id> --name app-debug --dir ./dir`)
- Backend Build also runs on pushes
- APK installed via ADB: `adb install -r ./path/app-debug.apk` (may need `adb uninstall com.secondbrain` first due to signature mismatch)

---

## Code Style Rules (from AGENTS.md)
- NEVER use emojis in any file
- Material3 + MVI architecture
- `collectAsStateWithLifecycle` for state collection
- `StateFlow` for UI state
- `sealed interface` for events
- ViewModel factory pattern: `viewModel(factory = object : ViewModelProvider.Factory { ... })`
- Repositories from `AppModule` (e.g., `AppModule.taskRepository`)
- Transparent scaffold with gradient background
- Surface cards with `surfaceContainer` color
- Left accent bar (4dp) on list cards
- Icons from `material-icons-extended` (full icon set available)
- Compose BOM 2025.01.01

---

## Current Branch
`feat/android-p4-link-picker` - contains phases 1-4 and 6.

Next phase: `feat/android-p5-settings-persistence`
