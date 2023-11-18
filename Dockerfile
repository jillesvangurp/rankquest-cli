FROM amazoncorretto:17-alpine
LABEL maintainer="tryformation.com"

COPY build/libs/rankquest-cli.jar /

VOLUME /rankquest
WORKDIR /rankquest

ENTRYPOINT ["java", "-jar", "/rankquest-cli.jar"]
