# Aerospike Retail Demo

A demo retail website powered by Aerospike, showcasing Key-Value operations with a modern Spring Boot + React architecture. This will use the newer, fluent API and is set up as a challenge.

## Quick Start

```bash
# 1. Start Aerospike
docker compose up -d

# 2. Install the fluent client (if not already installed)
cd external_jars && ./registerJars.sh && cd ..

# 3. Build and run the application
cd spring-server
mvn clean package -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=new-client

# 4. Open http://localhost:8080
```

Sample data loads automatically on first startup (~150 products). No manual data loading required.

## The Challenge!

We would love to get feedback on the fluent API! A guide to getting started with the API can be [found here](guide-to-fluent-apis.md). This workshop contains a fully-working retail / shopping cart application, with the entire database access encoded in a single file. This file can be found in `com.aerospikeworkshop.service`.

There are actually two files of interest in this package:
1. `KeyValueServiceOldClient` contains the fully working code in the existing (non-fluent) Aerospike client.
2. `KeyValueServiceNewClient` contains the skeleton code to be coded in the new API with big `TODO:` comments detailing what needs to be done.

Any comments or questions, please leave github comments associated with either this repository or the [aerospike-fluent-client-java](https://github.com/aerospike-community/aerospike-fluent-client-java).

## Project Structure

```
retail-demo/
├── spring-server/          # Spring Boot backend (Java 21)
├── website/                # React frontend (Vite)
├── external_jars/          # External JAR files (tracked in Git)
├── data/                   # Sample product data (auto-loaded on startup)
│   └── styles/             # Product JSON files
├── config/                 # Configuration files
│   └── aerospike/          # Aerospike server configuration
├── docker-compose.yml      # Local Aerospike container
└── guide-to-fluent-apis.md # Fluent API reference
```

## Technologies

- **Backend**: Spring Boot 3.2, Java 21, Aerospike Client 9.1.0
- **Frontend**: React 18, Vite 5, React Router DOM
- **Database**: Aerospike (Key-Value operations, Secondary Indexes)
- **Build**: Maven (Java), npm/yarn (React)

## Running with the Legacy Client

To run with the old (non-fluent) Aerospike client instead:

```bash
mvn spring-boot:run
```

The default profile uses the legacy client. Use `-Dspring-boot.run.profiles=new-client` for the fluent API version.

## Optional: Building the Frontend

The frontend should be pre-built in `spring-server/src/main/resources/static/`. If it's missing:

```bash
cd website
npm install
npm run build
```

## Data Management

Sample data loads automatically when the app starts with an empty database. You can also manage data manually:

```bash
# Reload data
curl -X POST "http://localhost:8080/rest/v1/data/load?dataPath=$(cd ../data && pwd)"

# Check product count
curl "http://localhost:8080/rest/v1/data/count"

# Clear all data
curl -X DELETE "http://localhost:8080/rest/v1/data/clear?confirm=yes-delete-all"
```
