---
feature: m3-ui-refactor
status: designed
updated: 2025-07-25
branch: feat/m3-ui-refactor
---

# M3 UI Refactor & Dashboard Simplification

## Report

## [S1] Problem
The app has accumulated UI bloat: Dashboard has redundant scope chips (today/tomorrow/week/pick date) and a search button that duplicates the workspace search, Settings page is too verbose, TaskEdit page has separate Date and Time sections that waste vertical space, subtasks lack drag-and-drop reordering, and occurrence editing incorrectly allows date range selection.

## [S2] Design

### D1: Dashboard Simplification
- Remove `ScopeChipsRow` (today/tomorrow/week/pick date) entirely
- Remove `DateSelectorCard` (the date picker row)
- Remove search button from TopAppBar actions
- Dashboard always shows today's view: greeting + date in TopAppBar, then routine, overdue tasks, today's tasks, quick task input, quick task list, quick note input
- Remove the `DatePickerDialog` and related state from DashboardScreen
- Remove `SelectScope`, `SelectDate` events from DashboardEvent (keep `LoadData`)
- Remove `selectedScope`, `selectedDate`, `weekStartDate`, `weekTasksByDay` from DashboardUiState
- DashboardViewModel: on init, just load today's data (no scope switching)

### D2: Settings Minimalist Redesign
- Remove `Shading Intensity` slider and its labels
- Remove `Gradient Background` toggle
- Remove `OLED Black Theme` toggle
- Keep: Server URL, Dark Mode (SegmentedButton replacing FilterChip flow), Color Source (SegmentedButton), Palette Style (SegmentedButton), About section, Save button
- Replace `FilterChip` flows with `SingleChoiceSegmentedButtonRow` for Dark Mode, Color Source, and Palette Style
- Remove `SettingsToggleRow` composable (no more toggle rows)
- Remove `SettingsSection` card wrapper — use direct `Column` with spacing for a flatter look
- Keep Material You colors throughout

### D3: TaskEdit Consolidation
- **Merge Date + Time into one section** called "Schedule"
- Time mode becomes a `SingleChoiceSegmentedButtonRow` with 4 options: None / Due Time / Start/End / Start+Duration (replacing the `FlowRow` of `FilterChip`)
- Date mode stays as `SingleChoiceSegmentedButtonRow` (already done)
- Both date and time controls live in the same "Schedule" card
- **Occurrence edit restriction**: When editing an occurrence (`isOccurrenceEdit == true`), hide the "Range" option from the date mode segmented button — only show None and Due Date
- Add a boolean `allowDateRange` to TaskEditUiState, computed as `!isOccurrenceEdit`
- In the date mode segmented button, filter out the "range" option when `allowDateRange == false`

### D4: Subtask Drag-and-Drop
- Use `org.burnoutcrew.composereorderable:reorderable:2.4.3` dependency
- Subtask list in TaskEditScreen becomes a `LazyColumn` with `Modifier.reorderable(reorderState)`
- Each subtask row gets a drag handle icon (`Icons.Default.DragHandle`)
- On reorder end, dispatch `TaskEditEvent.ReorderSubtasks(fromIndex, toIndex)` to ViewModel
- ViewModel moves the item in the `subtasks` list

### D5: Calendar Optimization
- Replace manual `CalendarGrid` composable with M3 `HorizontalPager` for month swiping
- Add `rememberPagerState` with `PagerState` centered on current month
- Each page renders one month's grid
- Preload previous/next months (±6 months) for smooth swiping
- Keep existing task list below calendar grid

### D6: Dashboard Search Removal
- Dashboard no longer has a search button
- Search is only accessible from the Workspace tab or as a standalone screen
- Remove `onNavigateToSearch` parameter from `DashboardScreen`
- Remove `Search` icon from DashboardScreen TopAppBar

### D7: M3 Component Upgrades
- `SearchScreen`: Replace `Surface` card with `ElevatedCard` + `CardDefaults`
- `TaskListScreen`: Already uses `ElevatedCard` — verify consistent usage
- `NoteListScreen`: Already uses `ElevatedCard` — verify
- `PersonListScreen`: Already uses `ElevatedCard` — verify
- All empty states: use 80dp icons with 30% alpha tint (primary/tertiary/secondary per entity type), `headlineMedium` title, `bodyLarge` subtitle, `FilledTonalButton` action — already done, verify consistency

## [S3] Out of Scope
- Bottom bar redesign (user explicitly excluded)
- New third-party UI libraries beyond reorderable
- Navigation structure changes
- Backend changes
- New screens or features

## Tasks
- [ ] T1: Dashboard simplification — remove scope chips, date selector, search button; keep today-only view (covers: D1, D6)
- [ ] T2: Settings minimalist redesign — remove toggles, replace FilterChips with SegmentedButtons, flatten layout (covers: D2)
- [ ] T3: TaskEdit schedule consolidation — merge Date+Time into Schedule section, time SegmentedButton, occurrence date range restriction (covers: D3)
- [ ] T4: Subtask drag-and-drop — add reorderable dependency, drag handle, ReorderSubtasks event (covers: D4)
- [ ] T5: Calendar optimization — HorizontalPager for month swiping (covers: D5)
- [ ] T6: SearchScreen M3 upgrade — ElevatedCard for search results (covers: D7)
- [ ] T7: Verify all changes build and install (covers: all)
