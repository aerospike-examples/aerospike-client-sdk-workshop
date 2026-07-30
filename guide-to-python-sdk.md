# Quick guide to using the Aerospike Python SDK

This guide parallels [guide-to-java-sdk.md](guide-to-java-sdk.md) for the Python workshop.

## Connecting to a cluster

```python
from aerospike_sdk import Client, ClusterDefinition, Behavior, DataSet

# Async context manager (recommended for FastAPI)
async with Client("localhost:3000") as client:
    session = client.create_session(Behavior.DEFAULT)
    # ... use session

# Or explicit connect/close
cluster_def = ClusterDefinition("localhost", 3000)
if username:
    cluster_def = cluster_def.with_native_credentials(username, password)
cluster = await cluster_def.connect()
session = cluster.create_session(Behavior.DEFAULT)
# ...
await cluster.close()
```

## DataSet and keys

```python
products = DataSet.of("test", "products")
key = products.id("13283")
```

## Upsert / insert a record

```python
await (
    session.upsert(products.id("13283"))
    .bin("name").set_to("Running Shoes")
    .bin("price").set_to(99)
    .execute()
)

# Dict shorthand (common in this workshop)
await session.insert(products.id("13283")).put({
    "id": "13283",
    "name": "Running Shoes",
    "price": 99,
}).execute()
```

## Point read

```python
stream = await session.query(products.id("13283")).execute()
result = await stream.first()
stream.close()
if result and result.is_ok and result.record:
    bins = result.record.bins
```

## Query with AEL filter

```python
stream = await (
    session.query(products)
    .where("$.subCategory == 'Shoes'")
    .bins(["id", "name", "images", "brandName", "price"])
    .limit(10)
    .execute()
)
products_list = []
async for row in stream:
    if row.is_ok and row.record:
        products_list.append(row.record.bins)
stream.close()
```

### AEL quick reference

| Syntax | Example |
|--------|---------|
| `$.binName` | `$.category` |
| `==` | `$.category == 'Footwear'` |
| `and` | `$.category == 'Footwear' and $.brandName == 'Adidas'` |
| String values | Use single quotes: `'Shoes'` |

Build dynamic filters:

```python
ael = f"$.{index} == '{filter_value}'"
stream = await session.query(products).where(ael).limit(count).execute()
```

## Secondary indexes

```python
await (
    session.index(dataset=products)
    .on_bin("subCategory")
    .named("subCat_idx")
    .string()
    .create()
)
```

## CDT map operations (shopping cart)

Cart items are stored in an `items` map bin keyed by product ID:

```python
ITEMS_BIN = "items"
cart_key = carts.id(user_id)

# Add item to map
await (
    session.update(cart_key)
    .bin(ITEMS_BIN).on_map_key(product_id).set_to(item_dict)
    .ensure_generation_is(generation)
    .execute()
)

# Increment quantity with optimistic locking
await (
    session.update(cart_key)
    .bin(ITEMS_BIN).on_map_key(product_id).on_map_key("quantity").add(1)
    .ensure_generation_is(generation)
    .execute()
)

# Clear cart
await session.upsert(cart_key).bin(ITEMS_BIN).map_clear().execute()
```

## Generation / optimistic locking

Read generation from query result, then pass to updates:

```python
stream = await session.query(cart_key).execute()
result = await stream.first()
stream.close()
generation = result.record.generation

try:
    await (
        session.update(cart_key)
        .bin(ITEMS_BIN).on_map_key(product_id).on_map_key("quantity").add(1)
        .ensure_generation_is(generation)
        .execute()
    )
except GenerationError:
    # Retry — another request modified the cart
    ...
```

## Category metadata

```python
from aerospike_async import MapOrder

await (
    session.upsert(category_dataset.id("product_meta"))
    .bin("categories").on_map_key(category, create_type=MapOrder.KEY_ORDERED).on_map_key(sub_category).add(1)
    .bin("articleTypes").list_append(article_type, unique=True, no_fail=True)
    .bin("usage").list_append(usage, unique=True, no_fail=True)
    .bin("brandNames").list_append(brand_name, unique=True, no_fail=True)
    .execute()
)
```

## Truncate / clear data

```python
await session.truncate(products)
await session.truncate(carts)
```

## Key difference from Java

The Python SDK uses **dict-based** reads and writes (`put({...})`, `record.bins`) rather than Java's `TypedDataSet` and `RecordMapper`. In the workshop, use `Product.from_bins()` and `product.to_bins()` helpers in the models package.

## Workshop file

Edit `python-server/src/aerospikeworkshop/services/key_value_service_new_client.py` and complete the TODO steps. Compare with `key_value_service_new_client_answers.py` for the solution.

## Further reading

- [Python SDK documentation](https://aerospike-python-sdk.readthedocs.io/)
- [aerospike-client-python-sdk examples](https://github.com/aerospike/aerospike-client-python-sdk/tree/main/examples)
