# Cluster Broccoli

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

Cluster Broccoli comes with a minimalistic web user interface. It allows you to create, destroy, start and stop your instances based on a set of predefined templates. Please consult the documenation for a [detailed description of the Web UI](https://github.com/FRosner/cluster-broccoli/wiki/Web-UI).

![image](https://cloud.githubusercontent.com/assets/3427394/18438860/6159e5f0-7903-11e6-9a59-b4ba0c884a50.png)

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
- CouchDB 1.6 (persistence layer)
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

### Using the Development Docker Image

If you only intend to develop Broccoli and want to quickly get started with the source code, you can use the [development image](https://hub.docker.com/r/frosner/cluster-broccoli-dev/). It comes with the required tools for building and running Broccoli in development mode.

### Building from Source

1. Download or clone the source code
2. Navigate into the project directory
3. `sbt dist`

## Configuration

In order to configure Cluster Broccoli, you can add key value pairs to your [configuration](https://www.playframework.com/documentation/2.4.x/Configuration).
The following configuration properties are supported. Please refer to the [Wiki](https://github.com/FRosner/cluster-broccoli/wiki) for more information about the individual topics.

### Nomad and Consul

| Property | Description | Default |
| -------- | ----------- | ------- |
| `broccoli.nomad.url` | Address of your nomad server | `http://localhost:4646` |
| `broccoli.consul.url` | Address of your consul server | `http://localhost:8500` |
| `broccoli.consul.lookup` | Lookup method used for consul. Options: `ip` or `dns` (recommended).| `ip` |
| `broccoli.polling.frequency` | Integer (seconds) to control the time between asking Nomad and Consul for job and service status. | `1` |

### [Templates](https://github.com/FRosner/cluster-broccoli/wiki/Templates) and [Instances](https://github.com/FRosner/cluster-broccoli/wiki/Instances)

| Property | Description | Default |
| -------- | ----------- | ------- |
| `broccoli.templates.storage.type` | Storage type for templates. Currently only `fs` supported. | `fs` |
| `broccoli.templates.storage.fs.url` | Storage directory for templates in `fs` mode. | `templates` |
| `broccoli.instances.storage.type` | Storage type for instances. `fs` and `couchdb` are supported. See the [instance documentation](https://github.com/FRosner/cluster-broccoli/wiki/Instances) for details. | `fs` |
| `broccoli.instances.storage.fs.url` | Storage directory for instances in `fs` mode. | `instances` |
| `broccoli.instances.storage.couchdb.url` | URL to CouchDB in `couchdb` mode. | `http://localhost:5984` |
| `broccoli.instances.storage.couchdb.dbName` | Database name in `couchdb` mode. | `broccoli_instances` |

### [Security](https://github.com/FRosner/cluster-broccoli/wiki/Security)

| Property | Description | Default |
| -------- | ----------- | ------- |
| `broccoli.auth.mode` | Authentication and authorization mode (`none` or `conf`). | `none` |
| `broccoli.auth.conf.accounts` | User accounts when running in `conf` mode. | `[{username:administrator, password:broccoli, role:administrator, instanceRegex:".*"}]` |
| `broccoli.auth.session.timeout` | Cookie session timeout (in seconds). | `3600` |
| `broccoli.auth.cookie.secure` | Whether to mark the cookie as secure (switch off if running on HTTP). | `true` |
| `broccoli.auth.allowedFailedLogins` | Number of allowed failed logins before an account is blocked. | `3` |

### Web Server

| Property | Description | Default |
| -------- | ----------- | ------- |
| `http.port` | Port to bind the HTTP interface to. | `9000` |
| `https.port` | Port to listen for HTTPS connections. See [Play documentation](https://www.playframework.com/documentation/2.4.x/ConfiguringHttps) for details. | |
