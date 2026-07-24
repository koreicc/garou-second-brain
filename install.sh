#!/bin/sh
# install.sh -- Quick setup for Second Brain System backend
# Usage: curl -fsSL https://raw.githubusercontent.com/koreicc/garou-second-brain/main/install.sh | sh
#
# Downloads the pre-built binary from GitHub Releases, installs it to
# ~/.local/bin, and creates the vault directory.
# Set SECOND_BRAIN_BUILD_FROM_SOURCE=1 to build from source instead.

set -e

BINARY_NAME="second-brain-server"
REPO="koreicc/garou-second-brain"
VAULT_PATH="${SECOND_BRAIN_VAULT_PATH:-$HOME/second-brain/vault}"
PORT="${SECOND_BRAIN_PORT:-8080}"
TARGET_DIR="${HOME}/.local/bin"

log() { printf "\n[install] %s\n" "$1"; }
warn() { printf "\n[install][warn] %s\n" "$1" >&2; }
fail() { printf "\n[install][error] %s\n" "$1" >&2; exit 1; }

# --- Detect Termux ---
IS_TERMUX=0
[ -d "/data/data/com.termux" ] && IS_TERMUX=1

if [ "$IS_TERMUX" = "1" ]; then
    pkg update -y 2>/dev/null || true
    command -v curl >/dev/null 2>&1 || pkg install -y curl
fi

mkdir -p "$TARGET_DIR"

# --- Download or build ---
if [ "${SECOND_BRAIN_BUILD_FROM_SOURCE:-0}" = "1" ]; then
    log "Building from source..."

    if [ "$IS_TERMUX" = "1" ]; then
        command -v git >/dev/null 2>&1 || pkg install -y git
        command -v go >/dev/null 2>&1 || pkg install -y golang
    else
        command -v git >/dev/null 2>&1 || fail "git not found. Install git first."
        command -v go >/dev/null 2>&1 || fail "go not found. Install Go first."
    fi

    BRANCH="${SECOND_BRAIN_BRANCH:-main}"
    BUILD_DIR=$(mktemp -d)
    log "Cloning $BRANCH (shallow)..."
    git clone --depth 1 -b "$BRANCH" "https://github.com/$REPO.git" "$BUILD_DIR"

    log "Building binary..."
    cd "$BUILD_DIR/backend"
    CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o "$TARGET_DIR/$BINARY_NAME" ./cmd/server
    cd /
    rm -rf "$BUILD_DIR"

    chmod +x "$TARGET_DIR/$BINARY_NAME"
    log "Binary built: $TARGET_DIR/$BINARY_NAME"
else
    DOWNLOAD_URL="https://github.com/$REPO/releases/download/nightly/second-brain-server-arm64"
    log "Downloading $BINARY_NAME from GitHub Releases..."

    curl -fSL --connect-timeout 10 --max-time 120 \
        -o "$TARGET_DIR/$BINARY_NAME" "$DOWNLOAD_URL"

    if [ ! -s "$TARGET_DIR/$BINARY_NAME" ]; then
        rm -f "$TARGET_DIR/$BINARY_NAME"
        warn "Download failed. The binary may not exist yet."
        warn ""
        warn "Options:"
        warn "  1. Check if the nightly build exists:"
        warn "     https://github.com/$REPO/actions"
        warn ""
        warn "  2. Build from source:"
        warn "     curl -fsSL https://raw.githubusercontent.com/$REPO/main/install.sh | SECOND_BRAIN_BUILD_FROM_SOURCE=1 sh"
        warn ""
        warn "  3. Build a specific branch:"
        warn "     curl -fsSL https://raw.githubusercontent.com/$REPO/main/install.sh | SECOND_BRAIN_BUILD_FROM_SOURCE=1 SECOND_BRAIN_BRANCH=feat/my-branch sh"
        fail "Aborting."
    fi

    chmod +x "$TARGET_DIR/$BINARY_NAME"
    log "Binary installed: $TARGET_DIR/$BINARY_NAME"
fi

# --- Add to PATH ---
add_to_path() {
    target_file="$1"
    if [ -f "$target_file" ] && grep -q "$TARGET_DIR" "$target_file" 2>/dev/null; then
        return
    fi
    log "Adding $TARGET_DIR to PATH in $target_file..."
    printf '\n# Added by second-brain install script\nexport PATH="%s:$PATH"\n' "$TARGET_DIR" >> "$target_file"
}

case ":$PATH:" in
    *":$TARGET_DIR:"*) ;;
    *)
        if [ -n "$ZSH_VERSION" ] || [ -f "$HOME/.zshrc" ]; then
            add_to_path "$HOME/.zshrc"
        fi
        if [ -f "$HOME/.bashrc" ]; then
            add_to_path "$HOME/.bashrc"
        fi
        export PATH="$TARGET_DIR:$PATH"
        log "Run 'source ~/.bashrc' (or ~/.zshrc) or open a new shell."
        ;;
esac

# --- Create vault directory ---
log "Creating vault at $VAULT_PATH..."
mkdir -p "$VAULT_PATH/notes" \
         "$VAULT_PATH/tasks" \
         "$VAULT_PATH/quick-tasks" \
         "$VAULT_PATH/people" \
         "$VAULT_PATH/archive"

# --- Summary ---
log "Setup complete."
printf "\n  Binary:  %s\n" "$TARGET_DIR/$BINARY_NAME"
printf "  Vault:   %s\n" "$VAULT_PATH"
printf "  Port:    %s\n" "$PORT"
printf "\nStart: %s\n\n" "$BINARY_NAME"

# --- Optional: start server ---
if [ "${SECOND_BRAIN_AUTOSTART:-0}" = "1" ]; then
    log "Starting server (Ctrl+C to stop)..."
    export SECOND_BRAIN_VAULT_PATH="$VAULT_PATH"
    export SECOND_BRAIN_PORT="$PORT"
    exec "$BINARY_NAME"
fi
