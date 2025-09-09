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

check_health() {
  docker run --rm --network "${DOCKER_NETWORK}" curlimages/curl:8.10.1 \
    -fsS "http://${NEW_CONTAINER}:8080${HEALTH_ENDPOINT}" 2>/dev/null | grep -q '"status":"UP"'
}

ok=0
for i in {1..10}; do
  sleep 3
  if check_health; then
    echo "✅ 헬스체크 통과 (via ${DOCKER_NETWORK} → ${NEW_CONTAINER}:8080)"
    ok=1
    break
  fi

  # 보조 플랜: 혹시 호스트에서 돌고 있으면 기존 방식도 한번 시도
  if curl -fsS "http://localhost:${NEW_PORT}${HEALTH_ENDPOINT}" 2>/dev/null | grep -q '"status":"UP"'; then
    echo "✅ 헬스체크 통과 (via localhost:${NEW_PORT})"
    ok=1
    break
  fi

  echo "… 대기 중(${i}/10)"
done

if [[ $ok -ne 1 ]]; then
  echo "❌ 헬스체크 실패. 롤백 진행"
  docker logs --tail=300 "${NEW_CONTAINER}" || true
  if [[ "${KEEP_ON_FAIL:-0}" == "1" ]]; then
    echo "🧷 KEEP_ON_FAIL=1 → 실패 컨테이너 유지: ${NEW_CONTAINER}"
  else
    docker rm -f "${NEW_CONTAINER}" || true
  fi
  exit 1
fi

# ====== Nginx 프록시 전환 ======
echo "🔁 Nginx 프록시 전환..."

# 우선순위:
# 1) 호스트 Nginx(SSH): NGINX_HOST가 설정되어 있으면 host:/etc/nginx/... 수정
# 2) 컨테이너 Nginx(docker exec): NGINX_CONTAINER가 있고 같은 네트워크일 때만
# 없으면 스킵(배포는 성공)


SED_EXPR_HOST="s#server[[:space:]]\\+[^;]*;#server localhost:${NEW_PORT};#"
if [[ -n "${NGINX_HOST:-}" ]]; then
  NGINX_USER="${NGINX_USER:-ubuntu}"
  SSH="ssh -o StrictHostKeyChecking=no ${NGINX_USER}@${NGINX_HOST}"
  echo "🔁 호스트(${NGINX_HOST}) Nginx 설정 변경 → localhost:${NEW_PORT}"
  if $SSH "sudo test -f '${NGINX_CONF}' && \
           sudo sed -i '${SED_EXPR_HOST}' '${NGINX_CONF}' && \
           sudo nginx -s reload"; then
    echo "✅ Nginx reloaded on ${NGINX_HOST}"
  else
    echo "⚠️  호스트 Nginx 업데이트 실패. 경로/권한/비번/무비번 sudo 설정을 확인하세요."
  fi
else
  NGINX_CONTAINER="${NGINX_CONTAINER:-}"  # 있으면 이름 지정
  if [[ -n "${NGINX_CONTAINER}" ]] && docker ps --format '{{.Names}}' | grep -q "^${NGINX_CONTAINER}$"; then
    # 컨테이너가 app-net에 붙어 있어야 컨테이너명으로 접근 가능
    if docker inspect -f '{{json .NetworkSettings.Networks}}' "${NGINX_CONTAINER}" | grep -q "${DOCKER_NETWORK}"; then
      TARGET="${NEW_CONTAINER}:8080"
      SED_EXPR_CONT="s#server[[:space:]]\\+[^;]*;#server ${TARGET};#"
      echo "🔁 Nginx 컨테이너(${NGINX_CONTAINER}) 설정 변경 → ${TARGET}"
      if docker exec "${NGINX_CONTAINER}" sh -lc \
        "test -f '${NGINX_CONF}' && sed -i '${SED_EXPR_CONT}' '${NGINX_CONF}' && nginx -s reload"; then
        echo "✅ Nginx reloaded in container '${NGINX_CONTAINER}'"
      else
        echo "⚠️  ${NGINX_CONTAINER}:${NGINX_CONF} 수정 실패(파일/권한/경로 확인)."
      fi
    else
      echo "⚠️  '${NGINX_CONTAINER}'가 '${DOCKER_NETWORK}'에 연결되어 있지 않아 전환 불가 → 스킵"
    fi
  else
    echo "ℹ️ NGINX_HOST/NGINX_CONTAINER 미설정 → 프록시 전환 스킵(배포 계속)"
  fi
fi

# ====== 기존 컨테이너 제거 ======
if docker ps -a --format '{{.Names}}' | grep -q "^${OLD_CONTAINER}$"; then
  echo "🧹 기존 컨테이너 ${OLD_CONTAINER} 제거"
  docker stop "${OLD_CONTAINER}" || true
  docker rm "${OLD_CONTAINER}" || true
fi

echo "✅ 무중단 배포 완료: ${NEW_CONTAINER}"
exit 0
