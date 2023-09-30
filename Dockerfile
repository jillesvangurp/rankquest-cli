FROM openjdk:17.0.2-slim
LABEL maintainer="tryformation.com"

RUN apt-get --yes update && apt-get --yes install curl

COPY build/libs/rankquest-cli.jar /

VOLUME /rankquest
WORKDIR /rankquest

ENTRYPOINT ["java", "-jar", "/rankquest-cli.jar"]
