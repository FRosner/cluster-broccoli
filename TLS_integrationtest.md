# How to get Broccoli to run with TLS Nomad

### Broccoli Configuration

We run cluster-broccoli like this:
```
docker run --rm -d --net host sunygit/cluster-broccoli-test cluster-broccoli -Dconfig.file="/application-tls.conf" -Dbroccoli.nomad.url=https://localhost:4646 
```
So it knows that is should speak https with nomad. The application-tls.conf is standard play stuff:
```
    ws.ssl {
      trustManager = {
        stores = [
          { type = "PEM", path = "/nomad-ca.pem" }
        ]
      }
      keyManager = {
        stores = [
          { type = "JKS", path = "/broccoli.global.nomad.jks", password = "secret" }
        ]
      }
      debug = {
        ssl = true
        trustmanager = true
        keymanager = true
      }
    }
play.ws.ssl.loose.acceptAnyCertificate=true

```
### Certificate generation
For the integration tests, we go with self signed certs.  The installation of cfssl failed on amazon linux due to missing 32 bit libraries, so using vault:

1. Install vault, start the server in dev mode, export VAULT\_ADDR='http://127.0.0.1:8200'
2. Create CA, as in https://www.vaultproject.io/docs/secrets/pki/index.html
```
### create and configure certificate authority
./vault secrets enable pki
./vault secrets tune -max-lease-ttl=87600h pki
./vault write pki/root/generate/internal \
  common_name=integration-test-root-ca ttl=87600h > nomad-ca
./vault write pki/config/urls \
    issuing_certificates="http://127.0.0.1:8200/v1/pki/ca" \
    crl_distribution_points="http://127.0.0.1:8200/v1/pki/crl"
### create role
./vault write pki/roles/integrationtest-role \
    allowed_domains=integrationtest \
    allow_subdomains=true \
    max_ttl=87600h
### create nomad server certificate
./vault write pki/issue/integrationtest-role common_name=nomad-server.integrationtest \
  ttl=87000h alt_names=localhost ip_sans=127.0.0.1 > server
### create nomad cli certificate for broccoli to use
./vault write pki/issue/integrationtest-role common_name=nomad-cli.integrationtest \
  ttl=87000h alt_names=localhost ip_sans=127.0.0.1 > cli
### cut the server.pem, server-key.pem files into usable pems
### cut the cli certificate and convert to jks
openssl pkcs12 -export -inkey cli-key.pem -in cli-chain.pem -name client.global.nomad -out broccoli.global.nomad.p12
## keep the export password for the following step
keytool -importkeystore -srckeystore broccoli.global.nomad.p12 -srcstoretype pkcs12 -destkeystore broccoli.global.nomad.jks
## keep the keystore password for the broccoli configuration
## add the keystore and the pems to the docker images, add stanza in application config
```
The integration tests shamelessly drop all verification - don't do that in production. nomad uses
```
  verify_server_hostname = false
  verify_https_client    = false
```
cluster-broccoli uses `play.ws.ssl.loose.acceptAnyCertificate=true`, and curl uses "-k".

Note for debugging: It's useful to push the combined dockercontainer to docker hub for debugging. Add this to the travis.yml after the `docker build` (and add the variables to your travis-ci):
```
        - docker tag ynux/cluster-broccoli-test $DOCKER_USERNAME/cluster-broccoli-test
        - docker push $DOCKER_USERNAME/cluster-broccoli-test
```
