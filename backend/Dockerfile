FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --gid 10001 solvestack \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin solvestack
WORKDIR /app
COPY --from=build /workspace/target/solvestack-*.jar /app/solvestack.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/solvestack.jar"]
