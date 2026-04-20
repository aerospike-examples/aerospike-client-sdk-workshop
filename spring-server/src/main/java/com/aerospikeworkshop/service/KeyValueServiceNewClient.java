package com.aerospikeworkshop.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.aerospike.client.sdk.AerospikeException;
import com.aerospike.client.sdk.AerospikeException.GenerationException;
import com.aerospike.client.sdk.Cluster;
import com.aerospike.client.sdk.ClusterDefinition;
import com.aerospike.client.sdk.DataSet;
import com.aerospike.client.sdk.Key;
import com.aerospike.client.sdk.Record;
import com.aerospike.client.sdk.Log;
import com.aerospike.client.sdk.RecordMapper;
import com.aerospike.client.sdk.RecordResult;
import com.aerospike.client.sdk.RecordStream.ObjectWithMetadata;
import com.aerospike.client.sdk.Session;
import com.aerospike.client.sdk.policy.Behavior;
import com.aerospike.client.sdk.TypeSafeDataSet;
import com.aerospike.client.sdk.cdt.MapOrder;
import com.aerospike.client.sdk.query.IndexCollectionType;
import com.aerospike.client.sdk.query.IndexType;
import com.aerospikeworkshop.config.ClientConfiguration;
import com.aerospikeworkshop.model.Cart;
import com.aerospikeworkshop.model.CartItem;
import com.aerospikeworkshop.model.Product;

import jakarta.annotation.PreDestroy;

/**
 * New Client implementation of KeyValue operations
 * Uses enhanced Aerospike client configuration with optimizations
 * Active when 'new-client' profile is enabled
 */
@Service
@Profile("new-client")
public class KeyValueServiceNewClient implements KeyValueServiceInterface {

    private static final String ITEMS_BIN = "items";
    private static final String NAMESPACE = "test";
    private static final String PRODUCT_SET = "products";
    private static final String CARTS_SET = "shopping_carts";
    private static final String CATEGORY_SET = "cat_index";
    private static final String CATEGORY_KEY = "product_meta";

    private final Cluster aerospikeCluster;
    
    private final TypeSafeDataSet<Product> productDataSet = TypeSafeDataSet.of(NAMESPACE, PRODUCT_SET, Product.class);
    private final TypeSafeDataSet<CartItem> cartDataSet = TypeSafeDataSet.of(NAMESPACE, CARTS_SET, CartItem.class);
    private final DataSet categoryDataSet = DataSet.of(NAMESPACE, CATEGORY_SET);
    private final Session session;
    private final ProductMapper productMapper = new ProductMapper();
    private final CartItemMapper cartItemMapper = new CartItemMapper();
    private final CartMapper cartMapper = new CartMapper();
    
    public KeyValueServiceNewClient(ClientConfiguration config) {
        // =================================================================================
        // TODO: STEP 1: VALIDATE THE CONNECTION
        // =================================================================================
        ClusterDefinition clusterDef = new ClusterDefinition(config.getHostname(), config.getPort());
        if (config.getUserName() != null && config.getPassword() != null) {
            clusterDef = clusterDef.withNativeCredentials(config.getUserName(), config.getPassword());
        }
        aerospikeCluster = clusterDef.connect();

        session = aerospikeCluster.createSession(Behavior.DEFAULT);
    }

    /**
     * Store a product record in Aerospike
     * 
     * @param product Product data map
     * @param productId Product identifier
     */
    public void storeProduct(Product product) {
        // =================================================================================
        // TODO: STEP 2: STORE A PRODUCT OBJECT
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
    }

    /**
     * Key-Value lookup of a specified product
     * Gets the product record and returns the record bins
     * 
     * @param productId Product identifier
     * @return Map containing the product data
     */
    public Optional<Product> getProduct(String productId) {
        // =================================================================================
        // TODO: STEP 3: GET A PRODUCT BY ID
        // =================================================================================
        // Implement the logic to fetch a single product by its `productId`.
        //
        // This is a key-value lookup. Your goal is to:
        //  - Query the `productDataSet` using the `id()` method with the `productId`.
        //  - Execute the query.
        //  - Get the first record from the result set.
        //  - Use the `productMapper` to convert the record into a `Product` object.
        // =================================================================================
        return Optional.empty();
    }

    /**
     * Secondary index query on a specified index and filter
     * Gets the first N records of the secondary index query
     * Returns a list of dictionaries, each containing a record's bins, and the query execution time
     * 
     * @param index Index name to query
     * @param filterValue Filter value to match
     * @param count Maximum number of records to return
     * @return QueryResult containing products list and execution time
     */
    public KeyValueServiceInterface.QueryResult query(String index, String filterValue, int count) {
        long startTime = System.currentTimeMillis();

        // =================================================================================
        // TODO: STEP 4: QUERY FOR PRODUCTS
        // =================================================================================
        // Implement the logic to query for a list of products.
        //
        // Refer to the documentation on how to build a query using the `session` object.
        // Your goal is to:
        //  - Query the `productDataSet`.
        //  - Filter results using the `where` clause. The `index` and `filterValue` parameters
        //    will be used to construct the filter expression. (For example: bin "category" equals "Footware".)
        //  - Limit the results to `count`.
        //  - Optimize the query to only read the "id", "name", "images", "brandName", and "price" bins.
        //  - Convert the final result into a `List<Product>` and assign it to the `products` variable.
        // =================================================================================
        List<Product> products = List.of(getProduct("41213").get());
        
        return new KeyValueServiceInterface.QueryResult(products, System.currentTimeMillis() - startTime);
    }


    /**
     * Advanced search with multiple filters
     * 
     * @param category Category filter (optional)
     * @param articleType Article type filter (optional)
     * @param usage Usage filter (optional)
     * @param brandName Brand name filter (optional)
     * @param searchText Text search query (optional)
     * @param count Maximum number of results
     * @return QueryResult containing filtered products
     */
    public KeyValueServiceInterface.QueryResult advancedSearch(String category, String articleType, String usage, String brandName, String searchText, int count) {
        long startTime = System.currentTimeMillis();

        Map<String, String> indexes = Map.of(
                "category", asNonNullString(category), 
                "articleType", asNonNullString(articleType), 
                "usage", asNonNullString(usage), 
                "brandName", asNonNullString(brandName));

        // Form a a DSL string of the non-empty index filters passed. For example,
        // if category == 'Footware' and brandName == 'Adidas' this will form
        // "$.category == 'Footware' and $.brandName == 'Adidas'"
        String ael = "";
        for (Entry<String, String> thisEntry : indexes.entrySet()) {
            if (!thisEntry.getValue().isEmpty()) {
                if (!ael.isEmpty()) {
                    ael += " and ";
                }
                ael += String.format("$.%s == '%s'", thisEntry.getKey(), thisEntry.getValue());
            }
        }
        System.out.println("AEL: " + ael);

        // =================================================================================
        // TODO: STEP 5: EXECUTE THE ADVANCED SEARCH
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
        List<Product> products = List.of(getProduct("13283").get());

        long endTime = System.currentTimeMillis() - startTime;
        return new KeyValueServiceInterface.QueryResult(products, endTime);
    }

    /**
     * Get the user's shopping cart. If the cart does not exist in the database, return 
     * a new cart.
     * @param userId - THe user whose cart is to be returned.
     * @return the Cart of the user. Guaranteed to be non-null
     */
    public Cart getCart(String userId) {
        try {
            // =================================================================================
            // TODO: STEP 6: GET THE CART OBJECT
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
            return new Cart(Map.of("13283", new CartItem(userId, 2, 
                    "http://assets.myntassets.com/v1/images/" +
                    "style/properties/3c03eb9654656c19f467f85b7de928f5_images.jpg",
                    getProduct("13283").get())));
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error getting cart: " + e.getMessage());
            return new Cart();
        }
    }

    /** 
     * For the given userId and the given productId, if the product is already in the user's cart then
     * increase the quantity by `quantity`. If it's not in the cart, then insert it into the cart with
     * `quantity` as the quantity. If the record doesn't exist, then do this, creating the record if needed.
     * <p/>
     * Since this method can potentially be called by multiple threads at once, make sure that the cart
     * doesn't change by external factors during this method!
     * <P/>
     * A cart will have a structure like:
     * <pre>
     * {
     *   "items": {
     *     "15943": {
     *       "brandName": "Turtle",
     *       "image": "http://assets.myntassets.com/h_161,q_95,w_125/v1/images/style/properties/403dce654df223f46677767988641d7a_images.jpg",
     *       "name": "Turtle Men Leather Black Wallets",
     *       "price": 995,
     *       "productId": "15943",
     *       "quantity": 1,
     *       "userId": "user_uv4ytwx6h"
     *     },
     *     "41213": {
     *       "brandName": "Lotto",
     *       "image": "http://assets.myntassets.com/h_161,q_95,w_125/v1/images/style/properties/4e98e52e6516a9f93ee70287eece69ac_images.jpg",
     *       "name": "Lotto Men Black Flip Flops",
     *       "price": 219,
     *       "productId": "41213",
     *       "quantity": 4,
     *       "userId": "user_uv4ytwx6h"
     *     }
     *   }
     * }
     * </pre>
     */
    public Cart addToCart(String userId, String productId, int quantity) {
        try {
            // Get product details first
            Optional<Product> productOptional = getProduct(productId);
            
            Product product = productOptional
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            Key key = cartDataSet.id(userId);
            
            // Get product image
            String image = extractProductImage(product);
            Cart resultCart = null;
            while (resultCart == null) {
                try {
                    // =================================================================================
                    // TODO: STEP 7: UPDATE THE CART
                    // =================================================================================
                    // Implement the logic to query a `Cart` object in the database by key
                    //
                    // Your goal is to:
                    // Step 7a:
                    //  - Retrieve the user's cart, using the `key`
                    //  - Return the first one (with it's metadata) and map it to a cart (with metadata)
                    // Step 7b:
                    //  - Increase the quantity associated with the passed product on this user's cart
                    //  - Make sure to check the generation has not changed since the record was read
                    // Step 7c:
                    //  - Insert the item to the cart in the ITEMS_BIN at the key `productId` in a new record
                    // =================================================================================

                    // TODO: STEP 7a: Fetch the user's cart and use `getFirstWithMetadata` to return an
                    // Optional with the cart and record metadata details
                    Optional<ObjectWithMetadata<Cart>>cartAndMetadata = 
                            Optional.of(new ObjectWithMetadata<Cart>(getCart(userId), new Record(null, 1, 1)));
                    
                    resultCart = cartAndMetadata
                        .map(cartWithMetadata -> {
                            Cart cart = cartWithMetadata.get();
                            cart.findItem(productId)
                                .ifPresentOrElse(item -> {
                                    
                                    // The item exists in the record, just update the quantity
                                    item.setQuantity(item.getQuantity() + quantity);
                                    
                                    // TODO: STEP 7b: On the record identified by `key`, there is a bin called `ITEMS_BIN`
                                    // which is a map of productId -> cart items as a map. Find the item with the
                                    // passed `productId` and add `quantity` to it's  "quantity" key. Make sure to 
                                    // check that the record has the same generation as when it was read!
                                },
                                () -> {
                                    // Record exists but item is not there, just add it
                                    CartItem newItem = new CartItem(userId, quantity, image, product);
                                    cart.add(newItem);
                                    
                                    session.update(key)
                                        .bin(ITEMS_BIN).onMapKey(productId).setTo(newItem, cartItemMapper)
                                        .ensureGenerationIs(cartWithMetadata.getGeneration())
                                        .execute();
                                });
                            return cart;
                        })
                        .orElseGet(() -> {
                            // New record
                            Cart cart = new Cart();
                            CartItem newItem = new CartItem(userId, quantity, image, product);
                            cart.add(newItem);
                            // TODO: STEP 7c: The record doesn't exist. Create the record and insert the `newItem` into 
                            // the map with productId as it's key
                            return cart;
                        });
                }
                catch (GenerationException ge) {
                    Log.info("Lost race condition when adding product " + productId);
                    // Continue to retry 
                }
            }
            return resultCart;
        }  catch (Exception e) {
            System.err.println("Error adding to cart: " + e.getMessage());
            throw new RuntimeException("Failed to add item to cart: " + e.getMessage());
        }
    }

    /**
     * Remove all items from the cart of the passed userid
     * @param userId - the user whose cart is to be cleared
     * @return the updated (empty) cart
     */
    public Cart clearCart(String userId) {
        try {
            Key key = cartDataSet.id(userId);
            session.upsert(key).bin(ITEMS_BIN).mapClear().execute();
            return new Cart();
        } catch (Exception e) {
            System.err.println("Error clearing cart: " + e.getMessage());
            throw new RuntimeException("Failed to clear cart: " + e.getMessage());
        }
    }
    

    public Cart updateCartItem(String userId, String productId, int quantity) {
        try {
            Key key = cartDataSet.id(userId);
            return session.query(key)
                    .execute()
                    .getFirst(cartMapper)
                    .map(cart -> {
                        if (quantity <= 0) {
                            session.update(key)
                                .bin(ITEMS_BIN).onMapKey(productId).remove()
                                .execute();
                            cart.remove(productId);
                            return cart;
                        }
                        else {
                            cart.findItem(productId)
                            .ifPresent(item -> {
                                item.setQuantity(quantity);
                                session.update(key)
                                    .bin(ITEMS_BIN).onMapKey(productId).onMapKey("quantity").setTo(quantity)
                                    .execute();
                            });
                            return cart;

                        }
                    })
                    .orElseGet(() -> {
                        return new Cart();
                    });
        } catch (Exception e) {
            System.err.println("Error updating cart item: " + e.getMessage());
            throw new RuntimeException("Failed to update cart item: " + e.getMessage());
        }
    }

    public Cart removeFromCart(String userId, String productId) {
        return updateCartItem(userId, productId, 0);
    }

    /**
     * Get a count of all the products in the database
     * @return The number of products stored. 
     */
    public int getProductCount() {
        int replicationFactor = session.info().namespaceDetails(NAMESPACE)
                .map(nsDetails -> nsDetails.getEffectiveReplicationFactor())
                .orElseGet(() -> 1);
        return session.info()
                .set(PRODUCT_SET)
                .map(set->(int)set.getObjects() / replicationFactor)
                .orElseGet(() -> 0);
    }

    public void clearAllData() {
        session.truncate(cartDataSet);
        session.truncate(productDataSet);
        session.truncate(categoryDataSet);
    }

    /**
     * Get all categories from the category metadata record
     * 
     * @return List of category names
     */
    @SuppressWarnings("unchecked")
    public List<String> getCategories() {
        Optional<RecordResult> result = session.upsert(categoryDataSet.id(CATEGORY_KEY))
                .bin("categories").onMapKeyRange("A", "Z").getKeys()
                .execute()
                .getFirst();
        return result.map(kr -> (List<String>)kr.recordOrThrow().getList("categories"))
                .orElseGet(List::of);
    }

    @SuppressWarnings("unchecked")
    private List<String> getCategoryPart(String bin) {
        return session.query(categoryDataSet.id(CATEGORY_KEY))
                .readingOnlyBins(bin)
                .execute()
                .getFirst()
                .map(kr -> ((List<String>)kr.recordOrThrow().getList(bin))
                        .stream()
                        .filter(cat -> !cat.isEmpty() && !cat.equals("NA"))
                        .toList()
                )
                .orElseGet(List::of);
    }
    /**
     * Get all article types from the category metadata record
     * 
     * @return List of article type names
     */
    public List<String> getArticleTypes() {
        return getCategoryPart("articleTypes");
    }
    
    /**
     * Get all usage types from the category metadata record
     * 
     * @return List of usage names
     */
    public List<String> getUsage() {
        return getCategoryPart("usage");
    }

    /**
     * Get all brand names from the category metadata record
     * 
     * @return List of brand names
     */
    public List<String> getBrandNames() {
        return getCategoryPart("brandNames");
    }

    /**
     * Load categories and article types to a meta record
     * 
     * @param category Category name
     * @param subCategory Subcategory name
     * @param articleType Article type
     * @param usage Usage description
     * @param brandName Brand name
     */
    public void loadCategories(String category, String subCategory, String articleType, String usage, String brandName) {
        session.upsert(categoryDataSet.id(CATEGORY_KEY))
            .bin("categories").onMapKey(category,MapOrder.KEY_ORDERED).onMapKey(subCategory).add(1)
            .bin("articleTypes").listAppend(articleType, opt -> opt.addUnique().allowFailures())
            .bin("usage").listAppend(usage, opt -> opt.addUnique().allowFailures())
            .bin("brandNames").listAppend(brandName, opt -> opt.addUnique().allowFailures())
            .execute();
    }

    /**
     * Create a string secondary index on the specified bin
     * 
     * @param binName Name of the bin to index
     * @param indexName Name of the index to create
     */
    public void createStringIndex(String binName, String indexName) {
        try {
            session.createIndex(productDataSet, indexName, binName, IndexType.STRING, IndexCollectionType.DEFAULT);
        } catch (AerospikeException e) {
            // Index already exists or other error - fail gracefully
            System.out.println("Index " + indexName + " already exists or failed to create: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup method called when the service is destroyed
     * Properly closes the AerospikeClient connection
     */
    @PreDestroy
    public void cleanup() {
        if (aerospikeCluster != null) {
            aerospikeCluster.close();
        }
    }

    /**
     * Extracts product image URL from the product data structure
     * Tries search/resolutions/125X161 first, then front/resolutions/125X161
     */
    private String extractProductImage(Product product) {
        Map<String, Object> images = (Map<String, Object>) product.getImages();
        if (images == null) {
            return null;
        }

        // Try search -> resolutions -> 125X161
        String image = getImageFromPath(images, "search", "resolutions", "125X161");
        if (image != null) {
            return image;
        }

        // Fallback: try front -> resolutions -> 125X161
        return getImageFromPath(images, "front", "resolutions", "125X161");
    }

    /**
     * Helper method to safely navigate nested map structure for image extraction
     */
    @SuppressWarnings("unchecked")
    private String getImageFromPath(Map<String, Object> root, String... path) {
        Map<String, Object> current = root;
        
        // Navigate through all path segments except the last one
        for (int i = 0; i < path.length - 1; i++) {
            Object next = current.get(path[i]);
            if (!(next instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) next;
        }
        
        // Get the final value
        Object result = current.get(path[path.length - 1]);
        return result instanceof String ? (String) result : null;
    }

    private static class ProductMapper implements RecordMapper<Product> {
        @Override
        public Product fromMap(Map<String, Object> map, Key recordKey, int generation) {
            return Product.fromMap(map);
        }
        @Override
        public Map<String, Object> toMap(Product element) {
            return Product.toMap(element);
        }
        @Override
        public Object id(Product product) {
            return product.getId();
        }
    }
    
    private static class CartMapper implements RecordMapper<Cart> {

        @Override
        public Cart fromMap(Map<String, Object> map, Key recordKey, int generation) {
            return Cart.fromMap(map);
        }
        @Override
        public Map<String, Object> toMap(Cart element) {
            return Cart.toMap(element);
        }
        @Override
        public Object id(Cart element) {
            return null;
        }
    }
    
    private static class CartItemMapper implements RecordMapper<CartItem> {
        @Override
        public CartItem fromMap(Map<String, Object> map, Key recordKey, int generation) {
            return CartItem.fromMap(map);
        }
        @Override
        public Map<String, Object> toMap(CartItem element) {
            return CartItem.toMap(element);
        }
        @Override
        public Object id(CartItem element) {
            return element.getUserId();
        }
    }

    
} 