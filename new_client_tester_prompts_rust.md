# New Rust Client — Workshop Prompts

Follow these steps to implement the Aerospike Rust client v2 in the workshop app.

**Workshop file:** `rust-server/src/services/workshop_client.rs`

**SDK guide:** [guide-to-rust-sdk.md](guide-to-rust-sdk.md)

**Facilitator materials:** See the companion repo `aerospike-rust-workshop-facilitator` (schedule, debugging labs, gotchas).

---

## Step 1: Connect to the Database

**Goal:** Connect to an existing Aerospike cluster on startup.

**Prompt:** Implement `connect()` in `workshop_client.rs` (STEP 1 block).

**Hints:**
- `ClientPolicy::default()` is a good starting point
- `Client::new(&policy, &hosts).await?`
- Store the client in `Arc<Client>` and save to `self.client`
- Also call `self.reference.set_client(client).await` so pre-implemented methods work

**Validate:**
```bash
AEROSPIKE_CLIENT_PROFILE=workshop ./scripts/run-rust.sh
curl http://localhost:8080/rest/v1/health
```

---

## Step 2: Store a Product Object

**Goal:** Insert a product record (fail if key already exists).

**Prompt:** Implement `store_product()` (STEP 2 block).

**Hints:**
- `WritePolicy { record_exists_action: RecordExistsAction::CreateOnly, .. }`
- `as_key!(NAMESPACE, PRODUCT_SET, product.id)`
- Convert `product.to_bins()` to `Vec<Bin>`

**Validate:**
```bash
curl -X POST "http://localhost:8080/rest/v1/data/create-indexes"
curl -X POST "http://localhost:8080/rest/v1/data/load?dataPath=$(pwd)/data"
curl "http://localhost:8080/rest/v1/data/count"
```

**Optional — peek at raw Aerospike records with `aql`:**

`aql` is not installed by `docker compose`. It ships in the separate [Aerospike Tools](https://aerospike.com/docs/database/tools/overview) package. Use the workshop helper (no local install):

```bash
./scripts/aql.sh -c "SELECT * FROM test.products LIMIT 5"
```

Or skip `aql` and confirm via the UI at http://localhost:5173.

---

## Step 3: Get a Product by ID

**Goal:** Point read by product ID.

**Prompt:** Implement `get_product()` (STEP 3 block).

**Hints:**
- `client.get(&ReadPolicy::default(), &key, Bins::All).await`
- Match `Error::ServerError(ResultCode::KeyNotFoundError, _, _)` → return `None`

**Validate:** Open http://localhost:5173/product/21030 — should show product details. (Start the UI with `./scripts/run-ui-dev.sh` if not already running.)

---

## Step 4: Query for Products

**Goal:** Filter products by a secondary-indexed bin (homepage categories).

**Prompt:** Implement `query()` (STEP 4 block). Remove the stub that returns product `13283`.

**Hints:**
- `Statement::new(NAMESPACE, PRODUCT_SET, Bins::from([...]))`
- `stmt.add_filter(Filter::equal(index, filter_value))`
- `QueryPolicy { max_records: count, .. }`
- Stream with `rs.into_stream()` and `while let Some(rec) = stream.next().await`

**Validate:** Homepage shows Shoes, Bags, Wallets, etc.

---

## Step 5: Execute Advanced Search

**Goal:** Multi-filter search using expression filters.

**Prompt:** Implement `advanced_search()` (STEP 5 block). The filter expression string is printed for discussion — Rust uses the expression builder API, not AEL strings.

**Hints:**
- Pick the first non-empty filter for `Filter::equal()` (secondary index)
- Additional filters via `QueryPolicy.base_policy.filter_expression`
- Use `expressions::and`, `eq`, `string_bin`, `string_val`

**Validate:** Search page filters by category, article type, usage, brand.

---

## Step 6: Get the Cart

**Goal:** Point read a user's shopping cart.

**Prompt:** Implement `get_cart()` (STEP 6 block). Remove the stub cart.

**Hints:**
- Key: `as_key!(NAMESPACE, CARTS_SET, user_id)`
- Return `Cart::default()` on `KeyNotFoundError`

**Validate:** Cart page loads (may be empty initially).

---

## Step 7: Update the Cart with Concurrency

**Goal:** Add items to cart using CDT map ops and generation checks.

**Prompt:** Implement `add_to_cart()` (STEP 7 block).

**Hints:**
- Read cart + capture `record.generation`
- Existing item: `maps::increment_value` with `.context(vec![ctx_map_key(...)])`
- New item: `maps::put` with nested map value
- `WritePolicy { generation_policy: ExpectGenEqual, generation, .. }`
- Retry loop on `ResultCode::GenerationError`

**Validate:** Add items to cart; open two browser tabs and add the same product simultaneously.

---

## Facilitator escape hatch

```bash
AEROSPIKE_CLIENT_PROFILE=workshop-answers ./scripts/run-rust.sh
```
