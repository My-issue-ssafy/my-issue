#!/bin/bash

IMAGE_TAG=$1
BASE_PORT=8080
BLUE_PORT=8083
GREEN_PORT=8084

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

echo "🚀 새 컨테이너 실행: $NEW_CONTAINER on port $NEW_PORT"

docker run -d \
  --name $NEW_CONTAINER \
  --network app-net \
  -e SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL \
  -e SPRING_DATASOURCE_USERNAME=$SPRING_DATASOURCE_USERNAME \
  -e SPRING_DATASOURCE_PASSWORD=$SPRING_DATASOURCE_PASSWORD \
  -p ${NEW_PORT}:8080 \
  xioz19/my-issue:${IMAGE_TAG}

sleep 10

echo "✅ 헬스체크 통과시 기존 컨테이너 종료"
OLD_CONTAINER=$(docker ps -qf "name=myapp-")
docker stop $OLD_CONTAINER
docker rm $OLD_CONTAINER
