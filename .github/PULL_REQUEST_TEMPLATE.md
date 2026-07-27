## Description

Please include a summary of the change and which issue is fixed.

Fixes # (issue)

## Type of change

- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Checklist

- [ ] Backend: `gofmt -w .` && `go build ./...` && `go vet ./...` && `go test -race ./...`
- [ ] Frontend: `./gradlew ktlintCheck` && `./gradlew assembleDebug`
- [ ] Commit messages follow semantic format
- [ ] No TODO/FIXME comments left in code
- [ ] No secrets, tokens, or absolute paths committed

## Testing

Describe the tests you ran to verify your changes.
