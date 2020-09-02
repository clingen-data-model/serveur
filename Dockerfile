# For more information on these images, and use of Clojure in Docker
# https://hub.docker.com/_/clojure
FROM clojure:openjdk-11-lein AS builder

# Copying and building deps as a separate step in order to mitigate
# the need to download new dependencies every build.
COPY project.clj /usr/src/app/project.clj
WORKDIR /usr/src/app
RUN lein deps 


COPY . /usr/src/app
RUN lein uberjar

# Using image without lein for deployment.
FROM openjdk:11
MAINTAINER Tristan Nelson <thnelson@geisinger.edu>

COPY --from=builder /usr/src/app/keys /keys
COPY --from=builder /usr/src/app/target/uberjar/serveur.jar /app/serveur.jar

EXPOSE 8888

CMD ["java", "-jar", "/app/serveur.jar"]
