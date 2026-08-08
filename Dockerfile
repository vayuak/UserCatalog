# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
# 🟢 FIX: Force Maven to use less than 256MB of RAM so Railway doesn't kill it
ENV MAVEN_OPTS="-Xmx256m"
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# 🟢 FIX: Force Java runtime to stay under 300MB
ENTRYPOINT ["java", "-Xmx300m", "-jar", "app.jar"]
