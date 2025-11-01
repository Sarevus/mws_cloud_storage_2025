#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${ROOT_DIR}/docker-compose.ceph.yml}"
SERVICE_NAME="${CEPH_SERVICE_NAME:-ceph-all-in-one}"
ENV_FILE="${ENV_FILE:-${ROOT_DIR}/.env}"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
else
  echo "[WARN] Файл окружения ${ENV_FILE} не найден. Используются значения по умолчанию." >&2
fi

RGW_USER="${CEPH_RGW_USER:-mws-backend}"
RGW_BUCKET="${CEPH_RGW_BUCKET:-mws-user-files}"
RGW_REALM="${CEPH_RGW_REALM:-default}"
RGW_ZONEGROUP="${CEPH_RGW_ZONEGROUP:-default}"
RGW_ZONE="${CEPH_RGW_ZONE:-default}"

compose() {
  docker compose -f "${COMPOSE_FILE}" "$@"
}

ensure_service_running() {
  if ! compose ps --status running --services | grep -qx "${SERVICE_NAME}"; then
    echo "[ERROR] Сервис ${SERVICE_NAME} не запущен. Выполните 'docker compose -f ${COMPOSE_FILE} up -d'" >&2
    exit 1
  fi
}

json_cmd() {
  compose exec "${SERVICE_NAME}" "$@"
}

ensure_service_running

echo "[INFO] Создание/получение S3-пользователя '${RGW_USER}'..."
USER_JSON=$(json_cmd radosgw-admin user info --uid="${RGW_USER}" 2>/dev/null || true)
if [[ -z "${USER_JSON}" ]]; then
  USER_JSON=$(json_cmd radosgw-admin user create \
    --uid="${RGW_USER}" \
    --display-name="${RGW_USER}" \
    --system | tr -d '\r')
  echo "[INFO] Пользователь '${RGW_USER}' создан."
else
  echo "[INFO] Пользователь '${RGW_USER}' уже существует."
fi

ACCESS_KEY=$(python3 -c "import json,sys; data=json.load(sys.stdin); print(data['keys'][0]['access_key'])" <<<"${USER_JSON}")
SECRET_KEY=$(python3 -c "import json,sys; data=json.load(sys.stdin); print(data['keys'][0]['secret_key'])" <<<"${USER_JSON}")

if [[ -z "${ACCESS_KEY}" || -z "${SECRET_KEY}" ]]; then
  echo "[ERROR] Не удалось извлечь ключи доступа из ответа radosgw-admin." >&2
  exit 1
fi

echo "[INFO] Создание бакета '${RGW_BUCKET}' (realm: ${RGW_REALM}, zonegroup: ${RGW_ZONEGROUP}, zone: ${RGW_ZONE})..."
json_cmd radosgw-admin bucket create \
  --bucket="${RGW_BUCKET}" \
  --uid="${RGW_USER}" \
  --zone="${RGW_ZONE}" \
  --format=json >/dev/null 2>&1 || true

cat <<RESULT
========================================
Ceph RGW credentials:
  AWS_ACCESS_KEY_ID=${ACCESS_KEY}
  AWS_SECRET_ACCESS_KEY=${SECRET_KEY}
  BUCKET=${RGW_BUCKET}
  ENDPOINT=http://${CEPH_MON_IP:-127.0.0.1}:${CEPH_RGW_PORT:-7480}
========================================
Сохраните ключи и передайте их backend-сервису.
RESULT
