#!/data/data/com.termux/files/usr/bin/bash
# install.sh -- Quick setup for Second Brain System backend on Termux
# Usage: curl -fsSL https://raw.githubusercontent.com/koreicc/garou-second-brain/main/install.sh | bash
#
# Installs the backend binary via go install, creates the vault directory,
# and adds the binary to PATH in ~/.bashrc.

set -e

BINARY_NAME="second-brain-server"
GO_MODULE="github.com/koreicc/garou-second-brain/backend/cmd/server@latest"
VAULT_PATH="${SECOND_BRAIN_VAULT_PATH:-$HOME/second-brain/vault}"
PORT="${SECOND_BRAIN_PORT:-8080}"

log() {
    printf "\n[install] %s\n" "$1"
}

warn() {
    printf "\n[install][warn] %s\n" "$1" >&2
}

fail() {
    printf "\n[install][error] %s\n" "$1" >&2
    exit 1
}

# --- Preflight: only run inside Termux ---
if [ ! -d "/data/data/com.termux" ]; then
    fail "This script targets Termux on Android. Run it from Termux."
fi

# --- Step 1: install Go if not present ---
if ! command -v go &>/dev/null; then
    log "Go is not installed. Installing golang via pkg..."
    pkg update -y
    pkg install -y golang
else
    log "Go found: $(go version)"
fi

# --- Step 2: install the backend binary ---
log "Installing $BINARY_NAME via 'go install'..."
export GO111MODULE=on
go install "$GO_MODULE"

# Determine GOBIN (where go install placed the binary)
GOBIN="$(go env GOBIN 2>/dev/null)"
if [ -z "$GOBIN" ]; then
    GOBIN="$(go env GOPATH)/bin"
fi

if [ ! -f "$GOBIN/$BINARY_NAME" ]; then
    fail "Binary not found at $GOBIN/$BINARY_NAME after go install."
fi

log "Binary installed: $GOBIN/$BINARY_NAME"

# --- Step 3: add GOBIN to PATH if not already ---
case ":$PATH:" in
    *":$GOBIN:"*) ;;
    *)
        log "Adding $GOBIN to PATH in ~/.bashrc..."
        printf '\n# Added by second-brain install script\nexport PATH="%s:$PATH"\n' "$GOBIN" >> "$HOME/.bashrc"
        export PATH="$GOBIN:$PATH"
        ;;
esac

# --- Step 4: create the vault directory ---
log "Creating vault directory at $VAULT_PATH..."
mkdir -p "$VAULT_PATH/notes" \
         "$VAULT_PATH/tasks" \
         "$VAULT_PATH/quick-tasks" \
         "$VAULT_PATH/people" \
         "$VAULT_PATH/archive"

# --- Step 5: summary ---
log "Setup complete."
printf "\n"
printf "  Binary:  %s\n" "$GOBIN/$BINARY_NAME"
printf "  Vault:   %s\n" "$VAULT_PATH"
printf "  Port:    %s\n" "$PORT"
printf "\n"
printf "Start the backend:\n"
printf "  SECOND_BRAIN_VAULT_PATH=%s SECOND_BRAIN_PORT=%s %s\n" "$VAULT_PATH" "$PORT" "$BINARY_NAME"
printf "\n"
printf "Or set the defaults permanently and just run:\n"
printf "  echo 'export SECOND_BRAIN_VAULT_PATH=%s' >> ~/.bashrc\n" "$VAULT_PATH"
printf "  echo 'export SECOND_BRAIN_PORT=%s' >> ~/.bashrc\n" "$PORT"
printf "  source ~/.bashrc && %s\n" "$BINARY_NAME"
printf "\n"
printf "Using $BINARY_NAME --help for all options.\n"
printf "\n"

# --- Step 6 (optional): start the server immediately ---
if [ "${SECOND_BRAIN_AUTOSTART:-0}" = "1" ]; then
    log "SECOND_BRAIN_AUTOSTART=1 -- starting server now (Ctrl+C to stop)..."
    export SECOND_BRAIN_VAULT_PATH="$VAULT_PATH"
    export SECOND_BRAIN_PORT="$PORT"
    exec "$GOBIN/$BINARY_NAME"
fi
