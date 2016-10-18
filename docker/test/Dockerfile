FROM java:8

# install docker libraries
RUN apt-get update && \
  apt-get install -y apt-transport-https ca-certificates && \
  apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D && \
  echo "deb https://apt.dockerproject.org/repo ubuntu-trusty main" > /etc/apt/sources.list.d/docker.list && \
  apt-get update && \
  apt-get purge lxc-docker && \
  apt-cache policy docker-engine && \
  apt-get update && \
  apt-get install -y docker-engine

# put nomad
RUN curl https://releases.hashicorp.com/nomad/0.4.0/nomad_0.4.0_linux_amd64.zip > nomad.zip
RUN unzip nomad.zip
RUN echo "#!/bin/bash" > /usr/bin/nomad && \
  echo "exec /nomad agent -dev" >> /usr/bin/nomad && \
  chmod 777 /usr/bin/nomad

# put consul
RUN curl https://releases.hashicorp.com/consul/0.6.4/consul_0.6.4_linux_amd64.zip > consul.zip
RUN unzip consul.zip
RUN echo "#!/bin/bash" > /usr/bin/consul && \
  echo "exec /consul agent -dev -node travis -bind 127.0.0.1" >> /usr/bin/consul && \
  chmod 777 /usr/bin/consul

# put cluster broccoli
COPY cluster-broccoli-dist /cluster-broccoli-dist

COPY templates /templates
RUN echo "#!/bin/bash" > /usr/bin/cluster-broccoli && \
  echo "exec /cluster-broccoli-dist/bin/cluster-broccoli -Dbroccoli.templatesDir=/templates" >> /usr/bin/cluster-broccoli && \
  chmod 777 /usr/bin/cluster-broccoli
