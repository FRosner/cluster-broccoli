FROM java:8

RUN curl -O https://downloads.typesafe.com/typesafe-activator/1.3.10/typesafe-activator-1.3.10.zip
RUN unzip typesafe-activator-1.3.10.zip && mv activator-dist-1.3.10 activator && rm -f typesafe-activator-1.3.10.zip

ENV PATH /activator/bin:$PATH
