# Aerospike Retail Demo

A demo retail website powered by Aerospike, showcasing Key-Value operations. Available in **Java** (Spring Boot), **Python** (FastAPI), and **Rust** (Axum). Pick the language you prefer — all share the same React UI and sample data.

## Quick Start

```bash
# 1. Start Aerospike
docker compose up -d

# 2. Choose ONE backend (Java, Python, or Rust) — all use port 8080
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

### Rust workshop

```bash
# Terminal 1 — API only (no frontend build required)
AEROSPIKE_CLIENT_PROFILE=workshop ./scripts/run-rust.sh

# Terminal 2 — shared React UI (auto-installs npm deps on first run)
./scripts/run-ui-dev.sh
```

Open **http://localhost:5173** — the UI proxies API calls to whichever backend is on :8080.

For a single-port demo (UI + API on :8080), build static assets first: `cd website && npm run build:rust`, then open http://localhost:8080.

## The Challenge!

We would love feedback on the Aerospike SDKs! Each backend has a single file where all database access lives:

| Language | Workshop file | SDK guide |
|----------|---------------|-----------|
| Java | `spring-server/.../KeyValueServiceNewClient.java` | [guide-to-java-sdk.md](guide-to-java-sdk.md) |
| Python | `python-server/.../key_value_service_new_client.py` | [guide-to-python-sdk.md](guide-to-python-sdk.md) |
| Rust | `rust-server/src/services/workshop_client.rs` | [guide-to-rust-sdk.md](guide-to-rust-sdk.md) |

Each also has:

- **Old client** — fully working reference using the legacy Aerospike client
- **New client** — skeleton with `TODO:` steps for you to implement
- **Answers** — complete solution for facilitators

### Client profiles

| Profile | Java | Python | Rust |
|---------|------|--------|------|
| Reference (default) | `old-client` | `old-client` | `reference` |
| Workshop | `new-client` | `new-client` | `workshop` |
| Solution | `new-client-answers` | `new-client-answers` | `workshop-answers` |

Facilitator schedule, debugging labs, and gotchas: see the companion repo `~/dev/aerospike-rust-workshop-facilitator`.

## Project Structure

```
aerospike-client-sdk-workshop/
├── spring-server/          # Java Spring Boot backend
├── python-server/          # Python FastAPI backend
├── rust-server/            # Rust Axum backend
├── website/                # React frontend (Vite) — shared by all backends
├── data/                   # Sample product data (auto-loaded on startup)
├── config/aerospike/       # Aerospike server configuration
├── docker-compose.yml      # Local Aerospike container
├── guide-to-java-sdk.md
├── guide-to-python-sdk.md
├── guide-to-rust-sdk.md
├── new_client_tester_prompts.md        # Java workshop steps
├── new_client_tester_prompts_python.md # Python workshop steps
└── new_client_tester_prompts_rust.md   # Rust workshop steps
```

## Technologies

- **Java backend**: Spring Boot 3.5, Java 21, Aerospike Java SDK
- **Python backend**: FastAPI, uvicorn, aerospike-sdk (Python)
- **Rust backend**: Axum, Tokio, aerospike crate v2.1
- **Frontend**: React 18, Vite 5

- **Database**: Aerospike (Key-Value, Secondary Indexes, CDT maps)

## Building the Frontend

The frontend should be pre-built in each backend's static directory. If missing:

```bash
cd website
npm install
npm run build          # copies to spring, python, and rust-server
# Or target one backend:
npm run build:spring
npm run build:python
npm run build:rust
```

## Dev Mode (hot-reload frontend)

Recommended for the Rust workshop — one UI works with Java, Python, or Rust backends:

```bash
# Terminal 1 — run any ONE backend on :8080
AEROSPIKE_CLIENT_PROFILE=reference ./scripts/run-rust.sh

# Terminal 2 — UI with hot reload (installs deps automatically on first run)
./scripts/run-ui-dev.sh
```

Open **http://localhost:5173**. API calls use relative `/rest/...` URLs and are proxied to :8080. No frontend build or CORS setup required.

## Troubleshooting

### What does `./scripts/run-rust.sh` do?

Starts the Rust Axum API server on port **8080** (`cargo run --release` in `rust-server/`). Default profile is `reference` (fully working). Use `AEROSPIKE_CLIENT_PROFILE=workshop` for the exercise skeleton.

### Rust cannot connect (`Seeding host 172.19.0.x:3000 failed`)

Aerospike in Docker advertises its container IP. The Rust server runs on your host and cannot reach that address.

Verify Aerospike advertises localhost:

```bash
docker exec aerospike-workshop asinfo -v 'service'   # must show 127.0.0.1:3000
```

If it shows `172.19.0.x:3000`, ensure `config/aerospike/aerospike.conf` has `access-address 127.0.0.1` under `network.service`, then:

```bash
docker compose restart aerospike
# stop any old Rust server on :8080, then:
AEROSPIKE_CLIENT_PROFILE=reference ./scripts/run-rust.sh
```

### UI dev server already running

`./scripts/run-ui-dev.sh` exits if port 5173 is in use. Either use the existing UI at http://localhost:5173, or stop the old process:

```bash
lsof -i :5173 -sTCP:LISTEN
kill <PID>
```

## Optional: Aerospike CLI tools (`aql`, `asinfo`)

`docker compose up -d` starts the database only — it does **not** install `aql` on your laptop. `aql` ships in the separate Aerospike Tools package and is optional for the workshop.

```bash
./scripts/asinfo.sh -v 'build'                                      # health check (via server container)
./scripts/aql.sh -c "SELECT * FROM test.products LIMIT 3"           # peek at records (via tools image)
```

See `prerequisites.md` in the companion facilitator repo (`~/dev/aerospike-rust-workshop-facilitator`), or use `curl`/the UI instead of `aql`.

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

Docker Compose exposes Aerospike on port **3000**. Pass `--aerospike.port=3000` (Java), `AEROSPIKE_PORT=3000` (Python/Rust), or set in `.env`.
