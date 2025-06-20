FROM eclipse-temurin:17.0.11_9-jdk-jammy AS build

WORKDIR /wd

RUN apt-get update && apt-get install -y unzip

COPY . .

RUN ./gradlew :self-demo:distZip && \
    cd self-demo/build/distributions/ && \
    unzip self-demo.zip


FROM eclipse-temurin:17.0.11_9-jdk-jammy

COPY --from=build /wd/self-demo/build/distributions/self-demo /srv/self-demo

CMD ["/srv/self-demo/bin/self-demo"]
