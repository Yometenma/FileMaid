FROM gradle:8.10.2-jdk17 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY modules/core modules/core
COPY modules/application modules/application
COPY modules/infrastructure modules/infrastructure
COPY modules/server modules/server
RUN gradle :modules:server:bootJar --no-daemon --console=plain

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system filemaid && useradd --system --gid filemaid --home-dir /app filemaid \
    && mkdir -p /config /media && chown -R filemaid:filemaid /app /config /media
COPY --from=build /workspace/modules/server/build/libs/server-*.jar /app/filemaid.jar
USER filemaid
EXPOSE 8080
ENV FILEMAID_MEDIA_ROOT=/media
ENTRYPOINT ["java", "-jar", "/app/filemaid.jar"]
