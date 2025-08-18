# Simple Dockerfile for pre-built WAR
FROM tomcat:9-jre17-temurin
WORKDIR /usr/local/tomcat

# Set timezone to Asia/Seoul
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Install unzip and tzdata for timezone
RUN apt-get update && apt-get install -y unzip tzdata && rm -rf /var/lib/apt/lists/*

# Security: Create non-root user
RUN groupadd -r tomcat && useradd -r -g tomcat tomcat

# Remove default webapps and docs
RUN rm -rf webapps/* webapps.dist/docs webapps.dist/examples

# Copy pre-built WAR file and extract it
COPY build/libs/*.war /tmp/ROOT.war

# Extract WAR to ensure proper deployment
RUN cd webapps && \
    unzip -q /tmp/ROOT.war -d ROOT && \
    rm /tmp/ROOT.war && \
    # Ensure proper permissions for extracted files
    chown -R tomcat:tomcat ROOT && \
    chmod -R 755 ROOT

# Copy Firebase service account file to the correct location
# Firebase Admin SDK looks for this file in the classpath
COPY config-submodule/firebase-service-account.json /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/firebase-service-account.json

# Set proper permissions for Firebase service account file
RUN chown tomcat:tomcat /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/firebase-service-account.json && \
    chmod 644 /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/firebase-service-account.json

# Create logs directory and set proper permissions
RUN mkdir -p /usr/local/tomcat/logs && \
    chmod 777 /usr/local/tomcat/logs && \
    touch /usr/local/tomcat/logs/localhost_access_log.$(date +%Y-%m-%d).txt && \
    chmod 666 /usr/local/tomcat/logs/localhost_access_log.$(date +%Y-%m-%d).txt

# Create Tomcat configuration for context
RUN echo '<?xml version="1.0" encoding="UTF-8"?>' > conf/context.xml && \
    echo '<Context>' >> conf/context.xml && \
    echo '    <WatchedResource>WEB-INF/web.xml</WatchedResource>' >> conf/context.xml && \
    echo '    <WatchedResource>WEB-INF/classes/META-INF/web-fragment.xml</WatchedResource>' >> conf/context.xml && \
    echo '    <WatchedResource>${catalina.base}/conf/web.xml</WatchedResource>' >> conf/context.xml && \
    echo '</Context>' >> conf/context.xml

# Disable AccessLogValve to prevent permission issues
RUN sed -i '/AccessLogValve/d' conf/server.xml

# Set ownership for entire tomcat directory and ensure logs permissions
RUN chown -R tomcat:tomcat /usr/local/tomcat && \
    chmod -R 755 /usr/local/tomcat/logs && \
    chown -R tomcat:tomcat /usr/local/tomcat/logs

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m -Dspring.profiles.active=prod -Duser.timezone=Asia/Seoul"
# Add Tomcat configuration for metadata-complete
ENV CATALINA_OPTS="-Dorg.apache.catalina.startup.EXIT_ON_INIT_FAILURE=true"

# Switch to non-root user
USER tomcat

EXPOSE 8080

CMD ["catalina.sh", "run"]