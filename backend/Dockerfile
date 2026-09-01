# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Run as an unprivileged user rather than root. The group is created explicitly
# rather than relying on useradd's implicit behaviour, which varies by base image.
RUN groupadd --system --gid 10001 appuser \
    && useradd --system --uid 10001 --gid 10001 --create-home appuser \
    && chown appuser:appuser /app/app.jar
USER appuser

EXPOSE 8080

# Render's free tier caps containers at 512 MB. The JVM's default heap sizing is a
# fraction of *host* memory, which overshoots that cgroup limit and gets the
# container OOM-killed, so cap the heap as a percentage of the container limit.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
