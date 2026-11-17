FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /code
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /opt/travel
COPY --from=build /code/target/travel-agent-1.0.0.jar /opt/travel/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/travel/app.jar"]
