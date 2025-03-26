# Use an official JDK image as a base
FROM openjdk:21-jdk-slim

# Set working directory inside the container
WORKDIR /app

# Copy the Spring Boot JAR file (update the filename as needed)
COPY target/UNIVERS-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
