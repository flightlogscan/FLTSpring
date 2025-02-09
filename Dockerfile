# Cache stage to download and cache dependencies
FROM eclipse-temurin:21-jdk AS cache
RUN mkdir -p /root/gradle_cache
ENV GRADLE_USER_HOME /root/gradle_cache
COPY build.gradle settings.gradle gradlew /app/
COPY gradle /app/gradle
WORKDIR /app
RUN ./gradlew dependencies --no-daemon

# Builder stage that uses the cached dependencies
FROM eclipse-temurin:21-jdk AS builder
COPY --from=cache /root/gradle_cache /root/.gradle
WORKDIR /app
COPY . .
ARG GRADLE_ARGS="build --no-daemon"
RUN ./gradlew ${GRADLE_ARGS}

# Final runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/FLTSpring-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/build/resources/main/. .

ENV JAVA_OPTS="-Xms512m -Xmx1024m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]