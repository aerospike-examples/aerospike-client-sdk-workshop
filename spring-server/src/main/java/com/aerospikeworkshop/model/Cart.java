package com.aerospikeworkshop.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.aerospike.client.fluent.Value;
import com.aerospike.client.fluent.util.MapUtil;

public class Cart {
    /**
     * Store the products as a Map (key: ProductId) of CartItems
     */
    private Map<String, CartItem> items;

    public Cart() {
        this.items = new HashMap<>();
    }
    public Cart(Map<String, CartItem> items) {
        super();
        this.items = items;
    }
    
    public Cart add(CartItem item) {
        this.items.put(item.getProductId(), item);
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
        if (productId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(items.get(productId));
    }
    
    /**
     * Remove an item with the specified product id from the map
     * @param productId - The productId to remove
     * @return
     */
    public CartItem remove(String productId) {
        return items.remove(productId);
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
                CartItem cartItem = CartItem.fromMap(thisItem.getValue());
                cart.items.put(cartItem.getProductId(), cartItem);
            });
        }
        return cart;
    }
    
    public static Map<String, Value> toMap(Cart cart) {
        List<Map<String, Value>> items = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            items.add(CartItem.toMap(item));
        }
        return MapUtil.buildMap()
                .add("items", items)
                .done();
        
    }
}
