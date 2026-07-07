# New Python Client - Usability Test Prompts

This document guides you through the usability test for the new Aerospike Python SDK. Follow the steps below. At each step, complete the task on your own; ask for help if you get stuck.

---

## Step 1: Connect to the DB and Create a Session

**Goal**: Update the code to connect to an existing database.

**File to update**: `python-server/src/aerospikeworkshop/services/key_value_service_new_client.py`

Find the comment block for `TODO: STEP 1: VALIDATE THE CONNECTION` in the `connect()` method.

**How to validate**:

```bash
cd python-server
pip install -e .
AEROSPIKE_CLIENT_PROFILE=new-client AEROSPIKE_PORT=3000 \
  uvicorn aerospikeworkshop.main:app --port 8080
```

The application should start without connection errors.

---

## Step 2: Store a Product Object

**Goal**: Implement storing a single product into the database.

**File to update**: `key_value_service_new_client.py` — `TODO: STEP 2: STORE A PRODUCT OBJECT`

**How to validate**:

```bash
# From repo root
curl -X POST "http://localhost:8080/rest/v1/data/create-indexes"
curl -X POST "http://localhost:8080/rest/v1/data/load?dataPath=$(pwd)/data"
aql -c "SELECT * FROM test.products LIMIT 5"
```

You should see product records returned.

---

## Step 3: Get a Product by ID

**Goal**: Implement a key-value lookup to fetch a single product by ID.

**File to update**: `key_value_service_new_client.py` — `TODO: STEP 3: GET A PRODUCT BY ID`

**How to validate**: Open http://localhost:8080 and click a product. The product detail page should load.

---

## Step 4: Query for Products

**Goal**: Implement a secondary-index-style query using AEL.

**File to update**: `key_value_service_new_client.py` — `TODO: STEP 4: QUERY FOR PRODUCTS`

**How to validate**: The homepage should show product rows for Shoes, Bags, Wallets, Watches, and Headwear.

---

## Step 5: Execute the Advanced Search

**Goal**: Run a multi-filter search using an AEL expression.

**File to update**: `key_value_service_new_client.py` — `TODO: STEP 5: EXECUTE THE ADVANCED SEARCH`

**How to validate**: Use the search page with category/brand filters. Results should match your filters.

---

## Step 6: Get the Cart Object

**Goal**: Load a user's shopping cart from Aerospike.

**File to update**: `key_value_service_new_client.py` — `TODO: STEP 6: GET THE CART OBJECT`

**How to validate**: Add an item to the cart, then open the cart page. Items should persist across page reloads.

---

## Step 7: Update the Cart

**Goal**: Add items to the cart with generation-checked updates for concurrency safety.

**File to update**: `key_value_service_new_client.py` — `TODO: STEP 7: UPDATE THE CART`

**How to validate**:

1. Add a product to the cart.
2. Add the same product again — quantity should increase.
3. Open two browser tabs and add different products simultaneously — both should appear.

---

## Reference

- [guide-to-python-sdk.md](guide-to-python-sdk.md) — Python SDK patterns
- `key_value_service_new_client_answers.py` — complete solution (facilitators only)
- Run with answers: `AEROSPIKE_CLIENT_PROFILE=new-client-answers uvicorn aerospikeworkshop.main:app --port 8080`
