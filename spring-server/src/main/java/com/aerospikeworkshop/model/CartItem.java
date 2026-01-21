package com.aerospikeworkshop.model;

import java.util.Map;

import com.aerospike.client.fluent.Value;
import com.aerospike.client.fluent.util.MapUtil;


public class CartItem {
    private String productId;
    private String name;
    private long price;
    private String brandName;
    private int quantity;
    private String image;
    private String userId;
    
    public CartItem() {
        super();
    }
    
    public CartItem(String userId, int quantity, String image, Product product) {
        this(product.getId(),
                product.getName(),
                product.getPrice(),
                product.getBrandName(),
                quantity,
                image,
                userId);
    }
    
    public CartItem(String productId, String name, long price, String brandName, int quantity, String image, String userId) {
        super();
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.brandName = brandName;
        this.quantity = quantity;
        this.image = image;
        this.userId = userId;
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

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    @Override
    public String toString() {
        return "CartItem [productId=" + productId + ", name=" + name + ", price=" + price + ", brandName=" + brandName
                + ", quantity=" + quantity + ", image=" + image + ", userId=" + userId + "]";
    }

    public static Map<String, Value> toMap(CartItem item) {
        if (item == null) {
            return null;
        }
        return MapUtil.buildMap()
                .add("productId", item.getProductId())
                .add("name", item.getName())
                .add("price", item.getPrice())
                .add("brandName", item.getBrandName())
                .add("quantity", item.getQuantity())
                .add("image", item.getImage())
                .add("userId", item.getUserId())
                .done();
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
        return item;
    }
}
