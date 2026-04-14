Phase 1 - New Java Client - Usability Test Script

This document outlines the script and success criteria for the usability test of the new Aerospike Java Client.

## Step 1: Connect to the DB and Create a Session

**Goal**: Here we need the tester to successfully connect to an existing DB that exists somewhere.

**Tester Prompt**: Update code to connect to DB

**File to update**: `KeyValueServiceNewClient.java`

```java
// =================================================================================
// STEP 1: CONNECT TO THE DATABASE AND CREATE A SESSION
// =================================================================================
// Define the cluster connection and assign it to the `aerospikeCluster` variable.
//
// Refer to the documentation for the `ClusterDefinition` class to see how to
// configure the connection. You will need to provide the hostname, port, and
// user credentials, which are all available in the `config` object.
//
// Once connected, create a session off the cluster using the default behavior.
// =================================================================================
aerospikeCluster = null;
session = null;
```

**Expected tester output**:

```java
aerospikeCluster = new ClusterDefinition(config.getHostname(), config.getPort())
      .withNativeCredentials(config.getUserName(), config.getPassword())
      .connect();
session = aerospikeCluster.createSession(Behavior.DEFAULT);
```

**Questions**:

*   How did you find the process of defining the cluster connection compared to previous experiences with database clients? Was it better or worse?
*   What is your understanding of `sessions` and `behavior`? How does it compare to how the current client works?

**Success criteria**:

*   Able to successfully connect to the DB.
*   Able to successfully create a session
*   Able to connect to DB in under 5 minutes.

## Step 2: Store a Product Object

**Goal**: The tester needs to implement the logic to store a single product object into the database, testing the object mapping functionality.

**Tester Prompt**: Update code to add product data to the DB.

**File to update**: `KeyValueServiceNewClient.java`

```java
// =================================================================================
// STEP 2: STORE A PRODUCT OBJECT
// =================================================================================
// Implement the logic to store a `Product` object in the database.
//
// This task tests the client's object mapping capabilities. Your goal is to:
//  - Use the `session` object to `insertInto` the `productDataSet`.
//  - Pass the `product` object to the operation.
//  - Specify the `productMapper` to handle the conversion.
//  - Execute the operation.
//  - Throw an exception if it's already there.
// =================================================================================
// <-- Your code goes here
```

**Expected tester output**:

```java
session.insertInto(productDataSet)
        .object(product)
        .using(productMapper)
        .execute();
```

**Questions**:

*   Walk me through your understanding of the different parts of that command: the DataSet, the object, the mapper, and the execute step. How does it compare with your current process for inserting data?
*   What are your thoughts on the chain of commands used to insert the object (`insertInto`, `object`, `using`, `execute`)?
*   What are your thoughts on the `TypeSafeDataSet` concept for representing your data's location?

**Success criteria**:

*   Data is successfully persisted to the database.
*   **Validation**: After you implement the code, you need to load product data into the database. Open a new terminal, navigate to the `data` directory, and run the following commands from your project root:

    ```bash
    cd data
    curl -X POST "http://localhost:8080/rest/v1/data/create-indexes"
    curl -X POST "http://localhost:8080/rest/v1/data/load?dataPath=`pwd`"
    ```

    Once the data is loaded, verify the records using the Aerospike Query Language tool (`aql`):

    ```bash
    aql -c "SELECT * FROM test.products LIMIT 5"
    ```

    You should see 5 product records returned.
*   Able to complete the task in under 5 minutes.

## Step 3: Get a Product by ID

**Goal**: The tester needs to implement a key-value lookup to fetch a single product by its unique ID.

**Tester Prompt**: Update the code to get the product detail page to work by querying for a product given its ID.

**File to update**: `KeyValueServiceNewClient.java`

```java
// =================================================================================
// STEP 3: GET A PRODUCT BY ID
// =================================================================================
// Implement the logic to fetch a single product by its `productId`.
//
// This is a key-value lookup. Your goal is to:
//  - Query the `productDataSet` using the `id()` method with the `productId`.
//  - Execute the query.
//  - Get the first record from the result set.
//  - Use the `productMapper` to convert the record into a `Product` object.
// =================================================================================
return Optional.of(product); // <-- Your code goes here
```

**Expected tester output**:

```java
return session.query(productDataSet.id(productId))
        .execute()
        .getFirst(productMapper);
```

**Questions**:

*   How did you approach finding the right method to look up a product by its ID?
*   Describe the process of chaining commands to get the final result.
*   What were your thoughts on using `.getFirst()` to retrieve a single object?

**Success criteria**:

*   Opening browser and navigating directly to `http://localhost:5173/product/41213` displays the details for the "CASIO" watch.
*   Able to complete the task in under 3 minutes.

## Step 4: Query for Products

**Goal**: The tester needs to implement a simple query to retrieve a list of products, testing the basic query API.

**Tester Prompt**: Update the code to populate the homepage with items from the DB.

**File to update**: `KeyValueServiceNewClient.java`

```java
// =================================================================================
// STEP 4: QUERY FOR PRODUCTS
// =================================================================================
// Implement the logic to query for a list of products.
//
// Refer to the documentation on how to build a query using the `session` object.
// Your goal is to:
//  - Query the `productDataSet`.
//  - Filter results using the `where` clause. The `index` and `filterValue` parameters
//    will be used to construct the filter expression. (For example: bin "category" equals "Footware".)
//  - Limit the results to `count`.
//  - Optimize the query to only read the "id", "name", "images", and "brandName" bins.
//  - Convert the final result into a `List<Product>` and assign it to the `products` variable.
// =================================================================================
List<Product> products = List.of(getProduct("41213").get()); // <-- Your code goes here
```

**Expected tester output**:

```java
List<Product> products = session.query(productDataSet)
        .where("$.%s == '%s'", index, filterValue)
        .readingOnlyBins("id", "name", "images", "brandName")
        .limit(count)
        .execute()
        .toObjectLlist(productMapper);
```

**Questions**:

*   Describe your experience using the Java SDK to build the query.
*   How did using a string-based DSL in the `.where()` clause feel compared to building query objects programmatically?
*   The new client automatically uses the best secondary index. What are your thoughts on not having to specify an index in your query code?

**Success criteria**:

*   The application's homepage successfully loads with products.
*   Able to complete the task in under 7 minutes.

## Step 5: Build the DSL String

**Goal**: The tester needs to build a DSL query string from multiple optional filters.

**Tester Prompt**: Update the code to build a DSL query for the advanced search.

**File to update**: `KeyValueServiceNewClient.java`

```java
// =================================================================================
// STEP 5: BUILD THE DSL STRING
// =================================================================================
// Form a a DSL string of the non-empty index filters passed. For example,
// if category == 'Footware' and brandName == 'Adidas' you want to form a string of:
// "$.category == 'Footware' and $.brandName == 'Adidas'"
// Your goal is to:
//  - Use the map above to form a `dsl` string with the non-empty clauses AND'ed
//    together.
//  - Assign the result to the `dsl` variable.
// =================================================================================
String dsl = ""; // <-- Your code goes here
```

**Expected tester output**:

```java
String dsl = "";
for (Map.Entry<String, String> thisEntry : indexes.entrySet()) {
    if (!thisEntry.getValue().isEmpty()) {
        if (!dsl.isEmpty()) {
            dsl += " and ";
        }
        dsl += String.format("$.%s == '%s'", thisEntry.getKey(), thisEntry.getValue());
    }
}
```

**Questions**:

*   Describe the process of building the DSL string.
*   What are your thoughts on the readability and simplicity of the final DSL string (e.g., `$.category == 'Footware'`)?
*   How did the provided example help you with the task?

**Success criteria**:

*   The DSL string is correctly formed.
*   **Validation**: After implementing the code, run a search from the application's UI using one of the filter dropdowns (e.g., Brand). Check the console output where your Spring Boot application is running. You should see the DSL string you constructed printed to the log, for example: `DSL: $.brandName == 'Aerospike'`.
*   Task completed in under 5 minutes.

## Step 6: Execute an Advanced Search

**Goal**: The tester needs to execute a query using a pre-built DSL string, demonstrating how to handle more complex, multi-filter queries.

**Tester Prompt**: Update the code to activate the dropdowns and perform a filtered search.

**File to update**: `KeyValueServiceNewClient.java`

```java
// =================================================================================
// STEP 6: EXECUTE THE ADVANCED SEARCH
// =================================================================================
// The DSL query string has been built by you. Now, execute the query.
//
// Your goal is to:
//  - Use the `session` object to query the `productDataSet`.
//  - Apply the pre-built `dsl` string using the `.where()` clause.
//  - Limit the results to `count`.
//  - Execute the query and convert the result to a list of `Product` objects
//    using the `productMapper`.
//  - Assign the result to the `products` variable.
// =================================================================================
List<Product> products = List.of(getProduct("41213").get()); // <-- Your code goes here
```

**Expected tester output**:

```java
List<Product> products = session.query(productDataSet)
        .where(dsl)
        .limit(count)
        .execute()
        .toObjectLlist(productMapper);
```

**Questions**:

*   How did this compare to your expectation of how the client would handle index optimization for this query?
*   Describe your experience of applying the DSL string to your query. Good or bad?

**Success criteria**:

*   The application's search page returns correctly filtered results.
*   Able to complete the task in under 5 minutes.

## Step 7: Get the Cart Object

**Goal**: The tester needs to implement the logic to retrieve a user's cart.

**Tester Prompt**: Update the code to get the user's shopping cart.

**File to update**: `KeyValueServiceNewClient.java`

```java
// =================================================================================
// STEP 7: GET THE CART OBJECT
// =================================================================================
// Implement the logic to query a `Cart` object in the database by key
//
// This task tests the client's object mapping capabilities. Your goal is to:
//  - Use the `session` object to `query` the `cartDataSet`.
//  - Pass the `userId` object to the operation.
//  - Execute the operation.
//  - Return the first one, or if there isn't one an empty cart.
// =================================================================================
// <-- Your code goes here
```

**Expected tester output**:

```java
return session.query(cartDataSet.id(userId))
    .execute()
    .getFirst(cartMapper)
    .orElseGet(() -> new Cart());
```

**Questions**:

*   How would you rate the experience of retrieving an object and handling the case where it might not exist?
*   What are your thoughts on how the `cartMapper` was used to convert the database record into a `Cart` object?
*   Describe your experience with using `orElseGet` to provide a default value.

**Success criteria**:

*   The shopping cart page loads correctly (either with items or empty).
*   Able to complete the task in under 5 minutes.

## Step 8: Update the Cart

**Goal**: The tester needs to implement logic to add an item to the shopping cart, handling creation and updates atomically.

**Tester Prompt**: Update the code to add items to the shopping cart, ensuring correct quantity updates and race condition handling.

**File to update**: `KeyValueServiceNewClient.java`

```java
// =================================================================================
// STEP 8: UPDATE THE CART
// =================================================================================
// Implement the logic to query a `Cart` object in the database by key
//
// Your goal is to:
// Step 8a:
//  - Retrieve the user's cart, using the `key`
//  - Return the first one (with it's metadata) and map it to a cart (with metadata)
// Step 8b:
//  - Increase the quantity associated with the passed product on this user's cart
//  - Make sure to check the generation has not changed since the record was read
// Step 8c:
//  - Add the item to the cart in the ITEMS_BIN at the key `productId`
//  - Make sure to check the generation has not changed since the record was read
// Step 8d:
//  - Insert the item to the cart in the ITEMS_BIN at the key `productId` in a new record
// =================================================================================
```

**Expected tester output**:

```java
// Step 8a
Optional<ObjectWithMetadata<Cart>> cartAndMetadata = session.query(key)
        .execute()
        .getFirstWithMetadata(cartMapper);

// Step 8b
session.update(key)
    .bin(ITEMS_BIN).onMapKey(productId).onMapKey("quantity").add(quantity)
    .ensureGenerationIs(cartWithMetadata.getGeneration())
    .execute();

// Step 8c
session.update(key)
    .bin(ITEMS_BIN).onMapKey(productId).setTo(newItem, cartItemMapper)
    .ensureGenerationIs(cartWithMetadata.getGeneration())
    .execute();

// Step 8d
session.insertInto(key)
    .bin(ITEMS_BIN).onMapKey(productId).setTo(newItem, cartItemMapper)
    .execute();
```

**Questions**:

*   How intuitive were CDT map operations (onMapKey, add, setTo) with this new methodology?
*   Describe your understanding of how `getFirstWithMetadata` and `ensureGenerationIs` work together.
*   What are your thoughts on the Java SDK used for the map operations (e.g., `onMapKey`, `add`, `setTo`)?
*   How did you find the process of handling the different cases (item exists, item doesn't exist, cart doesn't exist)?

**Success criteria**:

*   Testers can successfully add items to the cart, and quantities are updated correctly even with simulated concurrent requests.
*   Able to complete the task in under 15 minutes.

## Final Feedback

At the end of the session, please ask the tester the following questions to gather their overall impressions:

1.  Rate your satisfaction with the new API from 1 (terrible) to 5 (amazing).
2.  What was the best or most intuitive part of using this new client API?
3.  What was the most confusing or difficult part of the new client API?
4.  Overall, how would you compare this new Java SDK to other database clients you have used in the past?
5.  Do you have any final thoughts or suggestions for how we could improve the client?
