FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null 2>&1 || true
COPY src src
COPY prompts prompts
COPY docs docs
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 platform
COPY --from=build /workspace/build/libs/*.jar /app/application.jar
USER platform
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
