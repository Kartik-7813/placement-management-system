# ===================================================================
# Stage 1: Build Application with Maven Wrapper & Eclipse Temurin JDK 21
# ===================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy Maven Wrapper and POM first
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Normalize line endings on mvnw (prevents Windows CRLF issues in Linux containers)
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Copy application source code
COPY src ./src

# Package application JAR without running unit or integration tests
RUN ./mvnw clean package -DskipTests -B

# ===================================================================
# Stage 2: Minimal Production Runtime with Eclipse Temurin JRE 21
# ===================================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Install curl for container health checks
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create a non-privileged system user and group to run the application
RUN groupadd -r spring && useradd -r -g spring spring

# Copy only the compiled executable JAR from builder stage with ownership
COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose standard Spring Boot HTTP port
EXPOSE 8080

# Configure production profile and container-aware JVM memory limits
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Execute application using exec to ensure OS signals (SIGTERM) are handled by PID 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]