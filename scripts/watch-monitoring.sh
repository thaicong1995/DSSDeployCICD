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

MODE="${1:-overview}"
PROM_PORT="${PROM_PORT:-19090}"
GRAFANA_PORT="${GRAFANA_PORT:-13000}"

case "$MODE" in
  overview)
    $CONTAINER_BIN ps --filter "name=local-ocp"
    echo
    echo "Grafana: http://localhost:${GRAFANA_PORT}"
    echo "Prometheus: http://localhost:${PROM_PORT}"
    ;;
  grafana-logs)
    exec $CONTAINER_BIN logs -f grafana-local-ocp
    ;;
  prometheus-logs)
    exec $CONTAINER_BIN logs -f prometheus-local-ocp
    ;;
  *)
    echo "Usage: ./scripts/watch-monitoring.sh [overview|grafana-logs|prometheus-logs]" >&2
    exit 1
    ;;
esac
