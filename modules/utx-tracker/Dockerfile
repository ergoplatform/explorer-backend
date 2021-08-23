FROM openjdk:8-jre-slim as builder
RUN apt-get update && \
    apt-get install -y --no-install-recommends apt-transport-https apt-utils bc dirmngr gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends sbt
COPY . /explorer-backend
WORKDIR /explorer-backend
RUN sbt utx-tracker/assembly
RUN mv `find . -name UtxTracker-assembly-*.jar` /utx-tracker.jar
CMD ["/usr/bin/java", "-jar", "/utx-tracker.jar"]

FROM openjdk:8-jre-slim
COPY --from=builder /utx-tracker.jar /utx-tracker.jar
ENTRYPOINT java -jar /utx-tracker.jar $0