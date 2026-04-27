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

NETWORK_NAME="demo1-local-ocp-net"
PROM_CONTAINER="prometheus-local-ocp"
GRAFANA_CONTAINER="grafana-local-ocp"
PROM_PORT="${PROM_PORT:-19090}"
GRAFANA_PORT="${GRAFANA_PORT:-13000}"

$CONTAINER_BIN network exists "$NETWORK_NAME" >/dev/null 2>&1 || $CONTAINER_BIN network create "$NETWORK_NAME" >/dev/null

$CONTAINER_BIN rm -f "$PROM_CONTAINER" "$GRAFANA_CONTAINER" >/dev/null 2>&1 || true

$CONTAINER_BIN run -d \
  --name "$PROM_CONTAINER" \
  --network "$NETWORK_NAME" \
  -p "${PROM_PORT}:9090" \
  -v "$(pwd)/monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:Z" \
  docker.io/prom/prometheus:v2.54.1 \
  --config.file=/etc/prometheus/prometheus.yml

$CONTAINER_BIN run -d \
  --name "$GRAFANA_CONTAINER" \
  --network "$NETWORK_NAME" \
  -p "${GRAFANA_PORT}:3000" \
  -e GF_SECURITY_ADMIN_USER=admin \
  -e GF_SECURITY_ADMIN_PASSWORD=admin \
  -v "$(pwd)/monitoring/grafana/provisioning/datasources:/etc/grafana/provisioning/datasources:Z" \
  -v "$(pwd)/monitoring/grafana/provisioning/dashboards:/etc/grafana/provisioning/dashboards:Z" \
  docker.io/grafana/grafana:11.1.4

echo "Prometheus: http://localhost:${PROM_PORT}"
echo "Grafana:    http://localhost:${GRAFANA_PORT}  (admin/admin)"
echo "Dashboard:  demo1 Local OCP Overview"
