FROM docker.io/library/eclipse-temurin:21-jre

WORKDIR /app

COPY target/demo1-0.0.1-SNAPSHOT.jar app.jar

# Keep heap below the pod memory limit to leave room for metaspace, threads and native memory.
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=70.0 -XX:ActiveProcessorCount=2 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
