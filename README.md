# Cluster Broccoli

[![Build Status](https://travis-ci.org/FRosner/cluster-broccoli.svg?branch=master)](https://travis-ci.org/FRosner/cluster-broccoli)
[![codecov](https://codecov.io/gh/FRosner/cluster-broccoli/branch/master/graph/badge.svg)](https://codecov.io/gh/FRosner/cluster-broccoli)

## Description

Cluster Broccoli is a RESTful web service + UI to manage [Nomad](https://www.nomadproject.io) jobs through a self service application. Jobs are defined based on templates, allowing for a selectable amount of customization.

If you want to give your end users the possibility to create new instances of live demos of your product, while allowing them to customize it (e.g. using an embedded database or an external one, number of cores, ...) - Cluster Broccoli is for you.

Cluster Broccoli is meant to be setup by your IT. Some technical knowledge is required to setup the infrastructure and define the templates. End users can be internal (QA, data scientists) or external (customers, potential customers).

## Usage

### Web UI

Cluster Broccoli comes with a minimalistic web user interface. It allows you to create, destroy, start and stop your instances based on a set of predefined templates. Please consult the documenation for a [detailed description of the Web UI](https://github.com/FRosner/cluster-broccoli/wiki/Web-UI).

![image](https://cloud.githubusercontent.com/assets/3427394/16338304/0aa8fd52-3a1c-11e6-85d0-d31e254dcbb4.png)

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

### Using the Docker Image

```
docker run -p 9000:9000 frosner/cluster-broccoli \
  -Dbroccoli.nomad.url=<your-nomad-url> \
  -Dbroccoli.consul.url=<your-consul-url>
```

### Building from Source

1. Download or clone the source code
2. Navigate into the project directory
3. `activator dist`

## Configuration

In order to configure Cluster Broccoli, you can add key value pairs to your [configuration](https://www.playframework.com/documentation/2.4.x/Configuration).
The following configuration properties are supported.

| Property | Description | Default |
| -------- | ----------- | ------- |
| `broccoli.nomad.url` | Address of your nomad server | `http://localhost:4646` |
| `broccoli.consul.url` | Address of your consul server | `http://localhost:8500` |
| `broccoli.templatesDir` | Directory where your templates are located | `templates` |
