# Use Maven to build the project first
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Then copy the jar to a lightweight JDK container
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/ChatApplication-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
