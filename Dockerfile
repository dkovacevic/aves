FROM docker.io/maven AS build-env

WORKDIR /app

COPY pom.xml ./

RUN mvn verify --fail-never

COPY . ./

RUN mvn -Dmaven.test.skip=true package

FROM docker.io/openjdk:8-jdk-alpine

COPY --from=build-env /app/target /opt/aves/
COPY swisscom.jks        /opt/aves/
COPY aves.yaml           /opt/aves/

WORKDIR /opt/aves

ENTRYPOINT ["java", "-jar", "aves.jar", "server", "aves.yaml"]