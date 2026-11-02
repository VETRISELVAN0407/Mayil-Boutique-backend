# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --uid 10001 app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
# Product images / try-on temp files; mount a persistent disk here on Render
# and set PRODUCT_IMAGE_DIR=/data/product-images (otherwise they are lost on redeploy).
RUN mkdir -p /data && chown app:app /data
USER app

# Render injects PORT; 10000 is its default.
ENV PORT=10000
EXPOSE 10000
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
