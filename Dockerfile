FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S notify && adduser -S notify -G notify

COPY --from=build /workspace/target/notify-0.0.1-SNAPSHOT.jar /app/notify.jar

USER notify

ENV PORT=10000
EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/notify.jar"]
