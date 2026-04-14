# New Java Client - Usability Test Prompts

This document will guide you through the usability test for the new Aerospike Java Client. Please follow the steps below. At each step, you will be given a task to complete. Please try to complete the tasks on your own. If you get stuck, feel free to ask for help.

---

## Step 1: Connect to the DB and Create a Session

**Goal**: Your task is to update the code to connect to an existing database.

**Prompt**: Update the code to connect to the DB.

**File to update**: `spring-server/src/main/java/com/aerospike/service/KeyValueServiceNewClient.java`

In the file, find the comment block for `STEP 1: CONNECT TO THE DATABASE AND CREATE A SESSION`. You'll need to replace the `null` assignments for `aerospikeCluster` and `session` with your implementation.

**How to validate**:
*   The application starts up without connection errors. Run the Spring Boot application and check the console logs for any connection-related exceptions: 
```shell
cd spring-server
mvn clean package -DskipTests
```
*   Upon successfully building the application, run the application using:
```shell
java -jar target/retail-demo-spring-1.0.0.jar --aerospike.port=3100 ..."new-client"
```

---

## Step 2: Store a Product Object

**Goal**: Implement the logic to store a single product object into the database. This will test the object mapping functionality.

**Tester Prompt**: Update the code to add product data to the DB.

**File to update**: `spring-server/src/main/java/com/aerospike/service/KeyValueServiceNewClient.java`

In the file, find the comment block for `STEP 2: STORE A PRODUCT OBJECT` and add your implementation where indicated.

**How to validate**:
*   After you implement the code, you need to load product data into the database. Open a new terminal, and run the following commands from your project root (`aerospike-client-sdk-workshop`):
    ```bash
    cd data
    curl -X POST "http://localhost:8080/rest/v1/data/create-indexes"
    curl -X POST "http://localhost:8080/rest/v1/data/load?dataPath=`pwd`"
    cd ..
    ```
*   Once the data is loaded, verify the records using the Aerospike Query Language tool (`aql`). 
    ```bash
    aql -c "SELECT * FROM test.products LIMIT 5"
    ```
    You should see 5 product records returned.

---

## Step 3: Get a Product by ID

**Goal**: Implement a key-value lookup to fetch a single product by its unique ID.

**Tester Prompt**: Update the code to get the product detail page to work by querying for a product given its ID.

**File to update**: `spring-server/src/main/java/com/aerospike/service/KeyValueServiceNewClient.java`

In the file, find the comment block for `STEP 3: GET A PRODUCT BY ID` and replace the existing `return` statement with your implementation.

**How to validate**:
*   Open your browser and navigate to `http://localhost:5173/product/41213`. The page should display the details for the "CASIO" watch.

---

## Step 4: Query for Products

**Goal**: Implement a simple query to retrieve a list of products, testing the basic query API.

**Tester Prompt**: Update the code to populate the homepage with items from the DB.

**File to update**: `spring-server/src/main/java/com/aerospike/service/KeyValueServiceNewClient.java`

In the file, find the comment block for `STEP 4: QUERY FOR PRODUCTS` and replace the line initializing `products` with your implementation.

**How to validate**:
*   The application's homepage should successfully load and display products from the database.

---

## Step 5: Build the DSL String

**Goal**: Build a DSL query string from multiple optional filters.

**Tester Prompt**: Update the code to build a DSL query for the advanced search.

**File to update**: `spring-server/src/main/java/com/aerospike/service/KeyValueServiceNewClient.java`

In the file, find the comment block for `STEP 5: BUILD THE DSL STRING` and replace the line initializing `dsl` with your implementation.

**How to validate**:
*   Run a search from the application's UI using one of the filter dropdowns (e.g., Brand).
*   Check the console output where your Spring Boot application is running. You should see the DSL string you constructed printed to the log, for example: `DSL: $.brandName == 'Nike'`.

---

## Step 6: Execute an Advanced Search

**Goal**: Execute a query using a pre-built DSL string, demonstrating how to handle more complex, multi-filter queries.

**Tester Prompt**: Update the code to activate the dropdowns and perform a filtered search.

**File to update**: `spring-server/src/main/java/com/aerospike/service/KeyValueServiceNewClient.java`

In the file, find the comment block for `STEP 6: EXECUTE THE ADVANCED SEARCH` and replace the line initializing `products` with your implementation.

**How to validate**:
*   The application's search page should return correctly filtered results when you use the dropdown filters.

---

## Step 7: Get the Cart Object

**Goal**: Implement the logic to retrieve a user's cart.

**Tester Prompt**: Update the code to get the user's shopping cart.

**File to update**: `spring-server/src/main/java/com/aerospike/service/KeyValueServiceNewClient.java`

In the file, find the comment block for `STEP 7: GET THE CART OBJECT` and add your implementation where indicated.

**How to validate**:
*   Navigate to the shopping cart page in the application. It should load correctly, either showing items if you've added any, or an empty cart.

---

## Step 8: Update the Cart

**Goal**: Implement logic to add an item to the shopping cart, handling creation and updates atomically.

**Tester Prompt**: Update the code to add items to the shopping cart, ensuring correct quantity updates and race condition handling.

**File to update**: `spring-server/src/main/java/com/aerospike/service/KeyValueServiceNewClient.java`

In the file, find the comment block for `STEP 8: UPDATE THE CART`. You will need to implement the logic for all sub-steps (8a, 8b, 8c, 8d) in the `addCartItem` method.

**How to validate**:
*   You should be able to add items to the cart from the product pages.
*   The quantities in the cart should update correctly if you add the same item multiple times.
*   After adding an item to your cart, you can also verify the data is being saved to the database. From a terminal, run the following `aql` command to see all the carts in the database (there should only be one for your user):
    ```bash
    aql -c "SELECT * FROM test.shopping_carts"
    ```

---

## Final Feedback

Once you have completed all the steps, please provide your feedback on the following questions:

1.  Rate your satisfaction with the new API from 1 (terrible) to 5 (amazing).
2.  What was the best or most intuitive part of using this new client API?
3.  What was the most confusing or difficult part of the new client API?
4.  Overall, how would you compare this new Java SDK to other database clients you have used in the past?
5.  Do you have any final thoughts or suggestions for how we could improve the client?

Thank you for your participation!
