job "zeppelin-${id}" {
  datacenters = ["dc1"]

  # todo make this on demand based on the template folder or parse it and use as template name?
  name = "zeppelin"

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
