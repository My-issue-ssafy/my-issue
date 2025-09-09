#!/usr/bin/env bash
set -Eeuo pipefail

# ===== 기본 파라미터 =====
IMAGE_TAG="${1:?Usage: deploy.sh <image_tag>}"     # ex) release, manual, abc123
APP_IMAGE="xioz19/my-issue:${IMAGE_TAG}"

APP_NAME="myapp"
BLUE_NAME="${APP_NAME}-blue"
GREEN_NAME="${APP_NAME}-green"

DOCKER_NETWORK="app-net"
NGINX_UPSTREAM_DIR="/etc/nginx/upstreams"
NGINX_UPSTREAM_FILE="${NGINX_UPSTREAM_DIR}/myapp.active.conf"
NGINX_CONF_MAIN="/etc/nginx/conf.d/myapp.conf"     # upstream 포함한 서버 conf
HOST_HEADER="j13d101.p.ssafy.io"
HEALTH_PATH="/actuator/health"

# =====(선택) Spring 환경변수 =====
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:-health,info}"
MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED="${MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED:-true}"

echo "▶ IMAGE=${APP_IMAGE}"
echo "▶ PROFILE=${SPRING_PROFILES_ACTIVE}"

# ===== 네트워크 보장 =====
if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
  echo "🔌 create network: ${DOCKER_NETWORK}"
  docker network create "${DOCKER_NETWORK}"
fi

# ===== 현재 활성 타겟 파악 (Nginx upstream 파일 기준) =====
if [[ ! -f "${NGINX_UPSTREAM_FILE}" ]]; then
  echo "📄 init upstream file @ ${NGINX_UPSTREAM_FILE}"
  sudo mkdir -p "${NGINX_UPSTREAM_DIR}"
  # 기본: blue 활성, green 백업
  echo -e "server ${BLUE_NAME}:8080 max_fails=3 fail_timeout=5s;\nserver ${GREEN_NAME}:8080 backup;" \
    | sudo tee "${NGINX_UPSTREAM_FILE}" >/dev/null
  sudo nginx -t && sudo nginx -s reload
fi

if grep -q "${BLUE_NAME}:8080" "${NGINX_UPSTREAM_FILE}" && ! grep -q "backup" "${NGINX_UPSTREAM_FILE}" | grep -q "${BLUE_NAME}:8080"; then
  ACTIVE="blue"
elif grep -q "${BLUE_NAME}:8080" "${NGINX_UPSTREAM_FILE}" && grep -q "backup" "${NGINX_UPSTREAM_FILE}"; then
  # 단순 체크 보완
  ACTIVE=$(grep -oE "${BLUE_NAME}:8080|${GREEN_NAME}:8080" "${NGINX_UPSTREAM_FILE}" | head -1 | grep -q "${BLUE_NAME}" && echo blue || echo green)
else
  ACTIVE=$(grep -oE "${BLUE_NAME}:8080|${GREEN_NAME}:8080" "${NGINX_UPSTREAM_FILE}" | head -1 | grep -q "${BLUE_NAME}" && echo blue || echo green)
fi

if grep -q "${BLUE_NAME}:8080" "${NGINX_UPSTREAM_FILE}" | grep -qv "backup" >/dev/null 2>&1; then
  ACTIVE="blue"
fi

if grep -q "${BLUE_NAME}:8080" "${NGINX_UPSTREAM_FILE}"; then
  # 첫 server 라인을 활성으로 간주
  FIRST=$(grep -n "${BLUE_NAME}:8080\|${GREEN_NAME}:8080" "${NGINX_UPSTREAM_FILE}" | head -1 | awk -F: '{print $2}')
  if echo "$FIRST" | grep -q "${BLUE_NAME}"; then ACTIVE="blue"; else ACTIVE="green"; fi
fi

if [[ "${ACTIVE}" == "blue" ]]; then
  NEW_COLOR="green"
else
  NEW_COLOR="blue"
fi

NEW_CONTAINER="${APP_NAME}-${NEW_COLOR}"
OLD_CONTAINER="${APP_NAME}-${ACTIVE}"

echo "👉 ACTIVE=${ACTIVE} → NEW=${NEW_COLOR}"
echo "👉 NEW_CONTAINER=${NEW_CONTAINER}"

# ===== 기존 동명이 컨테이너 정리 =====
docker rm -f "${NEW_CONTAINER}" >/dev/null 2>&1 || true

# ===== 새 컨테이너 기동 (컨테이너명으로만 접근, 호스트 포트 바인딩 X) =====
echo "🚀 run ${NEW_CONTAINER} (${APP_IMAGE})"
docker run -d \
  --name "${NEW_CONTAINER}" \
  --network "${DOCKER_NETWORK}" \
  -e SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}" \
  -e SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-}" \
  -e SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-}" \
  -e SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-}" \
  -e MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE}" \
  -e MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED="${MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED}" \
  "${APP_IMAGE}"

# ===== 헬스체크 (동일 네트워크에서 컨테이너명으로 체크) =====
echo "⏳ healthcheck: http://${NEW_CONTAINER}:8080${HEALTH_PATH}"
ok=0
for i in {1..30}; do
  if docker run --rm --network "${DOCKER_NETWORK}" curlimages/curl:8.10.1 \
      -fsS "http://${NEW_CONTAINER}:8080${HEALTH_PATH}" | grep -q '"status":"UP"'; then
    echo "✅ health UP"
    ok=1; break
  fi
  sleep 2
done
if [[ $ok -ne 1 ]]; then
  echo "❌ health FAILED"
  docker logs --tail=200 "${NEW_CONTAINER}" || true
  docker rm -f "${NEW_CONTAINER}" || true
  exit 1
fi

# ===== Nginx 전환 (include 파일 전체 치환 → 무중단 reload) =====
echo "🔁 switch upstream → ${NEW_CONTAINER} active"
if [[ "${NEW_COLOR}" == "blue" ]]; then
  NEW_UPSTREAM="server ${BLUE_NAME}:8080 max_fails=3 fail_timeout=5s;
server ${GREEN_NAME}:8080 backup;"
else
  NEW_UPSTREAM="server ${GREEN_NAME}:8080 max_fails=3 fail_timeout=5s;
server ${BLUE_NAME}:8080 backup;"
fi

echo "${NEW_UPSTREAM}" | sudo tee "${NGINX_UPSTREAM_FILE}" >/dev/null
sudo nginx -t && sudo nginx -s reload

# ===== 구 컨테이너 드레인 후 제거 =====
echo "🧹 remove old: ${OLD_CONTAINER}"
docker rm -f "${OLD_CONTAINER}" >/dev/null 2>&1 || true

# ===== 검증 (X-Upstream 확인, 컨테이너명으로 찍힘) =====
UP=$(curl -sI -H "Host: ${HOST_HEADER}" http://127.0.0.1/ | awk -F': ' '/X-Upstream/ {print $2}' | tr -d '\r')
echo "🔎 X-Upstream: ${UP}"

echo "✅ DONE (active=${NEW_COLOR})"