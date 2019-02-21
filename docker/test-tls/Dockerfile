FROM java:8

ENV NOMAD_VERSION 0.8.7
# install docker libraries
RUN apt-get update && \
  apt-get install -y apt-transport-https ca-certificates && \
  apt-get clean all

RUN curl -fsSL get.docker.com -o get-docker.sh && \
  sh get-docker.sh

# put nomad
RUN curl https://releases.hashicorp.com/nomad/${NOMAD_VERSION}/nomad_${NOMAD_VERSION}_linux_amd64.zip > nomad.zip && \
  unzip nomad.zip
RUN mkdir /etc/nomad.d/
## nomad certificates and configs
COPY ./nomad-server-config/ /etc/nomad.d/

RUN echo "#!/bin/bash" > /usr/bin/nomad && \
  echo "exec /nomad agent -dev -config=/etc/nomad.d/" >> /usr/bin/nomad && \
  chmod 777 /usr/bin/nomad

# put consul
RUN curl https://releases.hashicorp.com/consul/0.6.4/consul_0.6.4_linux_amd64.zip > consul.zip
RUN unzip consul.zip
RUN echo "#!/bin/bash" > /usr/bin/consul && \
  echo "exec /consul agent -dev -node travis -bind 127.0.0.1" >> /usr/bin/consul && \
  chmod 777 /usr/bin/consul

# put cluster broccoli
ADD cluster-broccoli-dist /cluster-broccoli-dist
ADD templates /cluster-broccoli-dist/templates
RUN mkdir /cluster-broccoli-dist/instances
RUN ln -s /cluster-broccoli-dist/bin/cluster-broccoli /usr/bin/cluster-broccoli
## cluster-broccoli certificates and configs
COPY ./cluster-broccoli-files/ /
