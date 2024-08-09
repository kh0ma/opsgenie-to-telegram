FROM observabilitystack/graalvm-maven-builder:21.0.1-ol9 AS build

ADD . /app

WORKDIR "/app"

RUN ./mvnw -Pproduction -Pnative native:compile

FROM debian
LABEL authors="kh0ma"

COPY --from=build /app/target/opsgenie-to-telegram /app/opsgenie-to-telegram

CMD ["/app/opsgenie-to-telegram"]
