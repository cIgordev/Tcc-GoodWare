FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir /app/tmp && chown 10001:10001 /app/tmp
COPY --from=build --chown=10001:10001 /build/target/goodware-backend-1.0.0.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-Djava.io.tmpdir=/app/tmp","-jar","/app/app.jar"]
