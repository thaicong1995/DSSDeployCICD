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

if ! CONTAINER_BIN="$(find_podman)"; then
  echo "Khong tim thay podman trong PATH. Hay set PODMAN_BIN=/duong-dan/podman" >&2
  exit 1
fi

if [ ! -f target/demo1-0.0.1-SNAPSHOT.jar ]; then
  echo "Chua co target/demo1-0.0.1-SNAPSHOT.jar. Hay build truoc bang ./mvnw -DskipTests package" >&2
  exit 1
fi

IMAGE_NAME="demo1:local-ocp"
CONTAINER_NAME="demo1-local-ocp"
NETWORK_NAME="demo1-local-ocp-net"

echo "Starting demo1 with OCP-like limits: 2.5G RAM, 2 CPU"
$CONTAINER_BIN network exists "$NETWORK_NAME" >/dev/null 2>&1 || $CONTAINER_BIN network create "$NETWORK_NAME" >/dev/null
$CONTAINER_BIN build -t "$IMAGE_NAME" .
$CONTAINER_BIN rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
$CONTAINER_BIN run -d \
  --name "$CONTAINER_NAME" \
  --network "$NETWORK_NAME" \
  --memory 2560m \
  --cpus 2 \
  --restart unless-stopped \
  -p 8080:8080 \
  "$IMAGE_NAME"

echo
echo "Container status:"
$CONTAINER_BIN ps --filter "name=$CONTAINER_NAME"

echo
echo "Follow logs:"
echo "  ./scripts/watch-local-ocp.sh logs"
