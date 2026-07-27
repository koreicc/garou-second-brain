# Contributing to Second Brain System

Thank you for your interest in contributing. This project is a local-first,
single-user PKM system. Contributions that improve reliability, performance,
or the development workflow are welcome.

## Development Workflow

1. Fork the repository and create a feature branch from `main`.
   Use `feat/[name]` or `fix/[name]` naming.
2. Make changes following the coding standards below.
3. Validate your changes (see Validation section).
4. Submit a pull request.

## Coding Standards

### Go

- Go 1.26.5. Use standard library where possible.
- Error wrapping: `fmt.Errorf("context: %w", err)`.
- Error inspection: `errors.Is` / `errors.As`.
- Dependencies on `Server` struct with constructor injection --
  no package globals.
- Per-file mutex locking for vault operations.
- Atomic file writes (temp file + rename).

### Kotlin

- `StateFlow` for UI state. `CoroutineScope` for async operations.
- `@Serializable` from kotlinx.serialization for data classes.
- Match backend JSON structures exactly.
- MVI pattern for screens (ViewModel + sealed events + StateFlow).

### Documentation

- All documentation is in Markdown.
- YAML frontmatter keys use kebab-case.
- No emojis in any project file.

## Validation

### Backend

```sh
gofmt -w .
go build ./...
go vet ./...
go test -race ./...
```

### Frontend

```sh
./gradlew ktlintCheck
./gradlew assembleDebug
```

Do not commit if validation fails.

## Pull Request Process

1. Run all validations locally before pushing.
2. Keep PRs focused on a single concern.
3. Write semantic commit messages:

```
<type>(<scope>): <description>

<body> (optional)
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `style`
Scopes: `api`, `vault`, `model`, `ui`, `data`, `config`, `deps`

4. A maintainer will review your PR. Address review feedback
   with additional commits.
