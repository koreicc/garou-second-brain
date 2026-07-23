#!/data/data/com.termux/files/usr/bin/bash
# install.sh -- Quick setup for Second Brain System backend on Termux
# Usage: curl -fsSL https://raw.githubusercontent.com/koreicc/garou-second-brain/main/install.sh | bash

set -e

REPO_URL="https://github.com/koreicc/garou-second-brain.git"
INSTALL_DIR="${SECOND_BRAIN_INSTALL_DIR:-$HOME/garou-second-brain}"
VAULT_PATH="${SECOND_BRAIN_VAULT_PATH:-$HOME/second-brain/vault}"
PORT="${SECOND_BRAIN_PORT:-8080}"

log() {
    printf "\n[install] %s\n" "$1"
}

fail() {
    printf "\n[install][error] %s\n" "$1" >&2
    exit 1
}

# --- Preflight: only run inside Termux ---
if [ ! -d "/data/data/com.termux" ]; then
    fail "This script targets Termux on Android. Run it from Termux."
fi

# --- Step 1: update packages and install Go ---
log "Updating package list and installing dependencies..."
pkg update -y
pkg install -y git golang

# --- Step 2: clone (or update) the repository ---
if [ -d "$INSTALL_DIR/.git" ]; then
    log "Existing repo found at $INSTALL_DIR -- pulling latest..."
    git -C "$INSTALL_DIR" fetch --all --prune
    git -C "$INSTALL_DIR" checkout main
    git -C "$INSTALL_DIR" pull --ff-only
else
    log "Cloning repository into $INSTALL_DIR..."
    git clone --depth 1 "$REPO_URL" "$INSTALL_DIR"
fi

# --- Step 3: create the vault directory ---
log "Creating vault directory at $VAULT_PATH..."
mkdir -p "$VAULT_PATH/notes" \
         "$VAULT_PATH/tasks" \
         "$VAULT_PATH/quick-tasks" \
         "$VAULT_PATH/people" \
         "$VAULT_PATH/archive"

# --- Step 4: download Go dependencies ---
log "Downloading Go modules..."
cd "$INSTALL_DIR/backend"
go mod tidy

# --- Step 5: summary and run instructions ---
log "Setup complete."
printf "\n"
printf "  Repo:   %s\n" "$INSTALL_DIR"
printf "  Vault:  %s\n" "$VAULT_PATH"
printf "  Port:   %s\n" "$PORT"
printf "\n"
printf "Start the backend:\n"
printf "  cd %s/backend\n" "$INSTALL_DIR"
printf "  SECOND_BRAIN_VAULT_PATH=%s SECOND_BRAIN_PORT=%s go run ./cmd/server\n" "$VAULT_PATH" "$PORT"
printf "\n"
printf "Then build the Android APK from another machine:\n"
printf "  cd %s/android && ./gradlew assembleDebug\n" "$INSTALL_DIR"
printf "\n"

# --- Step 6 (optional): start the server immediately ---
if [ "${SECOND_BRAIN_AUTOSTART:-0}" = "1" ]; then
    log "SECOND_BRAIN_AUTOSTART=1 -- starting server now (Ctrl+C to stop)..."
    export SECOND_BRAIN_VAULT_PATH="$VAULT_PATH"
    export SECOND_BRAIN_PORT="$PORT"
    go run ./cmd/server
fi
