FROM maven:3.10-eclipse-temurin-25-noble AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseContainerSupport"
RUN useradd --system --uid 10001 --home-dir /app mediconnect
USER 10001
COPY --from=build /workspace/target/mediconnect-0.1.0-SNAPSHOT.jar /app/mediconnect.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/mediconnect.jar"]
