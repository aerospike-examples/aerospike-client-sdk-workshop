---
---
# Quick guide to using the Aerospike Fluent API
## Connecting to a cluster

Connecting to a cluster involves creating a `ClusterDefinition` which controls everything needed to talk to an Aerospike database, such as user credentials and TLS options.
```java
Cluster cluster = new ClusterDefinition("localhost", 3100)
                .withNativeCredentials("tim", "myPassword!")
                .withLogLevel(Level.DEBUG)
                .connect();
```

Don’t forget to close the cluster when done, or use a resource try block:

```java
try (Cluster cluster = new ClusterDefinition("localhost", 3100).connect()) {
    // Do work, automatically close the cluster when done.
}
```

## Behaviors
Behaviors control how data operations behave. They encapsulate all network settings, timeouts, etc. Reasonable defaults can be found `Behavior.DEFAULT`.

### Deriving a new behaviour
Use cases typically need custom settings, for example a shorter timeout on a read with a retry against the replica. Many use cases also require different settings for different calls. So new `Behavior`s can be derived from existing `Behavior`s.

```java
Behavior newBehavior = Behavior.DEFAULT.deriveWithChanges("newBehavior", builder -> 
    builder.on(Selectors.all(), ops -> ops
            .waitForSocketResponseAfterCallFails(Duration.ofSeconds(3))
    )
    .on(Selectors.reads().ap(), ops -> ops
            .waitForCallToComplete(Duration.ofMillis(25))
            .abandonCallAfter(Duration.ofMillis(100))
            .maximumNumberOfCallAttempts(3)
    )
    .on(Selectors.reads().query(), ops -> ops
            .waitForCallToComplete(Duration.ofSeconds(2))
            .abandonCallAfter(Duration.ofSeconds(30))
    )
    .on(Selectors.reads().batch(), ops -> ops
            .maximumNumberOfCallAttempts(7)
            .allowInlineMemoryAccess(true)
    )
);
```
Once a `Behavior` has been created, it is effectively immutable.

## Creating a DataSet
A `DataSet` contains a namespace and a set. Almost all data operations require both of these parameters. `DataSet`s can be used in some data operations in their own right, or used to create `Key`(s).
```java
DataSet customerDataSet = DataSet.of("test", "customer");
```

## Creating a session
Sessions are the primary means for accessing data in the database. A `Session` is created from applying a `Behavior` to a `Cluster`.
```java
Session session = cluster.createSession(newBehavior);
```

## Upserting a single record
```java
session.upsert(customerDataSet.id(80))
        .bin("name").setTo("Tim")
        .bin("age").setTo(342)
        .execute();
```
# Insert a record in one shot
```java
session.insertInto(customerDataSet.id(81))
        .bins("name", "age")
        .values("Sam", 28)
        .execute();
```
## Update multiple records
```java
session.update(customerDataSet.ids(84, 85))
        .bins("name", "age")
        .values("Tim", 342)
        .values("Fred", 37)
        .execute();
```
## Inserting objects
```java
session.insertInto(customerDataSet)
    .objects(customerList)
    .using(customerMapper)
    .execute();
```
## Bulk update of records (add 1 to the quantity bin of 4 records)
```java
session.update(productsDataSet.ids(102, 103, 104, 105))
    .bin("quantity").add(1)
    .execute();
```
## Updating CDTs
```java
RecordStream results = session.upsert(hotelDataSet.id(102)) 
    .bin("rooms").onMapKeyRange("room1", "room2").count()
    .bin("rooms").onMapKey("room1").getValues()
    .bin("rooms").onMapKey("room1").onMapKey("rates").setTo(110)
    .execute();
```
## Delete records
```java
session.delete(customerDataSet.ids(900, 901, 902, 903)).execute();
```
## Transactions
```java
session.doInTransaction(txnSession -> {
    Optional<KeyRecord> result = 
        txnSession.query(customerDataSet.id(1)).execute().getFirst();
    if (shouldInsert) {
        txnSession.insertInto(customerDataSet.id(3));
    }
    txnSession.delete(customerDataSet.id(4));
    txnSession.insertInto(customerDataSet.id(3)).notInAnyTransaction().execute();
});
```
## Querying data
### Querying a single record
```java
RecordStream recStream = session.query(customerDataSet.id(20)).execute();
```
```java
recStream = session.query(customerDataSet.id(20))
    .readingOnlyBins("name", "age")
    .execute();
```
```java
Optional<Customer> custOptional = session.query(customerDataSet.id(20))
    .execute()
    .getFirst(customerMapper);
```
#### Querying with metadata (generation number)
When you need record metadata (e.g. the generation number for optimistic locking), use `getFirstWithMetadata()`. The returned `ObjectWithMetadata<T>` wraps the mapped object and provides `.getGeneration()` for use with `ensureGenerationIs()`.
```java
Optional<ObjectWithMetadata<Customer>> custWithMeta = session.query(customerDataSet.id(20))
    .execute()
    .getFirstWithMetadata(customerMapper);

custWithMeta.ifPresent(cwm -> {
    Customer cust = cwm.get();
    int generation = cwm.getGeneration();

    // Use generation for optimistic locking on update
    session.update(customerDataSet.id(20))
        .bin("visits").add(1)
        .ensureGenerationIs(generation)
        .execute();
});
```

### Querying multiple records
#### Query with ids known
If the ids of multiple records are known, the list of ids can be passed to the `query` call:
```java
print(session.query(customerDataSet.ids(6,7,8))
    .readingOnlyBins("name", "age")
    .execute());
```
#### Query every record in the set
This call will effective scan the set, returning all recrods.
```java
RecordStream recordStream = session.query(customerDataSet).execute();
// Don’t forget to close the stream when done!

List<Customer> customers = session.query(customerDataSet)
    .execute()
    .toObjectList(customerMapper);

int total = session.query(productDataSet)
    .execute()
    .stream()     // Turn into Java stream
    .mapToInt(kr -> kr.record.getInt("quantity"))
    .sum();
```
#### Query selected records
To filter which records are returned, a `where()` clause can be specified. Note that no secondary index has been identified in the code -- Aerospike will work out which, if any, secondary index to use and then use this possibly in conjunction with a filter expression to return only the desired records.
```java
try (RecordStream rs = session.query(customerDataSet)
        .where("$.name == 'Tim' and $.age > 30")
        .limit(100)		// maximum of 100 records ever returned
        .pageSize(20)		// Results are pageable
        .execute()) {...}
```

#### DSL Quick Reference
The `.where()` clause accepts a string-based DSL (Domain-Specific Language) for filtering records. Here's a quick reference:

| Syntax | Example | Description |
|--------|---------|-------------|
| `$.binName` | `$.age` | Reference a bin (field) value |
| `== !=` | `$.status == 'active'` | Equality / inequality |
| `> >= < <=` | `$.age > 21` | Numeric comparisons |
| `and` | `$.age > 21 and $.status == 'active'` | Logical AND (combine filters) |
| `or` | `$.region == 'US' or $.region == 'EU'` | Logical OR |
| String values | `$.name == 'Tim'` | Use single quotes for strings |
| Numeric values | `$.count > 50` | No quotes for numbers |
| Format strings | `"$.%s == '%s'"` | Build dynamic DSL with `String.format` |

**Examples:**
```java
// Single filter
.where("$.category == 'Footwear'")

// Multiple filters
.where("$.category == 'Footwear' and $.brandName == 'Adidas'")

// Numeric comparison
.where("$.price > 500 and $.price < 1000")

// Dynamic construction
String dsl = String.format("$.%s == '%s'", "category", "Footwear");
.where(dsl)
```
### Sorting and pagination
Aerospike does not have inherent capabilities to sort data or paginate it by arbitrary fields, but this capability exists on the client. However, it is important to note that *all records will be downloaded to the client.* Hence this capability will consume memory on the client, and should only be used when a `limit` has been set on the query to alllow only a reasonably small set of records be returned.
```java
NavigatableRecordStream ns = session.query(customerDataSet)
        .where("$.name == 'Tim' and $.age > 30")
        .limit(1000)
        .execute()
        .asNavigatableStream();

List<Customer> customers = ns
    .sortBy("name", SortDir.SORT_ASC, true)
    .toObjectList(customerMapper);
```
Once a `NavigatableStream` is in memory, the stream can be manipulated many times without going back to the database. For example, to sort first by age, then name, and paginate the records in pages of 5 records, you can do:
```java
ns.sortBy(List.of(
    SortProperties.descending("age"),
    SortProperties.ascendingIgnoreCase("name")
))
.pageSize(5);
                
int page = 0;
while (ns.hasMorePages()) {
    System.out.println("---- Page " + (++page) + " -----");
    customers = ns.toObjectList(customerMapper);
    customers.forEach(cust -> System.out.println(cust));
}
```
