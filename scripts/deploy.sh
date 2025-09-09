#!/bin/bash

IMAGE_TAG=$1
BASE_PORT=8080
BLUE_PORT=8083
GREEN_PORT=8084
HEALTH_ENDPOINT="/actuator/health"
NGINX_CONF="/etc/nginx/conf.d/upstream.conf"

# 현재 실행 중인 컨테이너 확인
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

# 기존 동일 이름 컨테이너 제거 (중복 방지)
if docker ps -a --format '{{.Names}}' | grep -q "^${NEW_CONTAINER}$"; then
  echo "🧹 ${NEW_CONTAINER} 컨테이너가 이미 존재하여 제거합니다."
  docker stop $NEW_CONTAINER || true
  docker rm $NEW_CONTAINER || true
fi

echo "🚀 새 컨테이너 실행: $NEW_CONTAINER on port $NEW_PORT"

docker run -d \
  --name $NEW_CONTAINER \
  --network app-net \
  -e SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL \
  -e SPRING_DATASOURCE_USERNAME=$SPRING_DATASOURCE_USERNAME \
  -e SPRING_DATASOURCE_PASSWORD=$SPRING_DATASOURCE_PASSWORD \
  -p ${NEW_PORT}:8080 \
  xioz19/my-issue:${IMAGE_TAG}

# 헬스체크 대기
echo "⏳ 헬스체크 중..."
for i in {1..10}; do
  sleep 3
  STATUS=$(curl -s http://localhost:${NEW_PORT}${HEALTH_ENDPOINT} | grep '"status":"UP"')
  if [[ $STATUS != "" ]]; then
    echo "✅ 헬스체크 통과"
    break
  fi
  if [[ $i -eq 10 ]]; then
    echo "❌ 헬스체크 실패. 롤백 진행"
    docker logs --tail 300 "$NEW_CONTAINER" || true

    # 디버깅 편의: 컨테이너를 남겨두고 종료 (KEEP_ON_FAIL=1 일 때만)
    if [[ "${KEEP_ON_FAIL:-0}" == "1" ]]; then
      echo "🧷 KEEP_ON_FAIL=1 → 실패 컨테이너 유지: $NEW_CONTAINER"
    else
      docker rm -f "$NEW_CONTAINER" || true
    fi
    exit 1
  fi
done

# Nginx 프록시 전환
echo "🔁 Nginx 프록시를 ${NEW_PORT}로 전환 중..."
sed -i "s/server localhost:.*/server localhost:${NEW_PORT};/" $NGINX_CONF
nginx -s reload

# 기존 컨테이너 제거
if docker ps -a --format '{{.Names}}' | grep -q "^${OLD_CONTAINER}$"; then
  echo "🧹 기존 컨테이너 ${OLD_CONTAINER} 제거"
  docker stop $OLD_CONTAINER || true
  docker rm $OLD_CONTAINER || true
fi


echo "✅ 무중단 배포 완료: ${NEW_CONTAINER}"
exit 0