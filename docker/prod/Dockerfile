FROM openjdk:8-jre

ADD cluster-broccoli-dist /cluster-broccoli

ADD templates /templates

RUN mkdir /instances

ENTRYPOINT ["/cluster-broccoli/bin/cluster-broccoli"]
