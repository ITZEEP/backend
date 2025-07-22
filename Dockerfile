# Build stage
FROM gradle:7.6-jdk11-alpine AS builder
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradle ./gradle
COPY build.gradle settings.gradle gradlew ./

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build application
RUN ./gradlew clean build -x test --no-daemon

# Runtime stage
FROM tomcat:9-jre11-temurin
WORKDIR /usr/local/tomcat

# Security: Create non-root user
RUN groupadd -r tomcat && useradd -r -g tomcat tomcat

# Remove default webapps and docs
RUN rm -rf webapps/* webapps.dist/docs webapps.dist/examples

# Copy WAR file
COPY --from=builder /app/build/libs/*.war webapps/ROOT.war

# Set ownership
RUN chown -R tomcat:tomcat /usr/local/tomcat

# Environment variables
ENV SPRING_PROFILES_ACTIVE=default
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m"

# Switch to non-root user
USER tomcat

EXPOSE 8080

CMD ["catalina.sh", "run"]