FROM maven:3.9.9-eclipse-temurin-21 AS build

ARG BUILD_CF_CLIENT=false

WORKDIR /app

COPY ./pom.xml .
COPY ./fota-server/pom.xml fota-server/

RUN mvn dependency:resolve

COPY . .

RUN ARGS="" && \
    if [ "$BUILD_CF_CLIENT" = "true" ]; then ARGS="$ARGS -Pcf-client"; fi && \
    mvn clean package -DskipTests $ARGS

RUN mkdir -p target

FROM openjdk:21-jdk-slim

ARG FOTA_SERVER_JAR=fota-server-jar-with-dependencies.jar
ARG LOAD_TEST_JAR=load-test-jar-with-dependencies.jar

WORKDIR /app

COPY --from=build /app/fota-server/src/main/resources/certs certs
COPY --from=build /app/fota-server/target/*.jar .
COPY --from=build /app/target/*.jar .

RUN mv $FOTA_SERVER_JAR fota-server.jar

EXPOSE 5683/udp 5684/udp

CMD  ["java", "-jar", "fota-server.jar"]
