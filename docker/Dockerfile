FROM openjdk:8-jdk-alpine
ARG BUILD_VERSION

# Copy Application Files
COPY run.sh /
COPY trss-${BUILD_VERSION}.jar app.jar

EXPOSE 88

ENTRYPOINT ["/bin/sh", "/run.sh"]