FROM maven:3.10-eclipse-temurin-25-noble AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/mediconnect-0.1.0-SNAPSHOT.jar /app/mediconnect.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/mediconnect.jar"]
