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

CONTAINER_NAME="demo1-local-ocp"
MODE="${1:-overview}"

case "$MODE" in
  logs)
    exec $CONTAINER_BIN logs -f "$CONTAINER_NAME"
    ;;
  stats)
    exec $CONTAINER_BIN stats "$CONTAINER_NAME"
    ;;
  restart)
    exec $CONTAINER_BIN inspect "$CONTAINER_NAME" --format='RestartCount={{.RestartCount}} Status={{.State.Status}} OOMKilled={{.State.OOMKilled}} ExitCode={{.State.ExitCode}}'
    ;;
  health)
    exec curl -fsS http://localhost:8080/actuator/health
    ;;
  overview)
    $CONTAINER_BIN ps --filter "name=$CONTAINER_NAME"
    echo
    $CONTAINER_BIN inspect "$CONTAINER_NAME" --format='RestartCount={{.RestartCount}} Status={{.State.Status}} OOMKilled={{.State.OOMKilled}} ExitCode={{.State.ExitCode}}'
    echo
    echo "Use one of:"
    echo "  ./scripts/watch-local-ocp.sh logs"
    echo "  ./scripts/watch-local-ocp.sh stats"
    echo "  ./scripts/watch-local-ocp.sh restart"
    echo "  ./scripts/watch-local-ocp.sh health"
    ;;
  *)
    echo "Unknown mode: $MODE" >&2
    echo "Usage: ./scripts/watch-local-ocp.sh [overview|logs|stats|restart|health]" >&2
    exit 1
    ;;
esac
