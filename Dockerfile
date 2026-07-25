FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system clientdesk \
    && useradd --system --gid clientdesk --home-dir /app clientdesk

WORKDIR /app

COPY --from=build --chown=clientdesk:clientdesk \
    /workspace/target/clientdesk-backend-*.jar /app/clientdesk-backend.jar

USER clientdesk

EXPOSE 10000

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/clientdesk-backend.jar"]
