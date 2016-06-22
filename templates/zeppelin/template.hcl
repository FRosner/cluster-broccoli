job "{{id}}" {
  datacenters = ["dc1"]

  type = "service"

  constraint {
    attribute = "${attr.kernel.name}"
    value = "linux"
    distinct_hosts = true
  }

  update {
    stagger = "10s"
    max_parallel = 1
  }

  task "zeppelin" {
    driver = "raw_exec"

    artifact {
      source = "https://raw.githubusercontent.com/FRosner/docker-zeppelin/master/zeppelin-docker-wrapper"
    }

    env {
      ZEPPELIN_CONTAINER_NAME = "{{id}}"
      ZEPPELIN_NOTEBOOK_MOUNT = "${NOMAD_TASK_DIR}/notebooks"
    }

    config {
      command = "zeppelin-docker-wrapper"
    }

    resources {
      cpu = 500
      memory = 512
      network {
        mbits = 10
        port "ui" {}
      }
    }

    service {
      name = "id-ui" // FIXME replace id with {{id}} after json conversion
      tags = []
      port = "ui"
      check {
        type = "http"
        path = "/"
        interval = "10s"
        timeout = "2s"
      }
    }

    kill_timeout = "20s"
  }
}

