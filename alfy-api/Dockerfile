FROM m.daocloud.io/docker.io/library/eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY .mvn/settings.xml /root/.m2/settings.xml
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

FROM m.daocloud.io/docker.io/library/eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN useradd --system --create-home --home-dir /app alfy
COPY --from=build /workspace/target/*.jar /app/alfy-api.jar
RUN mkdir -p /app/data/uploads && chown -R alfy:alfy /app

USER alfy

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "/app/alfy-api.jar"]
