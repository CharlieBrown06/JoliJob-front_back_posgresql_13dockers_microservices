FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /workspace
ARG SERVICE

COPY . .
RUN chmod +x mvnw \
    && ./mvnw -B -ntp -pl "microservices/${SERVICE}" -am package -DskipTests

FROM eclipse-temurin:25-jre-jammy

WORKDIR /app
ARG SERVICE
COPY --from=build "/workspace/microservices/${SERVICE}/target/*.jar" /app/app.jar
RUN useradd --system --uid 10001 --create-home app \
    && chown app:app /app

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
