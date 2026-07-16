# Guide to the Aerospike Rust Client (v2)

Reference for the workshop. Targets **aerospike crate v2.1** (async-first, Tokio).

Official docs: [Aerospike Rust client](https://aerospike.com/docs/develop/client/rust)

---

## Installation

```toml
[dependencies]
aerospike = { version = "2.1", features = ["rt-tokio"] }
tokio = { version = "1", features = ["full"] }
futures = "0.3"
```

At the crate root:

```rust
#[macro_use]
extern crate aerospike;
```

---

## Connect

```rust
use std::sync::Arc;
use aerospike::{Client, ClientPolicy};

let policy = ClientPolicy::default();
let hosts = "127.0.0.1:3000";
let client = Arc::new(Client::new(&policy, hosts).await?);
```

**One `Arc<Client>` per cluster.** Share across all handlers/tasks. Never create a client per HTTP request.

On shutdown: `client.close().await?`

---

## Write (create-only)

```rust
use aerospike::{RecordExistsAction, WritePolicy};

let key = as_key!("test", "products", "41213");
let mut wp = WritePolicy::default();
wp.record_exists_action = RecordExistsAction::CreateOnly;

let bins = [
    as_bin!("name", "CASIO Watch"),
    as_bin!("price", 2995i64),
];
client.put(&wp, &key, &bins).await?;
```

---

## Read

```rust
use aerospike::{Bins, Error, ReadPolicy, ResultCode};

match client.get(&ReadPolicy::default(), &key, Bins::All).await {
    Ok(record) => {
        println!("bins: {:?}", record.bins);
        println!("generation: {}", record.generation);
    }
    Err(Error::ServerError(ResultCode::KeyNotFoundError, _, _)) => {
        // not found
    }
    Err(err) => return Err(err.into()),
}
```

---

## Query (secondary index)

There is **no `scan()` in v2**. Full-set iteration uses `query()` with no filters.

```rust
use aerospike::query::{Filter, PartitionFilter};
use aerospike::{Bins, QueryPolicy, Statement};
use futures::StreamExt;

let mut policy = QueryPolicy::default();
policy.max_records = 10;

let mut stmt = Statement::new("test", "products", Bins::from(["id", "name", "brandName"]));
stmt.add_filter(Filter::equal("subCategory", "Shoes"));

let rs = client.query(&policy, PartitionFilter::all(), stmt).await?;
let mut stream = rs.into_stream();
while let Some(result) = stream.next().await {
    let record = result?;
}
```

---

## Advanced search (expression filters)

Rust has no AEL string DSL. Use the expression builder:

```rust
use aerospike::expressions::{and, eq, string_bin, string_val};

let mut policy = QueryPolicy::default();
policy.base_policy.filter_expression = Some(and(vec![
    eq(string_bin("brandName".to_string()), string_val("Nike".to_string())),
]));

let mut stmt = Statement::new("test", "products", Bins::All);
stmt.add_filter(Filter::equal("category", "Apparel"));
```

---

## CDT map operations (cart)

```rust
use aerospike::operations::cdt_context::ctx_map_key;
use aerospike::operations::maps;
use aerospike::{GenerationPolicy, WritePolicy};

let mut wp = WritePolicy::default();
wp.generation_policy = GenerationPolicy::ExpectGenEqual;
wp.generation = record.generation;

let op = maps::increment_value(
    &MapPolicy::default(),
    "items",
    as_val!("quantity"),
    as_val!(1i64),
).context(vec![ctx_map_key(as_val!("product-id-123"))]);

client.operate(&wp, &key, &[op]).await?;
```

Retry on `ResultCode::GenerationError` for optimistic locking.

---

## Running the workshop

```bash
# Terminal 1 — Rust API
AEROSPIKE_CLIENT_PROFILE=workshop ./scripts/run-rust.sh

# Terminal 2 — React UI (no build step; auto-installs on first run)
./scripts/run-ui-dev.sh
```

Open **http://localhost:5173**. Validate API steps with `curl http://localhost:8080/rest/v1/...` or through the UI.

**Optional:** `./scripts/aql.sh -c "SELECT * FROM test.products LIMIT 3"` to inspect raw records. `aql` is not installed by Docker — the script runs it from the `aerospike/aerospike-tools` image. See facilitator `prerequisites.md`.

---

## Logging

From the **repo root** (same as the run commands above):

```bash
RUST_LOG=info AEROSPIKE_CLIENT_PROFILE=workshop ./scripts/run-rust.sh
RUST_LOG=aerospike_core=debug AEROSPIKE_CLIENT_PROFILE=workshop ./scripts/run-rust.sh
RUST_LOG=aerospike_rust_workshop=debug AEROSPIKE_CLIENT_PROFILE=workshop ./scripts/run-rust.sh
```

### Adding logs in `workshop_client.rs`

The `tracing` crate is already in `Cargo.toml` — no extra dependency. Use macros directly:

```rust
tracing::info!(user_id, product_id, quantity, "Added to cart");
tracing::debug!(product_id, "Product found");
tracing::warn!(index, "Index creation failed");
tracing::error!(?err, "Query failed");
```

| Level | When to use | Visible at default `RUST_LOG=info`? |
|-------|-------------|-------------------------------------|
| `error!` | Unrecoverable failures | Yes |
| `warn!` | Handled surprises | Yes |
| `info!` | Key workflow events (connect, query done) | Yes |
| `debug!` | Per-operation detail (found/miss, filter expr) | No — need `RUST_LOG=debug` or `aerospike_rust_workshop=debug` |

Structured fields (`user_id`, `product_id = %id`) show up in log output and are easy to filter on. The subscriber is initialized in `main.rs` from the `RUST_LOG` env var.

---

## Sync vs async

| Mode | Feature flags | Notes |
|------|---------------|-------|
| **Async** (default) | `features = ["rt-tokio"]` | All methods `.await` |
| **Sync** | `features = ["rt-tokio", "sync"]` | Blocking wrapper; Tokio still required |

---

## Feature gaps vs Java / C

| Feature | Rust v2 |
|---------|---------|
| Distributed ACID transactions | Roadmap |
| AEL string DSL | Not available |
| `Client.scan()` | Removed |
| Batch, CDT, TLS | Yes |
