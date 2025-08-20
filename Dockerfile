FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
# if artifact name stable, reference directly; else wildcard
COPY --from=build /build/target/*SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC -XX:+AlwaysPreTouch -XX:MaxGCPauseMillis=200"
# optional: run as non-root
RUN useradd -r -u 1001 appuser && chown appuser /app && chmod 444 app.jar
USER appuser
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]