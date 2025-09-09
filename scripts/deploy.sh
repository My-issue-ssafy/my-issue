#!/bin/bash

IMAGE_TAG=$1
NEW_PORT=$2
NEW_CONTAINER="myapp-${IMAGE_TAG}"

echo "👉 새 컨테이너 배포: ${NEW_CONTAINER}"

docker run -d \
  --name $NEW_CONTAINER \
  --network app-net \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://my-issue:5432/mydb \
  -e SPRING_DATASOURCE_USERNAME=myissue \
  -e SPRING_DATASOURCE_PASSWORD=myissue101234 \
  -p ${NEW_PORT}:8080 \
  xioz19/my-issue:${IMAGE_TAG}

sleep 10

echo "✅ 헬스체크 통과시 기존 컨테이너 종료"
OLD_CONTAINER=$(docker ps -qf "name=myapp-")
docker stop $OLD_CONTAINER
docker rm $OLD_CONTAINER
