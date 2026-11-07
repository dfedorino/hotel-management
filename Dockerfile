# One image recipe for every service: the build stage compiles the whole project once (Docker reuses it for all
# services), the runtime stage takes the jar of the module given by the MODULE build argument.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
# Tests and quality checks run in CI and in `./mvnw verify`; the image only needs the jars.
RUN --mount=type=cache,target=/root/.m2 \
    sh ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
RUN groupadd --system app && useradd --system --gid app --home /app app \
    && mkdir -p /app/logs && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /src/${MODULE}/target/${MODULE}-*.jar /app/app.jar
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
