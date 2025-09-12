#!/usr/bin/env bash
set -Eeuo pipefail

# 사용법: deploy_python.sh <tag>
TAG="${1:-manual}"
IMAGE="xioz19/my-issue-py:${TAG}"

APP_NAME="python_app"

# 컨테이너 내부 포트는 8085로 고정 (uvicorn --port 8085)
CONTAINER_PORT=8085

# 호스트 Blue/Green 포트(nginx upstream가 바라볼 포트)
BLUE_PORT=8085
GREEN_PORT=8086
BLUE_NAME="${APP_NAME}-blue"
GREEN_NAME="${APP_NAME}-green"

# nginx 포인터 파일(여기엔 upstream 블록 전체가 들어감)
UPSTREAM_FILE="/etc/nginx/upstreams/pyapp.active.conf"

echo "▶ IMAGE=${IMAGE}"

# 1) 현재 활성 포트 판정 (포인터 파일 안의 server 라인을 grep)
if [[ -f "${UPSTREAM_FILE}" ]]; then
  if grep -q "127.0.0.1:${BLUE_PORT}" "${UPSTREAM_FILE}"; then
    ACTIVE_PORT=${BLUE_PORT};  ACTIVE_NAME=${BLUE_NAME}
    NEW_PORT=${GREEN_PORT};    NEW_NAME=${GREEN_NAME}
  elif grep -q "127.0.0.1:${GREEN_PORT}" "${UPSTREAM_FILE}"; then
    ACTIVE_PORT=${GREEN_PORT}; ACTIVE_NAME=${GREEN_NAME}
    NEW_PORT=${BLUE_PORT};     NEW_NAME=${BLUE_NAME}
  else
    # 최초 배포 등: BLUE로 시작
    ACTIVE_PORT=${GREEN_PORT}; ACTIVE_NAME=${GREEN_NAME}
    NEW_PORT=${BLUE_PORT};     NEW_NAME=${BLUE_NAME}
  fi
else
  # 포인터 파일이 없으면 BLUE부터 시작
  ACTIVE_PORT=${GREEN_PORT}; ACTIVE_NAME=${GREEN_NAME}
  NEW_PORT=${BLUE_PORT};      NEW_NAME=${BLUE_NAME}
fi

echo "👉 ACTIVE=${ACTIVE_PORT} → NEW=${NEW_PORT}"

# 2) 새 컨테이너 기동
docker rm -f "${NEW_NAME}" >/dev/null 2>&1 || true

echo "🚀 run ${NEW_NAME} (host:${NEW_PORT} -> container:${CONTAINER_PORT})"

docker run -d --name "${NEW_NAME}" \
  -p ${NEW_PORT}:${CONTAINER_PORT} \
  -e "DATABASE_URL=${DATABASE_URL:?DATABASE_URL missing}" \
  -e GOOGLE_APPLICATION_CREDENTIALS=/run/secrets/gcp.json \
  -v /opt/sa/gcp.json:/run/secrets/gcp.json:ro \
  --health-cmd="python -c \"import sys,urllib.request; \
code=urllib.request.urlopen('http://127.0.0.1:${CONTAINER_PORT}/health', timeout=2).getcode(); \
sys.exit(0 if 200 <= code < 300 else 1)\"" \
  --health-interval=5s \
  --health-retries=30 \
  --health-timeout=3s \
  --health-start-period=60s \
  "${IMAGE}"


# 3) 헬스체크 대기
echo "⏳ waiting for health..."
for i in {1..60}; do
  status="$(docker inspect --format='{{.State.Health.Status}}' "${NEW_NAME}" 2>/dev/null || true)"
  if [[ "${status}" == "healthy" ]]; then
    echo "✅ ${NEW_NAME} healthy"; ok=1; break
  fi
  sleep 2
done
[[ "${ok:-0}" -eq 1 ]] || { echo "❌ health FAILED"; docker logs --tail=200 "${NEW_NAME}" || true; exit 1; }

# 4) nginx upstream 포인터 교체 (upstream 블록 전체)
sudo tee "${UPSTREAM_FILE}" >/dev/null <<EOF
server 127.0.0.1:${NEW_PORT} max_fails=3 fail_timeout=5s;
server 127.0.0.1:${ACTIVE_PORT} backup max_fails=3 fail_timeout=5s;
EOF

sudo nginx -t && sudo systemctl reload nginx
echo "🔁 Nginx -> python_app:${NEW_PORT}"

# 5) 구 컨테이너 제거
docker rm -f "${ACTIVE_NAME}" >/dev/null 2>&1 || true
echo "🧹 old ${ACTIVE_NAME} removed"

# 6) 최종 확인(선택)
curl -sI http://127.0.0.1:${NEW_PORT}/health | awk -F': ' '/HTTP/{print "🔎 new backend -> "$0}'