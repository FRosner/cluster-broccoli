job "{{id}}" {
  datacenters = ["dc1"]

  task "zeppelin" {
    driver = "raw_exec"

    artifact {
      source = "https://raw.githubusercontent.com/FRosner/nomad-docker-wrapper/1.0.0/nomad-docker-wrapper"
    }

    env {
      NOMAD_DOCKER_CONTAINER_NAME = "{{id}}"
    }

    config {
      command = "nomad-docker-wrapper"
      args = ["-p", "8080:8080",
              "-v", "${NOMAD_TASK_DIR}/notebooks:/usr/local/zeppelin/notebooks",
              "frosner/zeppelin"]
    }

    service {
      port = "ui"
      name = "id-ui" # FIXME needs to be changed to ${id} after JSON conversion
    }

    resources {
      cpu = 2000
      memory = 1024
      network {
        mbits = 10
        port "ui" {
          static = 8080
        }
      }
    }
  }
}
