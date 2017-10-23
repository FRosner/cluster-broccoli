# Cluster Broccoli ![Icon](https://github.com/FRosner/cluster-broccoli/raw/52dd3447343705bc2d2a76de7e19a84873d89d0c/public/images/favicon-readme.png)

[![Build Status](https://travis-ci.org/FRosner/cluster-broccoli.svg?branch=master)](https://travis-ci.org/FRosner/cluster-broccoli)
[![codecov](https://codecov.io/gh/FRosner/cluster-broccoli/branch/master/graph/badge.svg)](https://codecov.io/gh/FRosner/cluster-broccoli)
[![Docker Pulls](https://img.shields.io/docker/pulls/frosner/cluster-broccoli.svg?maxAge=2592000)](https://hub.docker.com/r/frosner/cluster-broccoli/)
[![Gitter](https://badges.gitter.im/FRosner/cluster-broccoli.svg)](https://gitter.im/FRosner/cluster-broccoli?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

## Description

Cluster Broccoli is a RESTful web service + UI to manage [Nomad](https://www.nomadproject.io) jobs through a self service application. Jobs are defined based on [templates](https://github.com/FRosner/cluster-broccoli/wiki/Templates), allowing for a selectable amount of customization.

If you want to give your end users the possibility to create new instances of live demos of your product, while allowing them to customize it (e.g. using an embedded database or an external one, number of cores, ...) - Cluster Broccoli is for you.

Cluster Broccoli is meant to be setup by your IT. Some technical knowledge is required to setup the infrastructure and define the templates. End users can be internal (QA, data scientists) or external (customers, potential customers).

## Usage

### Web UI

Cluster Broccoli comes with a minimalistic web user interface. It allows you to create, destroy, start and stop your instances based on a set of predefined templates. Please consult the documentation for a [detailed description of the Web UI](https://github.com/FRosner/cluster-broccoli/wiki/Web-UI).

![image](https://cloud.githubusercontent.com/assets/3427394/26752821/f1e2c6a8-4858-11e7-81ed-82afa3017dea.png)

### HTTP API

Cluster Broccoli provides a RESTful HTTP API. You can control it using curl, writing your own command line wrapper or connect from your microservices. Please consult the documentation for a [detailed description of the HTTP API](https://github.com/FRosner/cluster-broccoli/wiki/HTTP-API-v1).

## Installation

### Requirements

#### Minimal Setup

- Nomad (HTTP API v1)
- Java (for running the Play application)

#### Recommended Setup

- Nomad (HTTP API v1)
- Consul (HTTP API v1)
- Cluster Broccoli Nomad Job + Docker Image (for running the Play application)

#### Distributed Application Server

Broccoli needs to run as a single instance.
It is neither supported to run multiple Broccoli instances sharing the same instance storage, nor running Broccoli in distributed mode because it uses a [local cache](http://www.ehcache.org/) to store the session IDs.

### Using the Production Docker Image

If you only need an image to run the Broccoli distribution, go with the JRE based [production-ready image](https://hub.docker.com/r/frosner/cluster-broccoli/).

```
docker run -p 9000:9000 frosner/cluster-broccoli \
  -Dbroccoli.nomad.url=<your-nomad-url> \
  -Dbroccoli.consul.url=<your-consul-url>
```

### Building from Source

Clone this repository and run `sbt dist` to build an [universal ZIP file][universal] in `server/target/universal`.

To build the production Docker image in a local Docker daemon run `sbt docker:publishLocal` local instead.  
`sbt docker:stage` creates a directory with intermediate artifacts and the `Dockerfile` in `server/target/docker/stage`; use this command to inspect the image contents before building or to manually build the image with a custom docker command. 

[universal]: http://sbt-native-packager.readthedocs.io/en/stable/formats/universal.html

## Configuration

In order to configure Cluster Broccoli, you can add key value pairs to your [configuration](https://www.playframework.com/documentation/2.5.x/Configuration).
The following configuration properties are supported. Please refer to the [Wiki](https://github.com/FRosner/cluster-broccoli/wiki) for more information about the individual topics.

See [Play configuration](https://www.playframework.com/documentation/2.5.x/Configuration) and [Broccoli's reference.conf](https://github.com/FRosner/cluster-broccoli/blob/master/server/src/main/resources/reference.conf) for available parameters and their meanings.

### Nomad and Consul

| Property | Description | Default |
| -------- | ----------- | ------- |
| `broccoli.consul.url` | Address of your consul server | `http://localhost:8500` |
| `broccoli.consul.lookup` | Lookup method used for consul. Options: `ip` or `dns` (recommended).| `ip` |
