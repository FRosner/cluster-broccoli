job "zeppelin-${id}" {
  datacenters = ["dc1"]
  task "zeppelin" {
    driver = "docker"

    config {
      image = "frosner/zeppelin"
    }

    resources {
      cpu = 500
      memory = 256
      network {
        mbits = 10
        port "ui" {}
      }
    }
  }
}
