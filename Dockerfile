FROM openjdk:21
LABEL authors="kh0ma"

ADD . /app

WORKDIR "/app"
ENTRYPOINT ["/app/mvnw", "spring-boot:run"]
