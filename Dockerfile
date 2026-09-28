# syntax=docker/dockerfile:1.7
# Build: Maven + JDK 25. ~/.m2 cache is reused between builds.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY notification-domain/pom.xml notification-domain/
COPY notification-application/pom.xml notification-application/
COPY notification-infrastructure/pom.xml notification-infrastructure/
COPY notification-presentation/pom.xml notification-presentation/
COPY notification-domain/src notification-domain/src
COPY notification-application/src notification-application/src
COPY notification-infrastructure/src notification-infrastructure/src
COPY notification-presentation/src notification-presentation/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
    && java -Djarmode=tools -jar notification-presentation/target/notification-service.jar extract --layers --launcher --destination /workspace/extracted

# Runtime: JRE only, unprivileged user, Spring Boot layers for image caching.
FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --home /app app
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
