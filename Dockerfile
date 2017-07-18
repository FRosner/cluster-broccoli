FROM java:8

RUN curl -O https://downloads.typesafe.com/typesafe-activator/1.3.10/typesafe-activator-1.3.10.zip
RUN unzip typesafe-activator-1.3.10.zip && mv activator-dist-1.3.10 activator && rm -f typesafe-activator-1.3.10.zip

ENV PATH /activator/bin:$PATH

COPY ./ /cluster-broccoli
WORKDIR /cluster-broccoli
RUN activator dist

RUN unzip server/target/universal/cluster-broccoli-*.zip
RUN mv cluster-broccoli-* dist

EXPOSE 9000

ENTRYPOINT ["dist/bin/cluster-broccoli"]
