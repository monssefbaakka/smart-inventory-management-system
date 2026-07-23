# Build stage
FROM maven:3.9.15-eclipse-temurin-26-alpine AS builder
WORKDIR /app

# Copy the pom.xml and resolve dependencies to leverage Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests -B

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-privileged user to run the application
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from the builder stage
COPY --from=builder /app/target/smart-inventory-management-system-*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
