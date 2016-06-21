# Cluster Broccoli

## Description

Cluster Broccoli is a RESTful web service + UI to manage [Nomad](https://www.nomadproject.io) jobs through a self service application. Jobs are defined based on templates, allowing for a selectable amount of customization.

If you want to give your end users the possibility to create new instances of live demos of your product, while allowing them to customize whether it should use an embedded database or an external one, Cluster Broccoli is for you.

## Target Audience

Cluster Broccoli is meant to be setup by your IT. Some technical knowledge is required to setup the infrastructure and define the templates. End users can be internal (QA, data scientists) or external (customers, potential customers).

## Requirements

### Minimal Setup

- Nomad (REST API v1)
- Java (for running the Play application)

### Recommended Setup

- Nomad (REST API v1)
- Consul (REST API v1)
- Cluster Broccoli Nomad Job + Docker Image (for running the Play application)
