#!/usr/bin/env bash
set -Eeuo pipefail

# ===== 기본 파라미터 =====
IMAGE_TAG="${1:?Usage: deploy.sh <image_tag>}"     # ex) release, manual, abc123
APP_IMAGE="xioz19/my-issue:${IMAGE_TAG}"

APP_NAME="myapp"
BLUE_NAME="${APP_NAME}-blue"
GREEN_NAME="${APP_NAME}-green"
BLUE_PORT=8083
GREEN_PORT=8084

NGINX_UPSTREAM_FILE="/etc/nginx/upstreams/myapp.active.conf"
HOST_HEADER="j13d101.p.ssafy.io"
HEALTH_PATH="/actuator/health"

echo "▶ IMAGE=${APP_IMAGE}"

# 1) 현재 활성 포트 판정
if [[ -f "${NGINX_UPSTREAM_FILE}" ]] && grep -q "127.0.0.1:${BLUE_PORT}" "${NGINX_UPSTREAM_FILE}"; then
  ACTIVE_COLOR=blue;  ACTIVE_PORT=${BLUE_PORT};  ACTIVE_NAME=${BLUE_NAME}
  NEW_COLOR=green;    NEW_PORT=${GREEN_PORT};    NEW_NAME=${GREEN_NAME}
elif [[ -f "${NGINX_UPSTREAM_FILE}" ]] && grep -q "127.0.0.1:${GREEN_PORT}" "${NGINX_UPSTREAM_FILE}"; then
  ACTIVE_COLOR=green; ACTIVE_PORT=${GREEN_PORT}; ACTIVE_NAME=${GREEN_NAME}
  NEW_COLOR=blue;     NEW_PORT=${BLUE_PORT};     NEW_NAME=${BLUE_NAME}
else
  ACTIVE_COLOR=blue;  ACTIVE_PORT=${BLUE_PORT};  ACTIVE_NAME=${BLUE_NAME}
  NEW_COLOR=green;    NEW_PORT=${GREEN_PORT};    NEW_NAME=${GREEN_NAME}
fi
echo "👉 ACTIVE=${ACTIVE_COLOR}(${ACTIVE_PORT}) → NEW=${NEW_COLOR}(${NEW_PORT})"

# 2) 새 컨테이너 기동 (-p 바인딩 필수)
docker rm -f "${NEW_NAME}" >/dev/null 2>&1 || true
echo "🚀 run ${NEW_NAME} on :${NEW_PORT}"
docker run -d --name "${NEW_NAME}" \
  -p ${NEW_PORT}:8080 \
  -e SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}" \
  -e SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-}" \
  -e SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-}" \
  -e SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-}" \
  "${APP_IMAGE}"

# 3) 헬스체크
for i in {1..30}; do
  if curl -fsS "http://127.0.0.1:${NEW_PORT}${HEALTH_PATH}" | grep -q '"status":"UP"'; then
    echo "✅ health UP"; ok=1; break
  fi; sleep 2
done
[[ "${ok:-0}" -eq 1 ]] || { echo "❌ health FAILED"; docker logs --tail=200 "${NEW_NAME}" || true; exit 1; }

# 4) 업스트림 스위칭(127.0.0.1:포트)
cat > "${NGINX_UPSTREAM_FILE}" <<EOF
server 127.0.0.1:${NEW_PORT} max_fails=3 fail_timeout=5s;
server 127.0.0.1:${ACTIVE_PORT} backup max_fails=3 fail_timeout=5s;
EOF
sudo nginx -t && sudo nginx -s reload

# 5) 구 컨테이너 제거
docker rm -f "${ACTIVE_NAME}" >/dev/null 2>&1 || true

# 6) 확인
curl -sI -H "Host: ${HOST_HEADER}" http://127.0.0.1/ | awk -F': ' '/X-Upstream/{print "🔎 X-Upstream: "$2}'
echo "✅ DONE (active=${NEW_COLOR}:${NEW_PORT})"