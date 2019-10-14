job "{{id}}" {
    region = "global"
    type = "service"
    datacenters = ["dc1"]
    update {
        stagger = 10000000
        max_parallel = 2
    }
    group "http-server" {
        count = 1
        task "http-task" {
            driver = "raw_exec"
            config {
                args = ["-m", "SimpleHTTPServer"]
                command = "/usr/bin/python"
            }
            service {
                name = "{{id}}-web-ui-1"
                port = "ui"
                tags = ["protocol-http", "{{secret}}"]
                check {
                    name = "service: \"{{id}}-ui-1\" check"
                    type = "http"
                    path = "/"
                    protocol = ""
                    interval = 10000000000
                    timeout = 2000000000
                }
            }
            service {
                name = "{{id}}-web-ui-2"
                port = "ui"
                tags = ["protocol-http", "{{secret}}"]
                check {
                    name = "service: \"{{id}}-ui-2\" check"
                    type = "http"
                    path = "/doesnotexist"
                    protocol = ""
                    interval = 10000000000
                    timeout = 2000000000
                }
            }
            resources {
                memory = 128
                disk = 300
                network {
                    mbits = 10
                    port "ui" {
                        static = 8000
                    }
                }
                cpu = {{cpu}}
            }
            logs {
                max_files     = 10
                max_file_size = 10
            }
        }
        restart {
            interval = 60000000000
            attempts = 2
            delay    = 15000000000
            mode     = "delay"
        }
    }
}