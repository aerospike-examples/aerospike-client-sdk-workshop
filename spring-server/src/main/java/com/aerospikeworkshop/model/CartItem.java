package com.aerospikeworkshop.model;

import java.util.Map;

import com.aerospike.MapUtil;
import com.aerospike.client.Value;

public class CartItem {
    private String productId;
    private String name;
    private long price;
    private String brandName;
    private int quantity;
    private String image;
    private String userId;
    private String size;
    
    public CartItem() {
        super();
    }
    
    public CartItem(String userId, int quantity, String image, Product product) {
        this(userId, quantity, image, product, null);
    }
    
    public CartItem(String userId, int quantity, String image, Product product, String size) {
        this(product.getId(),
                product.getName(),
                product.getPrice(),
                product.getBrandName(),
                quantity,
                image,
                userId,
                size);
    }
    
    public CartItem(String productId, String name, long price, String brandName, int quantity, String image, String userId) {
        this(productId, name, price, brandName, quantity, image, userId, null);
    }
    
    public CartItem(String productId, String name, long price, String brandName, int quantity, String image, String userId, String size) {
        super();
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.brandName = brandName;
        this.quantity = quantity;
        this.image = image;
        this.userId = userId;
        this.size = size;
    }
    public String getProductId() {
        return productId;
    }
    public String getName() {
        return name;
    }
    public long getPrice() {
        return price;
    }
    public String getBrandName() {
        return brandName;
    }
    public int getQuantity() {
        return quantity;
    }
    public String getImage() {
        return image;
    }
    public String getUserId() {
        return userId;
    }
    
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    @Override
    public String toString() {
        return "CartItem [productId=" + productId + ", name=" + name + ", price=" + price + ", brandName=" + brandName
                + ", quantity=" + quantity + ", image=" + image + ", userId=" + userId + ", size=" + size + "]";
    }

    public static Map<String, Value> toMap(CartItem item) {
        if (item == null) {
            return null;
        }
        MapUtil.MapBuilder builder = MapUtil.buildMap()
                .add("productId", item.getProductId())
                .add("name", item.getName())
                .add("price", item.getPrice())
                .add("brandName", item.getBrandName())
                .add("quantity", item.getQuantity())
                .add("image", item.getImage())
                .add("userId", item.getUserId());
        if (item.getSize() != null) {
            builder.add("size", item.getSize());
        }
        return builder.done();
    }
    
    public static CartItem fromMap(Map<String, Object> map) {
        CartItem item = new CartItem();
        item.brandName = MapUtil.asString(map, "brandName");
        item.name = MapUtil.asString(map, "name");
        item.price = MapUtil.asLong(map, "price");
        item.productId = MapUtil.asString(map, "productId");
        item.quantity = MapUtil.asInt(map, "quantity");
        item.image = MapUtil.asString(map, "image");
        item.userId = MapUtil.asString(map, "userId");
        String size = MapUtil.asString(map, "size");
        item.size = (size != null && !size.isEmpty()) ? size : null;
        return item;
    }
}
