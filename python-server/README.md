# Aerospike Retail Demo — Python Backend

FastAPI backend for the Aerospike Python SDK workshop. Shares the React UI and sample data with the Java Spring Boot backend.

## Quick Start

```bash
# From repo root — start Aerospike
docker compose up -d

# Install and run (legacy client — fully working)
cd python-server
pip install -e .
AEROSPIKE_PORT=3000 uvicorn aerospikeworkshop.main:app --host 0.0.0.0 --port 8080

# Workshop mode — edit key_value_service_new_client.py
AEROSPIKE_CLIENT_PROFILE=new-client AEROSPIKE_PORT=3000 \
  uvicorn aerospikeworkshop.main:app --host 0.0.0.0 --port 8080

# Solution key (for facilitators)
AEROSPIKE_CLIENT_PROFILE=new-client-answers AEROSPIKE_PORT=3000 \
  uvicorn aerospikeworkshop.main:app --host 0.0.0.0 --port 8080
```

Open http://localhost:8080

## Client Profiles

| Profile | Service file | Purpose |
|---------|--------------|---------|
| `old-client` (default) | `key_value_service_old_client.py` | Legacy `aerospike` client — working reference |
| `new-client` | `key_value_service_new_client.py` | Workshop skeleton with TODO steps |
| `new-client-answers` | `key_value_service_new_client_answers.py` | Complete solution |

## Workshop File

Edit `src/aerospikeworkshop/services/key_value_service_new_client.py` and follow the TODO steps. See [guide-to-python-sdk.md](../guide-to-python-sdk.md) and [new_client_tester_prompts_python.md](../new_client_tester_prompts_python.md).

## Frontend

Build the shared React UI into `static/`:

```bash
cd ../website
npm install
npm run build:python
```

Or build for both backends: `npm run build`

## Dev Mode

```bash
# Terminal 1 — backend with hot reload
AEROSPIKE_CLIENT_PROFILE=new-client uvicorn aerospikeworkshop.main:app --reload --port 8080

# Terminal 2 — Vite dev server (proxies /rest to :8080)
cd ../website && npm run dev
```
