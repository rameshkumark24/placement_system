# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Resolve dependencies in their own layer so code changes do not re-download them.
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline

COPY src src
RUN mvn -q -B -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --no-create-home --uid 10001 spring
COPY --from=build /app/target/placement-system-0.0.1-SNAPSHOT.jar app.jar
USER spring

# Tuned for small containers such as Render's free instance (512 MB, shared CPU):
# - MaxRAMPercentage lets the heap use most of the container memory
# - SerialGC has the lowest overhead on a single CPU
# - TieredStopAtLevel=1 (C1 only) noticeably shortens cold starts
# Override JAVA_OPTS in the host's environment settings if needed.
ENV PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
