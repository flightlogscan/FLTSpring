# Use jdk to compile in the first stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
ARG GRADLE_ARGS="build --no-daemon"
RUN ./gradlew ${GRADLE_ARGS}

# In multi-stage builds, only the final FROM stage is included in the final image.
# Everything before it is temporary build context.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/FLTSpring-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /app/build/resources/main/. .

ENV JAVA_OPTS="-Xms512m -Xmx1024m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]