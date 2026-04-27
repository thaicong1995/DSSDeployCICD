#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

find_podman() {
  if [ -n "${PODMAN_BIN:-}" ]; then
    printf '%s\n' "$PODMAN_BIN"
    return 0
  fi

  for candidate in podman /usr/bin/podman /usr/local/bin/podman /snap/bin/podman; do
    if command -v "$candidate" >/dev/null 2>&1; then
      command -v "$candidate"
      return 0
    fi
    if [ -x "$candidate" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  return 1
}

echo "Checking local runtime for Podman flow"
echo

if PODMAN_PATH="$(find_podman)"; then
  echo "[OK] podman: $PODMAN_PATH"
  "$PODMAN_PATH" --version
else
  echo "[FAIL] podman khong co trong PATH"
  echo "       Co the set: export PODMAN_BIN=/duong-dan/podman"
fi

if [ -f target/demo1-0.0.1-SNAPSHOT.jar ]; then
  echo "[OK] jar: target/demo1-0.0.1-SNAPSHOT.jar"
else
  echo "[FAIL] thieu target/demo1-0.0.1-SNAPSHOT.jar"
  echo "      Build bang: ./mvnw -DskipTests package"
fi

if [ -f Dockerfile ]; then
  echo "[OK] Dockerfile"
else
  echo "[FAIL] thieu Dockerfile"
fi
