# Aerospike Retail Demo

A demo retail website powered by Aerospike, showcasing Key-Value operations with a modern Spring Boot + React architecture. This will use the newer Java SDK and is set up as a challenge.

## Quick Start

```bash
# 1. Start Aerospike (use "docker-compose" if "docker compose" is not available)
docker compose up -d

# 2. Install the Java SDK (if not already installed)
mkdir temp
cd temp
git clone https://github.com/aerospike/aerospike-client-java-sdk.git
cd aerospike-client-java-sdk
mvn clean install -DskipTests
cd ../..
rm -rf temp

# 3. Build and run the application
cd spring-server
mvn clean package -DskipTests
java -jar target/aerospike-client-sdk-workshop-1.0.0.jar --print.profiles.active=new-client
# add extra parameters needed to coonect to the cluster, eg --aerospike.port=3100

# 4. Open http://localhost:8080
```

Sample data loads automatically on first startup (~150 products). No manual data loading required.

## The Challenge!

We would love to get feedback on the Java SDK! A guide to getting started with the API can be [found here](guide-to-java-sdk.md). This workshop contains a fully-working retail / shopping cart application, with the entire database access encoded in a single file. This file can be found in `com.aerospikeworkshop.service`.

There are actually two files of interest in this package:
1. `KeyValueServiceOldClient` contains the fully working code in the legacy Aerospike client.
2. `KeyValueServiceNewClient` contains the skeleton code to be coded in the new SDK with big `TODO:` comments detailing what needs to be done.

Any comments or questions, please leave github comments associated with either this repository or the [aerospike-client-java-sdk](https://github.com/aerospike/aerospike-client-java-sdk).

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
└── guide-to-java-sdk.md    # Java SDK reference
```

## Technologies

- **Backend**: Spring Boot 3.2, Java 21, Aerospike Client 9.1.0
- **Frontend**: React 18, Vite 5, React Router DOM
- **Database**: Aerospike (Key-Value operations, Secondary Indexes)
- **Build**: Maven (Java), npm/yarn (React)

## Running with the Legacy Client

To run with the legacy Aerospike client instead:

```bash
mvn spring-boot:run
```

The default profile uses the legacy client. Use `-Dspring-boot.run.profiles=new-client` for the new Java SDK version.

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
