package vault

const schemaSQL = `
CREATE TABLE IF NOT EXISTS entities (
    id              TEXT PRIMARY KEY,
    type            TEXT NOT NULL,
    title           TEXT NOT NULL DEFAULT '',
    status          TEXT NOT NULL DEFAULT 'pending',
    icon            TEXT NOT NULL DEFAULT '',
    location        TEXT NOT NULL DEFAULT '',
    priority        TEXT NOT NULL DEFAULT '',
    tags            TEXT NOT NULL DEFAULT '[]',
    links           TEXT NOT NULL DEFAULT '[]',
    parent_id       TEXT NOT NULL DEFAULT '',
    is_template     INTEGER NOT NULL DEFAULT 0,
    occurrence_date TEXT NOT NULL DEFAULT '',
    date_mode       TEXT NOT NULL DEFAULT '',
    due_date        TEXT,
    start_date      TEXT,
    end_date        TEXT,
    time_mode       TEXT NOT NULL DEFAULT '',
    start_time      TEXT NOT NULL DEFAULT '',
    end_time        TEXT NOT NULL DEFAULT '',
    duration_minutes INTEGER NOT NULL DEFAULT 0,
    due_time        TEXT NOT NULL DEFAULT '',
    days_of_week    TEXT NOT NULL DEFAULT '[]',
    name            TEXT NOT NULL DEFAULT '',
    contacts        TEXT NOT NULL DEFAULT '[]',
    social_links    TEXT NOT NULL DEFAULT '[]',
    notes_body      TEXT NOT NULL DEFAULT '',
    body            TEXT NOT NULL DEFAULT '',
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_entities_type     ON entities(type);
CREATE INDEX IF NOT EXISTS idx_entities_status   ON entities(status);
CREATE INDEX IF NOT EXISTS idx_entities_priority ON entities(priority);
CREATE INDEX IF NOT EXISTS idx_entities_parent   ON entities(parent_id);
CREATE INDEX IF NOT EXISTS idx_entities_due_date ON entities(due_date);

CREATE TABLE IF NOT EXISTS subtasks (
    id        TEXT PRIMARY KEY,
    task_id   TEXT NOT NULL,
    title     TEXT NOT NULL,
    completed INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_subtasks_task ON subtasks(task_id);

CREATE TABLE IF NOT EXISTS overrides (
    parent_id TEXT NOT NULL,
    date      TEXT NOT NULL,
    status    TEXT NOT NULL DEFAULT '',
    title     TEXT NOT NULL DEFAULT '',
    body      TEXT NOT NULL DEFAULT '',
    subtasks  TEXT NOT NULL DEFAULT '[]',
    PRIMARY KEY (parent_id, date)
);

CREATE TABLE IF NOT EXISTS habit_completions (
    habit_id  TEXT NOT NULL,
    date      TEXT NOT NULL,
    completed INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (habit_id, date)
);

CREATE VIRTUAL TABLE IF NOT EXISTS entities_fts USING fts5(
    entity_id UNINDEXED,
    title,
    body,
    tokenize='unicode61'
);
`
