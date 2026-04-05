FROM gradle:8.7-jdk21 AS build

WORKDIR /workspace

# OpenAPI spec lives at repository root, backend build expects ../openapi.yaml
COPY openapi.yaml ./openapi.yaml
COPY backend ./backend

WORKDIR /workspace/backend

RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test
RUN JAR_PATH=$(ls build/libs/*.jar | grep -v 'plain' | head -n 1) && cp "$JAR_PATH" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
