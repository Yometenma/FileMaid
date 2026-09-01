FROM node:22-alpine AS web-build
WORKDIR /workspace/web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

FROM gradle:8.10.2-jdk17 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY modules/core modules/core
COPY modules/application modules/application
COPY modules/infrastructure modules/infrastructure
COPY modules/server modules/server
COPY --from=web-build /workspace/modules/server/src/main/resources/static modules/server/src/main/resources/static
RUN gradle :modules:server:bootJar --no-daemon --console=plain

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --system filemaid && useradd --system --gid filemaid --home-dir /app filemaid \
    && mkdir -p /config /media && chown -R filemaid:filemaid /app /config /media
COPY --from=build /workspace/modules/server/build/libs/server-*.jar /app/filemaid.jar
USER filemaid
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1
ENV FILEMAID_MEDIA_ROOT=/media
ENTRYPOINT ["java", "-jar", "/app/filemaid.jar"]
