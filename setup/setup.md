# Ergo Blockchain Explorer

## Quick Start

1. Install [Docker](https://www.docker.com/docker-mac)
2. In project Directory navigate to `/setup`
3. Run `docker-compose build` to setup project modules, redis & postgres database
4. Run `docker-compose up` to start all modules

## Notes
1. To start a single module run `docker-compose up <module-name>`, start module dependencies with same command
2. The above configuration will get your local ergo explorer application pointing at `Mainnet`
3. To get started on `TestNet` edit configuration files in `<module-name>/src/main/resources/application.conf` to match testnet config in `setup/config/config.md`