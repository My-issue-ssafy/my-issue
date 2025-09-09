#!/bin/bash
set -Eeuo pipefail

IMAGE_TAG=${1:? "Usage: $0 <IMAGE_TAG>"}

# ====== 기본 설정 ======
BASE_PORT=8080
BLUE_PORT=8083
GREEN_PORT=8084
HEALTH_ENDPOINT="/actuator/health"
NGINX_CONF="/etc/nginx/conf.d/upstream.conf"
APP_IMAGE="xioz19/my-issue:${IMAGE_TAG}"
DOCKER_NETWORK="app-net"

# ====== 필수 환경 변수 빠른 검증 (누락 시 바로 설명하고 종료) ======
: "${SPRING_DATASOURCE_URL:?SPRING_DATASOURCE_URL is required. e.g. jdbc:postgresql://host:5432/db or jdbc:mariadb://host:3306/db}"
: "${SPRING_DATASOURCE_USERNAME:?SPRING_DATASOURCE_USERNAME is required.}"
: "${SPRING_DATASOURCE_PASSWORD:?SPRING_DATASOURCE_PASSWORD is required.}"

# 선택(기본값 제공): 프로필/헬스 노출
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:-health,info}"
MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED="${MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED:-true}"

echo "▶ Using profile: ${SPRING_PROFILES_ACTIVE}"
echo "▶ Datasource URL: ${SPRING_DATASOURCE_URL}"

# ====== 네트워크 보장 ======
if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
  echo "🔌 Docker network '${DOCKER_NETWORK}' not found. Creating..."
  docker network create "${DOCKER_NETWORK}"
fi

# ====== 현재 실행 중인 컨테이너 확인 ======
CURRENT=$(docker ps --filter "name=myapp-" --format "{{.Names}}")
if [[ $CURRENT == *"blue"* ]]; then
  NEW_COLOR="green"
  NEW_PORT=$GREEN_PORT
  OLD_COLOR="blue"
else
  NEW_COLOR="blue"
  NEW_PORT=$BLUE_PORT
  OLD_COLOR="green"
fi

NEW_CONTAINER="myapp-${NEW_COLOR}"
OLD_CONTAINER="myapp-${OLD_COLOR}"

# ====== 동일 이름 컨테이너 사전 제거 ======
if docker ps -a --format '{{.Names}}' | grep -q "^${NEW_CONTAINER}$"; then
  echo "🧹 ${NEW_CONTAINER} 컨테이너가 이미 존재하여 제거합니다."
  docker stop "${NEW_CONTAINER}" || true
  docker rm "${NEW_CONTAINER}" || true
fi

echo "🚀 새 컨테이너 실행: ${NEW_CONTAINER} on port ${NEW_PORT}"

docker run -d \
  --name "${NEW_CONTAINER}" \
  --network "${DOCKER_NETWORK}" \
  -p "${NEW_PORT}:8080" \
  -e SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}" \
  -e SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL}" \
  -e SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME}" \
  -e SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD}" \
  -e MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE}" \
  -e MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED="${MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED}" \
  "${APP_IMAGE}"

# ====== 헬스체크 ======
echo "⏳ 헬스체크 중..."
for i in {1..10}; do
  sleep 3
  if curl -fsS "http://localhost:${NEW_PORT}${HEALTH_ENDPOINT}" | grep -q '"status":"UP"'; then
    echo "✅ 헬스체크 통과"
    break
  fi
  if [[ $i -eq 10 ]]; then
    echo "❌ 헬스체크 실패. 롤백 진행"
    docker logs --tail=300 "${NEW_CONTAINER}" || true

    if [[ "${KEEP_ON_FAIL:-0}" == "1" ]]; then
      echo "🧷 KEEP_ON_FAIL=1 → 실패 컨테이너 유지: ${NEW_CONTAINER}"
    else
      docker rm -f "${NEW_CONTAINER}" || true
    fi
    exit 1
  fi
done

# ====== Nginx 프록시 전환 ======
echo "🔁 Nginx 프록시를 ${NEW_PORT}로 전환 중..."
# upstream.conf의 'server localhost:xxxx;' 한 줄을 새 포트로 교체
sed -i "s/server localhost:.*/server localhost:${NEW_PORT};/" "${NGINX_CONF}"
nginx -s reload

# ====== 기존 컨테이너 제거 ======
if docker ps -a --format '{{.Names}}' | grep -q "^${OLD_CONTAINER}$"; then
  echo "🧹 기존 컨테이너 ${OLD_CONTAINER} 제거"
  docker stop "${OLD_CONTAINER}" || true
  docker rm "${OLD_CONTAINER}" || true
fi

echo "✅ 무중단 배포 완료: ${NEW_CONTAINER}"
exit 0
