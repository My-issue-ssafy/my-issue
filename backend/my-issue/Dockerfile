# ── Build stage : 애플리케이션 JAR 만드는 단계 ──
FROM gradle:8.9-jdk21 AS builder
WORKDIR /src
COPY . .
RUN chmod +x gradlew && ./gradlew clean bootJar --no-daemon

# ── Run stage : 빌드된 JAR 실행하는 단계 ──
# 1) Java 21 JRE 환경이 담긴 공식 이미지 사용
FROM eclipse-temurin:21-jre

# 2) 컨테이너 안의 작업 디렉토리
WORKDIR /app

# 타임존 환경 변수 설정 (서울)
ENV TZ=Asia/Seoul

# 3) 빌드된 JAR 파일 복사
COPY --from=builder /src/build/libs/*.jar app.jar

# 4) 외부에서 접근할 포트 (Spring Boot 기본 8080)
EXPOSE 8080

# 5) 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]