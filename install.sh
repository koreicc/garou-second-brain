#!/data/data/com.termux/files/usr/bin/bash
# install.sh -- Quick setup for Second Brain System backend on Termux
# Usage: curl -fsSL https://raw.githubusercontent.com/koreicc/garou-second-brain/main/install.sh | bash
#
# Downloads the pre-built binary from GitHub Releases, installs it to
# ~/.local/bin, and creates the vault directory.
# If the download fails, set SECOND_BRAIN_BUILD_FROM_SOURCE=1 to build
# from source instead (requires git + golang).

set -e

BINARY_NAME="second-brain-server"
REPO="koreicc/garou-second-brain"
VAULT_PATH="${SECOND_BRAIN_VAULT_PATH:-$HOME/second-brain/vault}"
PORT="${SECOND_BRAIN_PORT:-8080}"
TARGET_DIR="${HOME}/.local/bin"

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

# --- Preflight ---
if [ ! -d "/data/data/com.termux" ]; then
    fail "This script targets Termux on Android. Run it from Termux."
fi

# Refresh package index before any install
pkg update -y

if ! command -v curl &>/dev/null; then
    log "curl not found, installing..."
    pkg install -y curl
fi

# --- Create target directory ---
mkdir -p "$TARGET_DIR"

# --- Download or build ---
if [ "${SECOND_BRAIN_BUILD_FROM_SOURCE:-0}" = "1" ]; then
    # --- Build from source ---
    log "SECOND_BRAIN_BUILD_FROM_SOURCE=1 -- building from source..."

    if ! command -v git &>/dev/null; then
        log "git not found, installing..."
        pkg install -y git
    fi
    if ! command -v go &>/dev/null; then
        log "go not found, installing..."
        pkg install -y golang
    fi

    BUILD_DIR=$(mktemp -d)
    log "Cloning repository (shallow)..."
    git clone --depth 1 "https://github.com/$REPO.git" "$BUILD_DIR"
    cd "$BUILD_DIR/backend"
    log "Building binary..."
    go build -o "$TARGET_DIR/$BINARY_NAME" ./cmd/server
    rm -rf "$BUILD_DIR"
    log "Binary built: $TARGET_DIR/$BINARY_NAME"
else
    # --- Download pre-built binary ---
    DOWNLOAD_URL="https://github.com/$REPO/releases/download/nightly/second-brain-server-arm64"
    log "Downloading $BINARY_NAME (ARM64) from GitHub Releases..."
    log "URL: $DOWNLOAD_URL"

    set +e
    HTTP_CODE=$(curl -fSL --connect-timeout 10 --max-time 60 \
        -w "%{http_code}" \
        -o "$TARGET_DIR/$BINARY_NAME" \
        "$DOWNLOAD_URL" 2>&1)
    CURL_EXIT=$?
    set -e

    if [ "$CURL_EXIT" -ne 0 ] || [ "$HTTP_CODE" != "200" ]; then
        warn "Download failed (HTTP $HTTP_CODE, curl exit $CURL_EXIT)."
        warn "Either the nightly release hasn't built yet, or there's a network issue."
        warn ""
        warn "Try these:"
        warn "  1. Wait for the GitHub Actions workflow to finish, then retry."
        warn "     Check: https://github.com/$REPO/actions"
        warn ""
        warn "  2. Build from source instead:"
        warn "     curl -fsSL https://raw.githubusercontent.com/$REPO/main/install.sh | SECOND_BRAIN_BUILD_FROM_SOURCE=1 bash"
        warn ""
        warn "  3. Or manually download and install:"
        warn "     mkdir -p $TARGET_DIR"
        warn "     curl -fSL -o $TARGET_DIR/$BINARY_NAME $DOWNLOAD_URL"
        warn "     chmod +x $TARGET_DIR/$BINARY_NAME"
        fail "Aborting."
    fi

    chmod +x "$TARGET_DIR/$BINARY_NAME"
    log "Binary installed: $TARGET_DIR/$BINARY_NAME ($(du -h "$TARGET_DIR/$BINARY_NAME" | cut -f1))"
fi

# --- Add TARGET_DIR to PATH ---
case ":$PATH:" in
    *":$TARGET_DIR:"*) ;;
    *)
        log "Adding $TARGET_DIR to PATH in ~/.bashrc..."
        printf '\n# Added by second-brain install script\nexport PATH="$PATH:%s"\n' "$TARGET_DIR" >> "$HOME/.bashrc"
        export PATH="$PATH:$TARGET_DIR"
        log "Run 'source ~/.bashrc' or open a new shell to use the binary directly."
        ;;
esac

# --- Create vault directory ---
log "Creating vault directory at $VAULT_PATH..."
mkdir -p "$VAULT_PATH/notes" \
         "$VAULT_PATH/tasks" \
         "$VAULT_PATH/quick-tasks" \
         "$VAULT_PATH/people" \
         "$VAULT_PATH/archive"

# --- Summary ---
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

# --- Optional: start server ---
if [ "${SECOND_BRAIN_AUTOSTART:-0}" = "1" ]; then
    log "SECOND_BRAIN_AUTOSTART=1 -- starting server now (Ctrl+C to stop)..."
    export SECOND_BRAIN_VAULT_PATH="$VAULT_PATH"
    export SECOND_BRAIN_PORT="$PORT"
    exec "$BINARY_NAME"
fi
