# Ergo Blockchain Explorer Backend

## Project Overview

This is a Scala-based backend system for the Ergo Blockchain Explorer, consisting of four main microservices that work together to scan, index, and serve blockchain data. The project is built using Scala 2.12.15 with SBT as the build tool and follows functional programming principles using libraries like Cats Effect, fs2, and Tofu.

### Architecture Components

1. **Chain Grabber** (`modules/chain-grabber`)
   - Scans the blockchain and aggregates data to the PostgreSQL database
   - Dependencies: PostgreSQL (with schema defined in V9__Schema.sql)

2. **Explorer API** (`modules/explorer-api`)
   - Provides HTTP API methods for querying chain and off-chain data
   - API Specification: https://api.ergoplatform.com/docs/openapi
   - Dependencies: PostgreSQL, Redis

3. **UTX Tracker** (`modules/utx-tracker`)
   - Dumps unconfirmed transactions from the mempool to a local database
   - Dependencies: PostgreSQL

4. **UTX Broadcaster** (`modules/utx-broadcaster`)
   - Broadcasts unconfirmed transactions to a set of known peers
   - Dependencies: Redis

5. **Migrator** (`modules/migrator`)
   - Handles database migrations and schema updates

### Technology Stack

- **Language**: Scala 2.12.15
- **Build Tool**: SBT
- **Web Framework**: Http4s
- **API Documentation**: Tapir with OpenAPI
- **JSON Processing**: Circe
- **Database**: PostgreSQL with Doobie (functional database library)
- **Database Migrations**: Flyway
- **Functional Programming**: Cats Effect, fs2, Tofu
- **Configuration**: PureConfig
- **Logging**: SLF4J with Logback
- **Redis Client**: Redis4Cats
- **Testing**: ScalaTest, TestContainers

### Database Schema

The system uses PostgreSQL with a comprehensive schema that includes tables for:
- Block headers and metadata
- Transactions (confirmed and unconfirmed)
- Transaction inputs and outputs
- Assets and tokens
- Mining statistics
- Smart contract data (registers, constants, proofs)

## Building and Running

### Prerequisites
- Java 8+ (OpenJDK 8 recommended)
- SBT
- PostgreSQL server
- Redis server

### Build Commands

1. **Build individual service JARs**:
   ```bash
   sbt <service-name>/assembly
   ```
   Where `<service-name>` can be:
   - `grabber` (for chain-grabber)
   - `httpApi` (for explorer-api)
   - `utxTracker` (for utx-tracker)
   - `utxBroadcaster` (for utx-broadcaster)
   - `migrator` (for migrator)

2. **Run individual services**:
   ```bash
   sbt <service-name>/run
   ```

3. **Run tests**:
   ```bash
   sbt test
   ```

4. **Clean build**:
   ```bash
   sbt clean
   ```

### Docker Support

Each service has a Dockerfile for containerized deployment. The Dockerfiles use a multi-stage build process to create minimal images.

### Configuration

Configuration files are located in `modules/<service-name>/src/main/resources/application.conf` and include:
- Database connection settings
- Redis connection settings
- Network parameters for connecting to Ergo nodes
- API server settings
- Polling intervals for blockchain scanning

### Environment Setup

1. **Database Setup**:
   - Install PostgreSQL
   - Create a database named `explorer`
   - Configure credentials in application.conf files

2. **Redis Setup**:
   - Install Redis
   - Configure connection in application.conf files

3. **Network Configuration**:
   - Modify the `network.master-nodes` parameter in application.conf files to connect to the desired Ergo network (mainnet, testnet, etc.)

### TestNet Configuration

To connect to TestNet, modify the `.conf` parameters in modules to:

```application.conf
network.master-nodes = ["http://195.201.82.115:9052"]

protocol.network-prefix = 16
protocol.genesis-address = "AfYgQf5PappexKq8Vpig4vwEuZLjrq7gV97BWBVcKymTYqRzCoJLE9cDBpGHvtAAkAgQf8Yyv7NQUjSphKSjYxk3dB3W8VXzHzz5MuCcNbqqKHnMDZAa6dbHH1uyMScq5rXPLFD5P8MWkD5FGE6RbHKrKjANcr6QZHcBpppdjh9r5nra4c7dsCgULFZfWYTaYqHpx646BUHhhp8jDCHzzF33G8XfgKYo93ABqmdqagbYRzrqCgPHv5kxRmFt7Y99z26VQTgXoEmXJ2aRu6LoB59rKN47JxWGos27D79kKzJRiyYNEVzXU8MYCxtAwV"
```

## Development Conventions

### Code Formatting
- ScalaFmt is configured with max column width of 120
- Follows `defaultWithAlign` style with custom alignment rules
- Continuation indent is set to 2 spaces

### Testing
- Unit tests using ScalaTest
- Integration tests using TestContainers for PostgreSQL
- Property-based testing capabilities through ScalaCheck integration

### Functional Programming Patterns
- Use of Cats Effect for effect management
- Functional error handling with Either and Option types
- Immutable data structures throughout
- Type-safe configuration with PureConfig

### Database Access
- Functional database access using Doobie
- SQL migration management with Flyway
- Type-safe queries and mappings

## Deployment

The system can be deployed using:
1. SBT assembly JARs with `java -jar <service>-assembly-*.jar`
2. Docker containers built from the provided Dockerfiles
3. Docker Compose for local development (includes PostgreSQL and Redis services)

## Ergo Bootstrap Integration

This project is included in [ergo bootstrap](https://github.com/ergoplatform/ergo-bootstrap), which helps deploy an Ergo blockchain cluster with useful tools for dApp development.