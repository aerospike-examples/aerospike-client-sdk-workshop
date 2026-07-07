# Aerospike Retail Demo

A demo retail website powered by Aerospike, showcasing Key-Value operations. Available in **Java** (Spring Boot) and **Python** (FastAPI). Pick the language you prefer — both share the same React UI and sample data.

## Quick Start

```bash
# 1. Start Aerospike
docker compose up -d

# 2. Choose ONE backend (Java OR Python) — both use port 8080
```

### Java workshop

```bash
cd spring-server
mvn clean package -DskipTests
java -jar target/aerospike-client-sdk-workshop-1.0.0.jar \
  --spring.profiles.active=new-client --aerospike.port=3000

# Or use the helper script from repo root:
# AEROSPIKE_PORT=3000 SPRING_PROFILES_ACTIVE=new-client ./scripts/run-java.sh
```

### Python workshop

```bash
cd python-server
pip install -e .
AEROSPIKE_CLIENT_PROFILE=new-client AEROSPIKE_PORT=3000 \
  uvicorn aerospikeworkshop.main:app --host 0.0.0.0 --port 8080

# Or use the helper script from repo root:
# AEROSPIKE_CLIENT_PROFILE=new-client ./scripts/run-python.sh
```

Open http://localhost:8080 — sample data loads automatically on first startup (~195 products).

## The Challenge!

We would love feedback on the Aerospike SDKs! Each backend has a single file where all database access lives:

| Language | Workshop file | SDK guide |
|----------|---------------|-----------|
| Java | `spring-server/.../KeyValueServiceNewClient.java` | [guide-to-java-sdk.md](guide-to-java-sdk.md) |
| Python | `python-server/.../key_value_service_new_client.py` | [guide-to-python-sdk.md](guide-to-python-sdk.md) |

Each also has:

- **Old client** — fully working reference using the legacy Aerospike client
- **New client** — skeleton with `TODO:` steps for you to implement
- **Answers** — complete solution for facilitators

### Client profiles

| Profile | Java (`--spring.profiles.active`) | Python (`AEROSPIKE_CLIENT_PROFILE`) |
|---------|-----------------------------------|---------------------------------------|
| Legacy (default) | `old-client` | `old-client` |
| Workshop | `new-client` | `new-client` |
| Solution | `new-client-answers` | `new-client-answers` |

## Project Structure

```
aerospike-client-sdk-workshop/
├── spring-server/          # Java Spring Boot backend
├── python-server/          # Python FastAPI backend
├── website/                # React frontend (Vite) — shared by both backends
├── data/                   # Sample product data (auto-loaded on startup)
├── config/aerospike/       # Aerospike server configuration
├── docker-compose.yml      # Local Aerospike container
├── guide-to-java-sdk.md
├── guide-to-python-sdk.md
├── new_client_tester_prompts.md        # Java workshop steps
└── new_client_tester_prompts_python.md # Python workshop steps
```

## Technologies

- **Java backend**: Spring Boot 3.5, Java 21, Aerospike Java SDK
- **Python backend**: FastAPI, uvicorn, aerospike-sdk (Python)
- **Frontend**: React 18, Vite 5
- **Database**: Aerospike (Key-Value, Secondary Indexes, CDT maps)

## Building the Frontend

The frontend should be pre-built in each backend's static directory. If missing:

```bash
cd website
npm install
npm run build          # copies to both spring-server and python-server
# Or target one backend:
npm run build:spring   # spring-server only
npm run build:python   # python-server only
```

## Dev Mode (hot-reload frontend)

```bash
# Terminal 1 — run either Java or Python backend on :8080
# Terminal 2:
cd website && npm run dev   # http://localhost:5173, proxies /rest → :8080
```

## Data Management

```bash
# Reload data
curl -X POST "http://localhost:8080/rest/v1/data/load?dataPath=$(pwd)/data"

# Check product count
curl "http://localhost:8080/rest/v1/data/count"

# Clear all data
curl -X DELETE "http://localhost:8080/rest/v1/data/clear?confirm=yes-delete-all"
```

## Aerospike Port

Docker Compose exposes Aerospike on port **3000**. Pass `--aerospike.port=3000` (Java) or `AEROSPIKE_PORT=3000` (Python) when connecting to the local container.
