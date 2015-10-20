Vagrant.configure("2") do |config|
    config.vm.box = "ubuntu/trusty64"

    config.vm.network "private_network", ip: "192.168.50.4"
    config.vm.synced_folder ".", "/vagrant"

    config.vm.provision :docker
    config.vm.provision :docker_compose, yml: "/vagrant/docker-compose.yml", rebuild: true, project_name: "music_scraper", run: "always"
end
