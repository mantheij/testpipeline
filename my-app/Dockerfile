# Verwende ein Maven-Image zum Bauen
FROM maven:3.8.1-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Verwende ein OpenJDK-Image zum Ausführen der Anwendung
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/cloud-customer-usage-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
