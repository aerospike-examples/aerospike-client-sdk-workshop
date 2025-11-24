package com.aerospikeworkshop.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.aerospike.MapUtil;
import com.aerospike.client.Value;

public class Cart {
    /**
     * Store the products as a Map (key: ProductId or ProductId-Size) of CartItems
     */
    private Map<String, CartItem> items;

    public Cart() {
        this.items = new LinkedHashMap<>();
    }
    public Cart(Map<String, CartItem> items) {
        super();
        this.items = items;
    }
    
    /**
     * Create a composite key for cart items that includes size.
     * @param productId Product identifier
     * @param size Optional size (can be null)
     * @return Composite key string
     */
    private static String makeItemKey(String productId, String size) {
        if (size != null && !size.isEmpty()) {
            return productId + "-" + size;
        }
        return productId;
    }
    
    public Cart add(CartItem item) {
        String key = makeItemKey(item.getProductId(), item.getSize());
        this.items.put(key, item);
        return this;
    }

    public List<CartItem> getItems() {
        return items.entrySet()
                .stream()
                .map(entry -> entry.getValue())
                .toList();
    }

    public double getTotal() {
        return items.entrySet().stream()
                .mapToDouble(item -> item.getValue().getPrice() * item.getValue().getQuantity())
                .sum();
    }
    
    public int getItemCount() {
        return items.entrySet().stream()
                .mapToInt(item -> item.getValue().getQuantity())
                .sum();
    }
    
    public Optional<CartItem> findItem(String productId) {
        return findItem(productId, null);
    }
    
    /**
     * Find an item by product_id and optionally by size.
     * @param productId Product identifier
     * @param size Optional size (can be null)
     * @return Optional containing the CartItem if found
     */
    public Optional<CartItem> findItem(String productId, String size) {
        if (productId == null) {
            return Optional.empty();
        }
        String key = makeItemKey(productId, size);
        return Optional.ofNullable(items.get(key));
    }
    
    /**
     * Remove an item with the specified product id from the map
     * @param productId - The productId to remove
     * @return Removed CartItem or null
     */
    public CartItem remove(String productId) {
        return remove(productId, null);
    }
    
    /**
     * Remove an item by product_id and optionally by size.
     * @param productId Product identifier
     * @param size Optional size (can be null)
     * @return Removed CartItem or null
     */
    public CartItem remove(String productId, String size) {
        String key = makeItemKey(productId, size);
        return items.remove(key);
    }
    
    @Override
    public String toString() {
        return "Cart [items=" + items + "]";
    }
    public static Cart fromMap(Map<String, Object> map) {
        Cart cart = new Cart();
        Map<String, Map<String, Object>> items = (Map<String, Map<String, Object>>) map.get("items");
        if (items != null) {
            items.entrySet().forEach(thisItem -> {
                // Use the key from Aerospike directly (it's already the composite key we stored)
                String itemKey = thisItem.getKey();
                CartItem cartItem = CartItem.fromMap(thisItem.getValue());
                // Verify the key matches what we expect, or use the stored key
                String expectedKey = makeItemKey(cartItem.getProductId(), cartItem.getSize());
                // Use the key from Aerospike as the authoritative source
                cart.items.put(itemKey, cartItem);
            });
        }
        return cart;
    }
    
    public static Map<String, Value> toMap(Cart cart) {
        Map<String, Value> itemsMap = new LinkedHashMap<>();
        for (Map.Entry<String, CartItem> entry : cart.items.entrySet()) {
            String itemKey = entry.getKey(); // This is the composite key
            CartItem item = entry.getValue();
            itemsMap.put(itemKey, Value.get(CartItem.toMap(item)));
        }
        return MapUtil.buildMap()
                .add("items", itemsMap)
                .done();
        
    }
}
