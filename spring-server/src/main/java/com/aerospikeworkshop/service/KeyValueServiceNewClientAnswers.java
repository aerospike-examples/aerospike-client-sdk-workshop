package com.aerospikeworkshop.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.aerospike.client.fluent.AerospikeException;
import com.aerospike.client.fluent.AerospikeException.GenerationException;
import com.aerospike.client.fluent.Cluster;
import com.aerospike.client.fluent.ClusterDefinition;
import com.aerospike.client.fluent.DataSet;
import com.aerospike.client.fluent.Key;
import com.aerospike.client.fluent.Log;
import com.aerospike.client.fluent.Log.Level;
import com.aerospike.client.fluent.RecordMapper;
import com.aerospike.client.fluent.RecordResult;
import com.aerospike.client.fluent.RecordStream.ObjectWithMetadata;
import com.aerospike.client.fluent.Session;
import com.aerospike.client.fluent.TypeSafeDataSet;
import com.aerospike.client.fluent.Value;
import com.aerospike.client.fluent.cdt.MapOrder;
import com.aerospike.client.fluent.policy.Behavior;
import com.aerospike.client.fluent.query.IndexCollectionType;
import com.aerospike.client.fluent.query.IndexType;
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
@Profile("new-client-answers")
public class KeyValueServiceNewClientAnswers implements KeyValueServiceInterface {

    private static final String ITEMS_BIN = "items";
    private static final String NAMESPACE = "test";
    private static final String PRODUCT_SET = "products";
    private static final String CARTS_SET = "shopping_carts";
    private static final String CATEGORY_SET = "cat_index";
    private static final String CATEGORY_KEY = "product_meta";

    private final Cluster aerospikeCluster;
    
    private final TypeSafeDataSet<Product> productDataSet = new TypeSafeDataSet<>(NAMESPACE, PRODUCT_SET, Product.class);
    private final TypeSafeDataSet<CartItem> cartDataSet = new TypeSafeDataSet<>(NAMESPACE, CARTS_SET, CartItem.class);
    private final DataSet categoryDataSet = DataSet.of(NAMESPACE, CATEGORY_SET);
    private final Session session;
    private final ProductMapper productMapper = new ProductMapper();
    private final CartItemMapper cartItemMapper = new CartItemMapper();
    private final CartMapper cartMapper = new CartMapper();
    
    public KeyValueServiceNewClientAnswers(ClientConfiguration config) {
        ClusterDefinition definition = new ClusterDefinition(config.getHostname(), config.getPort())
                .withNativeCredentials(config.getUserName(), config.getPassword())
                .withLogLevel(Level.DEBUG)
                .preferringRacks(1);
        
        if (config.getTlsCaFile() != null || config.getTlsName() != null) {
            definition.withTlsConfigOf()
                .caFile(config.getTlsCaFile())
                .tlsName(config.getTlsName())
            .done();
        }
        aerospikeCluster = definition.connect();
        
        session = aerospikeCluster.createSession(Behavior.DEFAULT);
    }

    /**
     * Key-Value lookup of a specified product
     * Gets the product record and returns the record bins
     * 
     * @param productId Product identifier
     * @return Map containing the product data
     */
    public Optional<Product> getProduct(String productId) {
        return session.query(productDataSet.id(productId))
                .execute()
                .getFirst(productMapper);
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

        List<Product> products = session.query(productDataSet)
                .where("$.%s == '%s'", index, filterValue)
                .readingOnlyBins("id", "name", "images", "brandName")
                .limit(count)
                .execute()
                .toObjectList(productMapper);
        
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

        String dsl = "";
        for (Entry<String, String> thisEntry : indexes.entrySet()) {
            if (!thisEntry.getValue().isEmpty()) {
                if (!dsl.isEmpty()) {
                    dsl += " and ";
                }
                dsl += String.format("$.%s == '%s'", thisEntry.getKey(), thisEntry.getValue());
            }
        }
        
        System.out.println("DSL: " + dsl);
        
        List<Product> products = session.query(productDataSet)
                .where(dsl)
                .limit(count)
                .execute()
                .toObjectList(productMapper);
        
        long endTime = System.currentTimeMillis() - startTime;
        return new KeyValueServiceInterface.QueryResult(products, endTime);
    }

    /**
     * Store a product record in Aerospike
     * 
     * @param product Product data map
     * @param productId Product identifier
     */
    public void storeProduct(Product product) {
        session.insert(productDataSet)
                .object(product)
                .using(productMapper)
                .execute();
    }

    // Cart operations
    /**
     * Get the user's shopping cart. If the cart does not exist in the database, return 
     * a new cart.
     * @param userId - THe user whose cart is to be returned.
     * @return the Cart of the user. Guaranteed to be non-null
     */
    public Cart getCart(String userId) {
        try {
            return session.query(cartDataSet.id(userId))
                .execute()
                .getFirst(cartMapper)
                .orElseGet(() -> new Cart());
        } catch (Exception e) {
            System.err.println("Error getting cart: " + e.getMessage());
            return new Cart();
        }
    }

    public Cart addToCart(String userId, String productId, int quantity) {
        try {
            // Get product details first
            Optional<Product> productOptional = getProduct(productId);
            
            Product product = productOptional
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            Key key = cartDataSet.id(userId);
//            Cart cart;
            
            // Get product image
            String image = extractProductImage(product);
            Cart resultCart = null;
            while (resultCart == null) {
                try {
                    Optional<ObjectWithMetadata<Cart>> cartAndMetadata = session.query(key)
                            .execute()
                            .getFirstWithMetadata(cartMapper);
                    

                    resultCart = cartAndMetadata
                        .map(cartWithMetadata -> {
                            Cart cart = cartWithMetadata.get();
                            cart.findItem(productId)
                                .ifPresentOrElse(item -> {
                                    
                                    // The item exists in the record, just update the quantity
                                    item.setQuantity(item.getQuantity() + quantity);
                                    
                                    session.update(key)
                                        .bin(ITEMS_BIN).onMapKey(productId).onMapKey("quantity").add(quantity)
                                        .ensureGenerationIs(cartWithMetadata.getGeneration())
                                        .execute();
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
                            session.insert(key)
                                .bin(ITEMS_BIN).onMapKey(productId).setTo(newItem, cartItemMapper)
                                .execute();
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
        session.delete(categoryDataSet.id(CATEGORY_KEY));
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
            .bin("articleTypes").listAppendUnique(articleType, true)
            .bin("usage").listAppendUnique(usage, true)
            .bin("brandNames").listAppendUnique(brandName, true)
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
            session.createIndex(productDataSet, indexName, binName, com.aerospike.client.fluent.info.classes.IndexType.STRING, IndexCollectionType.DEFAULT);
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
        public Map<String, Value> toMap(Product element) {
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
        public Map<String, Value> toMap(Cart element) {
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
        public Map<String, Value> toMap(CartItem element) {
            return CartItem.toMap(element);
        }
        @Override
        public Object id(CartItem element) {
            return element.getUserId();
        }
    }

    
} 