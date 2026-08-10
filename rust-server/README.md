# Aerospike Rust Workshop Server

Axum backend for the Aerospike Rust client SDK workshop. Shares the same React UI and REST API as the Java and Python servers.

## Prerequisites

- Rust 1.87+
- Aerospike Server 6.4+ (via `docker compose up -d` from repo root)
- Node.js 18+ (only for the separate UI dev server — see below)

## Quick start (recommended: separate UI)

No frontend build required. Run the API and UI in two terminals:

```bash
# Terminal 1 — from repo root
docker compose up -d
AEROSPIKE_CLIENT_PROFILE=reference ./scripts/run-rust.sh

# Terminal 2 — from repo root
./scripts/run-ui-dev.sh
```

Open **http://localhost:5173**. The UI proxies `/rest` to the backend on :8080.

## Alternative: single-port demo

If you want everything on :8080 (like Java/Python quick start):

```bash
cd website && npm run build:rust
AEROSPIKE_CLIENT_PROFILE=reference ./scripts/run-rust.sh
```

Open http://localhost:8080

## Client profiles

| Profile | Env var | Purpose |
|---------|---------|---------|
| `reference` (default) | `AEROSPIKE_CLIENT_PROFILE=reference` | Fully working demo |
| `workshop` | `AEROSPIKE_CLIENT_PROFILE=workshop` | Participant skeleton with TODOs |
| `workshop-answers` | `AEROSPIKE_CLIENT_PROFILE=workshop-answers` | Facilitator solution |

Legacy aliases `old-client`, `new-client`, and `new-client-answers` also work.

## Workshop file

Participants implement:

```
rust-server/src/services/workshop_client.rs
```

Steps 1–7 cover: connect, store product, get product, query, advanced search, get cart, add to cart with generation locking.

Pre-implemented (not workshop steps): cart update/remove/clear, category metadata, data loading, index creation.

## SDK guide

See [guide-to-rust-sdk.md](../guide-to-rust-sdk.md) and [new_client_tester_prompts_rust.md](../new_client_tester_prompts_rust.md).

## Logging

From the **repo root**:

```bash
RUST_LOG=info AEROSPIKE_CLIENT_PROFILE=workshop ./scripts/run-rust.sh
RUST_LOG=debug AEROSPIKE_CLIENT_PROFILE=workshop ./scripts/run-rust.sh
RUST_LOG=aerospike_core=debug AEROSPIKE_CLIENT_PROFILE=workshop ./scripts/run-rust.sh
```

Or from `rust-server/` directly: `RUST_LOG=info cargo run --release` (and set `AEROSPIKE_CLIENT_PROFILE` as needed).

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `AEROSPIKE_HOST` | `localhost` | Aerospike host |
| `AEROSPIKE_PORT` | `3000` | Aerospike port |
| `AEROSPIKE_CLIENT_PROFILE` | `reference` | Client implementation |
| `SERVER_PORT` | `8080` | HTTP server port |
| `RUST_LOG` | `info` | Log filter |
