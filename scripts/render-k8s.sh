#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

APP_NAME="${APP_NAME:-demo1}"
NAMESPACE="${NAMESPACE:-demo1-dev}"
APP_PORT="${APP_PORT:-8080}"
REPLICAS="${REPLICAS:-1}"
MIN_REPLICAS="${MIN_REPLICAS:-1}"
MAX_REPLICAS="${MAX_REPLICAS:-3}"
CPU_TARGET_UTILIZATION="${CPU_TARGET_UTILIZATION:-25}"
CPU_REQUEST="${CPU_REQUEST:-500m}"
CPU_LIMIT="${CPU_LIMIT:-2}"
MEMORY_REQUEST="${MEMORY_REQUEST:-1024Mi}"
MEMORY_LIMIT="${MEMORY_LIMIT:-2560Mi}"
IMAGE="${IMAGE:-registry.local/demo:${IMAGE_TAG:-local}}"
IMAGE_PULL_POLICY="${IMAGE_PULL_POLICY:-Always}"
INGRESS_ENABLED="${INGRESS_ENABLED:-true}"
INGRESS_HOST="${INGRESS_HOST:-demo1.apps.example.local}"
INGRESS_CLASS_NAME="${INGRESS_CLASS_NAME:-nginx}"
ROUTE_ENABLED="${ROUTE_ENABLED:-false}"
ROUTE_HOST="${ROUTE_HOST:-demo1.apps.example.local}"
IMAGE_PULL_SECRET="${IMAGE_PULL_SECRET:-}"
SERVICE_ACCOUNT_NAME="${SERVICE_ACCOUNT_NAME:-demo1}"
SERVICE_TYPE="${SERVICE_TYPE:-ClusterIP}"
SERVICE_NODE_PORT="${SERVICE_NODE_PORT:-}"
CONFIG_KEYSTORE_TEST="${CONFIG_KEYSTORE_TEST:-testsign}"
SECRET_KEYSTORE_PASSWORD="${SECRET_KEYSTORE_PASSWORD:-}"

mkdir -p .rendered

sed \
  -e "s|__APP_NAME__|$APP_NAME|g" \
  -e "s|__NAMESPACE__|$NAMESPACE|g" \
  -e "s|__IMAGE__|$IMAGE|g" \
  -e "s|__IMAGE_PULL_POLICY__|$IMAGE_PULL_POLICY|g" \
  -e "s|__APP_PORT__|$APP_PORT|g" \
  -e "s|__REPLICAS__|$REPLICAS|g" \
  -e "s|__CPU_REQUEST__|$CPU_REQUEST|g" \
  -e "s|__CPU_LIMIT__|$CPU_LIMIT|g" \
  -e "s|__MEMORY_REQUEST__|$MEMORY_REQUEST|g" \
  -e "s|__MEMORY_LIMIT__|$MEMORY_LIMIT|g" \
  -e "s|__SERVICE_ACCOUNT_NAME__|$SERVICE_ACCOUNT_NAME|g" \
  k8s/deployment.yaml > .rendered/deployment.yaml

sed \
  -e "s|__APP_NAME__|$APP_NAME|g" \
  -e "s|__NAMESPACE__|$NAMESPACE|g" \
  -e "s|__MIN_REPLICAS__|$MIN_REPLICAS|g" \
  -e "s|__MAX_REPLICAS__|$MAX_REPLICAS|g" \
  -e "s|__CPU_TARGET_UTILIZATION__|$CPU_TARGET_UTILIZATION|g" \
  k8s/hpa.yaml > .rendered/hpa.yaml

awk -v secret="$IMAGE_PULL_SECRET" '
  /__IMAGE_PULL_SECRETS_BLOCK__/ {
    if (secret != "") {
      print "      imagePullSecrets:"
      print "        - name: " secret
    }
    next
  }
  { print }
' .rendered/deployment.yaml > .rendered/deployment.yaml.tmp && mv .rendered/deployment.yaml.tmp .rendered/deployment.yaml

sed \
  -e "s|__APP_NAME__|$APP_NAME|g" \
  -e "s|__NAMESPACE__|$NAMESPACE|g" \
  -e "s|__SERVICE_TYPE__|$SERVICE_TYPE|g" \
  -e "s|__APP_PORT__|$APP_PORT|g" \
  k8s/service.yaml > .rendered/service.yaml

awk -v node_port="$SERVICE_NODE_PORT" '
  /__SERVICE_NODE_PORT_BLOCK__/ {
    if (node_port != "") {
      print "      nodePort: " node_port
    }
    next
  }
  { print }
' .rendered/service.yaml > .rendered/service.yaml.tmp && mv .rendered/service.yaml.tmp .rendered/service.yaml

sed \
  -e "s|__APP_NAME__|$APP_NAME|g" \
  -e "s|__NAMESPACE__|$NAMESPACE|g" \
  k8s/servicemonitor.yaml > .rendered/servicemonitor.yaml

sed \
  -e "s|__APP_NAME__|$APP_NAME|g" \
  -e "s|__NAMESPACE__|$NAMESPACE|g" \
  -e "s|__CONFIG_KEYSTORE_TEST__|$CONFIG_KEYSTORE_TEST|g" \
  k8s/configmap.yaml > .rendered/configmap.yaml

SECRET_KEYSTORE_PASSWORD_B64="$(printf '%s' "$SECRET_KEYSTORE_PASSWORD" | base64 -w0)"
sed \
  -e "s|__APP_NAME__|$APP_NAME|g" \
  -e "s|__NAMESPACE__|$NAMESPACE|g" \
  -e "s|__SECRET_KEYSTORE_PASSWORD_B64__|$SECRET_KEYSTORE_PASSWORD_B64|g" \
  k8s/secret.yaml > .rendered/secret.yaml

sed \
  -e "s|__APP_NAME__|$APP_NAME|g" \
  -e "s|__NAMESPACE__|$NAMESPACE|g" \
  -e "s|__SERVICE_ACCOUNT_NAME__|$SERVICE_ACCOUNT_NAME|g" \
  k8s/serviceaccount.yaml > .rendered/serviceaccount.yaml

awk -v secret="$IMAGE_PULL_SECRET" '
  /__IMAGE_PULL_SECRETS_BLOCK__/ {
    if (secret != "") {
      print "imagePullSecrets:"
      print "  - name: " secret
    }
    next
  }
  { print }
' .rendered/serviceaccount.yaml > .rendered/serviceaccount.yaml.tmp && mv .rendered/serviceaccount.yaml.tmp .rendered/serviceaccount.yaml

if [ "$INGRESS_ENABLED" = "true" ]; then
  sed \
    -e "s|__APP_NAME__|$APP_NAME|g" \
    -e "s|__NAMESPACE__|$NAMESPACE|g" \
    -e "s|__APP_PORT__|$APP_PORT|g" \
    -e "s|__INGRESS_HOST__|$INGRESS_HOST|g" \
    -e "s|__INGRESS_CLASS_NAME__|$INGRESS_CLASS_NAME|g" \
    k8s/ingress.yaml > .rendered/ingress.yaml
fi

if [ "$ROUTE_ENABLED" = "true" ]; then
  sed \
    -e "s|__APP_NAME__|$APP_NAME|g" \
    -e "s|__NAMESPACE__|$NAMESPACE|g" \
    -e "s|__APP_PORT__|$APP_PORT|g" \
    -e "s|__ROUTE_HOST__|$ROUTE_HOST|g" \
    k8s/route.yaml > .rendered/route.yaml
fi

echo "Rendered files:"
echo "  .rendered/deployment.yaml"
echo "  .rendered/service.yaml"
echo "  .rendered/servicemonitor.yaml"
echo "  .rendered/configmap.yaml"
echo "  .rendered/secret.yaml"
echo "  .rendered/serviceaccount.yaml"
echo "  .rendered/hpa.yaml"
if [ "$INGRESS_ENABLED" = "true" ]; then
  echo "  .rendered/ingress.yaml"
fi
if [ "$ROUTE_ENABLED" = "true" ]; then
  echo "  .rendered/route.yaml"
fi
