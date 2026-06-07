FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S forum && adduser -S forum -G forum

COPY --from=build /workspace/target/forum-0.0.1-SNAPSHOT.jar /app/forum.jar

USER forum

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "/app/forum.jar"]
