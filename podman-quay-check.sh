#!/usr/bin/env bash
set -euo pipefail

PODMAN_BIN="${PODMAN_BIN:-podman}"
if [[ -x "/opt/podman/bin/podman" ]]; then
  PODMAN_BIN="/opt/podman/bin/podman"
fi
export PATH="$(dirname "$PODMAN_BIN"):$PATH"

COMPOSE_PROVIDER="${PODMAN_COMPOSE_PROVIDER:-}"
if [[ -z "$COMPOSE_PROVIDER" ]]; then
  if command -v podman-compose >/dev/null 2>&1; then
    COMPOSE_PROVIDER="$(command -v podman-compose)"
  elif [[ -x "$HOME/Library/Python/3.9/bin/podman-compose" ]]; then
    COMPOSE_PROVIDER="$HOME/Library/Python/3.9/bin/podman-compose"
  elif [[ -x "/opt/homebrew/bin/podman-compose" ]]; then
    COMPOSE_PROVIDER="/opt/homebrew/bin/podman-compose"
  fi
fi

if [[ -n "$COMPOSE_PROVIDER" ]]; then
  export PODMAN_COMPOSE_PROVIDER="$COMPOSE_PROVIDER"
  export PATH="$(dirname "$COMPOSE_PROVIDER"):$PATH"
else
  echo "FAIL: compose provider not found. Install podman-compose first."
  exit 1
fi

WORKDIR="$(mktemp -d /tmp/podman-quay-check.XXXXXX)"
COMPOSE_FILE="$WORKDIR/compose.yml"

cleanup() {
  "$PODMAN_BIN" compose -f "$COMPOSE_FILE" down -v >/dev/null 2>&1 || true
  rm -rf "$WORKDIR"
}
trap cleanup EXIT

cat >"$COMPOSE_FILE" <<'YAML'
services:
  check:
    image: quay.io/libpod/alpine:latest
    command: ["sh", "-c", "echo quay-check-ok && sleep 20"]
YAML

echo "[1/4] Ensure podman machine is running..."
"$PODMAN_BIN" machine start >/dev/null 2>&1 || true

echo "[2/4] Start compose service (quay.io only)..."
"$PODMAN_BIN" compose -f "$COMPOSE_FILE" up -d

echo "[3/4] Verify container status..."
"$PODMAN_BIN" compose -f "$COMPOSE_FILE" ps

echo "[4/4] Verify logs include quay-check-ok..."
if "$PODMAN_BIN" compose -f "$COMPOSE_FILE" logs check | grep -q "quay-check-ok"; then
  echo "PASS: Podman compose test succeeded with quay.io image."
else
  echo "FAIL: expected log marker not found."
  exit 1
fi
