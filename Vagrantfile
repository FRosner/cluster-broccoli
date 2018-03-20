# -*- mode: ruby -*-
# vi: set ft=ruby :

BASE_IMAGE="bento/ubuntu-14.04" 
VM_HOSTONLY_STATIC_IP="192.168.56.11" # HostOnly network

$script = <<SCRIPT

# Set env var for assigned static IP
export VM_HOSTONLY_STATIC_IP=#{VM_HOSTONLY_STATIC_IP}

# Set versions
NOMAD_VERSION=0.5.4
CONSUL_VERSION=0.7.5

# Update apt and get dependencies
sudo apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y unzip curl vim \
    apt-transport-https \
    ca-certificates \
    software-properties-common 

echo "Fetching Consul..."
curl -sSL https://releases.hashicorp.com/consul/${CONSUL_VERSION}/consul_${CONSUL_VERSION}_linux_amd64.zip -o consul.zip

echo "Installing Consul..."
unzip consul.zip
sudo cp consul /usr/bin/consul

echo "Fetching Nomad..."
cd /tmp/
curl -sSL https://releases.hashicorp.com/nomad/${NOMAD_VERSION}/nomad_${NOMAD_VERSION}_linux_amd64.zip -o nomad.zip

echo "Installing Nomad..."
unzip nomad.zip
sudo install nomad /usr/bin/nomad

sudo mkdir -p /etc/nomad.d
sudo chmod a+w /etc/nomad.d

# Set hostname's IP to made advertisement Just Work
sudo sed -i -e "s/.*nomad.*/$(ip route get 1 | awk '{print $NF;exit}') nomad/" /etc/hosts

echo "Installing Docker..."
if [[ -f /etc/apt/sources.list.d/docker.list ]]; then
    echo "Docker repository already installed; Skipping"
else
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
    sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
    sudo apt-get update
fi
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y docker-ce

# Restart docker to make sure we get the latest version of the daemon if there is an upgrade
sudo service docker restart

# Make sure we can actually use docker as the vagrant user
sudo usermod -aG docker vagrant

echo "Configuring Nomad..."
(
cat <<-EOF
	{ 
    "server": {
        "enabled": true
    },
    "client": {
        "enabled": true
    },
    "data_dir": "/tmp",
    "bind_addr": "#{VM_HOSTONLY_STATIC_IP}",
    "advertise": {
        "http": "#{VM_HOSTONLY_STATIC_IP}:4646",
        "rpc": "#{VM_HOSTONLY_STATIC_IP}:4647",
        "serf": "#{VM_HOSTONLY_STATIC_IP}:4648"
    },
    "consul": { 
        "address": "#{VM_HOSTONLY_STATIC_IP}:8500" 
    },
    "enable_debug": true 
}
EOF
) | sudo tee /etc/nomad.d/local.json

for bin in cfssl cfssl-certinfo cfssljson
do
	echo "Installing $bin..."
	curl -sSL https://pkg.cfssl.org/R1.2/${bin}_linux-amd64 > /tmp/${bin}
	sudo install /tmp/${bin} /usr/local/bin/${bin}
done

SCRIPT

Vagrant.configure(2) do |config|
  config.vm.box = BASE_IMAGE
  config.vm.hostname = "nomad"
  config.vm.provision "shell", inline: $script, privileged: false
  config.vm.provision "docker" # Just install it
  
  # Expose the consul DNS api 
  config.vm.network "forwarded_port", guest_ip: VM_HOSTONLY_STATIC_IP, guest: 8600, host_ip: "127.0.0.1", host: 8600, auto_correct: false
  
  # Expose the consul api and ui to the host
  config.vm.network "forwarded_port", guest_ip: VM_HOSTONLY_STATIC_IP, guest: 8500, host_ip: "127.0.0.1", host: 8500, auto_correct: false

  # Expose the nomad agent http api to the host
  config.vm.network "forwarded_port", guest_ip: VM_HOSTONLY_STATIC_IP, guest: 4646, host_ip: "127.0.0.1", host: 4646, auto_correct: false
 
  # Expose the nomad agent rpc api to the host
  config.vm.network "forwarded_port", guest_ip: VM_HOSTONLY_STATIC_IP, guest: 4647, host_ip: "127.0.0.1", host: 4647, auto_correct: false
 
  # Expose the nomad agent http api to the host
  config.vm.network "forwarded_port", guest_ip: VM_HOSTONLY_STATIC_IP, guest: 4648, host_ip: "127.0.0.1", host: 4648, auto_correct: false
 
  # Set up network for nomad node
  config.vm.network "private_network", ip: VM_HOSTONLY_STATIC_IP


  # Increase memory for Parallels Desktop
  config.vm.provider "parallels" do |p, o|
    p.memory = "1024"
  end

  # Increase memory for Virtualbox
  config.vm.provider "virtualbox" do |vb|
        vb.memory = "1024"
  end

  # Increase memory for VMware
  ["vmware_fusion", "vmware_workstation"].each do |p|
    config.vm.provider p do |v|
      v.vmx["memsize"] = "1024"
    end
  end

  config.vm.provision "Infrastructure services",
    type:"shell", 
    inline: " (/usr/bin/consul agent -dev -ui -data-dir=/tmp -advertise=#{VM_HOSTONLY_STATIC_IP} -bind=#{VM_HOSTONLY_STATIC_IP} -client=#{VM_HOSTONLY_STATIC_IP} | sed -u 's/^/[CONSUL]/' ) & /usr/bin/nomad agent -dev -config=/etc/nomad.d/local.json | sed -u 's/^/[NOMAD] /' ", 
    privileged: true, 
    run:"always"

end