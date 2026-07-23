#!/data/data/com.termux/files/usr/bin/bash
# install.sh -- Quick setup for Second Brain System backend on Termux
# Usage: curl -fsSL https://raw.githubusercontent.com/koreicc/garou-second-brain/main/install.sh | bash
#
# Downloads the pre-built binary from GitHub Releases, installs it to PREFIX/bin,
# and creates the vault directory.

set -e

BINARY_NAME="second-brain-server"
REPO="koreicc/garou-second-brain"
VAULT_PATH="${SECOND_BRAIN_VAULT_PATH:-$HOME/second-brain/vault}"
PORT="${SECOND_BRAIN_PORT:-8080}"
TARGET_DIR="$PREFIX/bin"

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

if ! command -v curl &>/dev/null; then
    log "curl not found, installing..."
    pkg update -y
    pkg install -y curl
fi

# --- Step 1: download the ARM64 binary from GitHub Releases ---
log "Downloading $BINARY_NAME (ARM64) from GitHub Releases..."
DOWNLOAD_URL="https://github.com/$REPO/releases/download/nightly/second-brain-server-arm64"

HTTP_CODE=$(curl -fsSL -w "%{http_code}" -o "$TARGET_DIR/$BINARY_NAME" "$DOWNLOAD_URL" 2>&1) || true

if [ "$HTTP_CODE" != "200" ] || [ ! -f "$TARGET_DIR/$BINARY_NAME" ]; then
    fail "Failed to download binary from $DOWNLOAD_URL (HTTP $HTTP_CODE).\n  The nightly release may not be built yet. Wait for the GitHub Actions workflow to finish.\n  Retry after a minute: curl -fsSL https://raw.githubusercontent.com/$REPO/main/install.sh | bash"
fi

chmod +x "$TARGET_DIR/$BINARY_NAME"
log "Binary installed: $TARGET_DIR/$BINARY_NAME"

# --- Step 2: ensure TARGET_DIR is in PATH ---
case ":$PATH:" in
    *":$TARGET_DIR:"*) ;;
    *)
        log "Adding $TARGET_DIR to PATH in ~/.bashrc..."
        printf '\n# Added by second-brain install script\nexport PATH="$PATH:%s"\n' "$TARGET_DIR" >> "$HOME/.bashrc"
        export PATH="$PATH:$TARGET_DIR"
        ;;
esac

# --- Step 3: create the vault directory ---
log "Creating vault directory at $VAULT_PATH..."
mkdir -p "$VAULT_PATH/notes" \
         "$VAULT_PATH/tasks" \
         "$VAULT_PATH/quick-tasks" \
         "$VAULT_PATH/people" \
         "$VAULT_PATH/archive"

# --- Step 4: summary ---
log "Setup complete."
printf "\n"
printf "  Binary:  %s\n" "$TARGET_DIR/$BINARY_NAME"
printf "  Vault:   %s\n" "$VAULT_PATH"
printf "  Port:    %s\n" "$PORT"
printf "\n"
printf "Start the backend:\n"
printf "  SECOND_BRAIN_VAULT_PATH=%s SECOND_BRAIN_PORT=%s %s\n" "$VAULT_PATH" "$PORT" "$BINARY_NAME"
printf "\n"
printf "Or set defaults permanently:\n"
printf "  echo 'export SECOND_BRAIN_VAULT_PATH=%s' >> ~/.bashrc\n" "$VAULT_PATH"
printf "  echo 'export SECOND_BRAIN_PORT=%s' >> ~/.bashrc\n" "$PORT"
printf "  source ~/.bashrc && %s\n" "$BINARY_NAME"
printf "\n"

# --- Step 5 (optional): start the server immediately ---
if [ "${SECOND_BRAIN_AUTOSTART:-0}" = "1" ]; then
    log "SECOND_BRAIN_AUTOSTART=1 -- starting server now (Ctrl+C to stop)..."
    export SECOND_BRAIN_VAULT_PATH="$VAULT_PATH"
    export SECOND_BRAIN_PORT="$PORT"
    exec "$BINARY_NAME"
fi
