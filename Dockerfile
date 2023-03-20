FROM oracle/graalvm-ce:20.2.0-java11 as graalvm
RUN gu install native-image

COPY attini-cli /home/app/attini-cli
WORKDIR /home/app/attini-cli

RUN native-image -cp target/attini-cli-*.jar

FROM frolvlad/alpine-glibc
RUN apk update && apk add libstdc++
EXPOSE 8080
COPY --from=graalvm /home/app/attini-cli/attini-cli /app/attini-cli

ENTRYPOINT ["/app/attini-cli"]
