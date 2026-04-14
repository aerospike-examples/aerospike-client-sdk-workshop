package com.aerospikeworkshop.model;

import java.util.List;
import java.util.Map;

import com.aerospike.client.sdk.util.MapUtil;

public class Product {
    private String brandName;
    private Map<String, Object> images;
    private String subCategory;
    private String gender;
    private long salePrice;
    private long added;
    private String usage;
    private List<String> displayCat;
    private String ageGroup;
    private List<String> colors;
    private Map<String, ?> descriptors;
    private String articleType;
    private long price;
    private String name;
    private List<Map<String, ?>> options;
    private String season;
    private String id;
    private Map<String, String> articleAttr;
    private String variantName;
    private String category;
    
    public String getBrandName() {
        return brandName;
    }
    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }
    public Map<String, Object> getImages() {
        return images;
    }
    public void setImages(Map<String, Object> images) {
        this.images = images;
    }
    public String getSubCategory() {
        return subCategory;
    }
    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
    public long getSalePrice() {
        return salePrice;
    }
    public void setSalePrice(long salePrice) {
        this.salePrice = salePrice;
    }
    public long getAdded() {
        return added;
    }
    public void setAdded(long added) {
        this.added = added;
    }
    public String getUsage() {
        return usage;
    }
    public void setUsage(String usage) {
        this.usage = usage;
    }
    public List<String> getDisplayCat() {
        return displayCat;
    }
    public void setDisplayCat(List<String> displayCat) {
        this.displayCat = displayCat;
    }
    public String getAgeGroup() {
        return ageGroup;
    }
    public void setAgeGroup(String ageGroup) {
        this.ageGroup = ageGroup;
    }
    public List<String> getColors() {
        return colors;
    }
    public void setColors(List<String> colors) {
        this.colors = colors;
    }
    public Map<String, ?> getDescriptors() {
        return descriptors;
    }
    public void setDescriptors(Map<String, ?> descriptors) {
        this.descriptors = descriptors;
    }
    public String getArticleType() {
        return articleType;
    }
    public void setArticleType(String articleType) {
        this.articleType = articleType;
    }
    public long getPrice() {
        return price;
    }
    public void setPrice(long price) {
        this.price = price;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<Map<String, ?>> getOptions() {
        return options;
    }
    public void setOptions(List<Map<String, ?>> options) {
        this.options = options;
    }
    public String getSeason() {
        return season;
    }
    public void setSeason(String season) {
        this.season = season;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Map<String, String> getArticleAttr() {
        return articleAttr;
    }
    public void setArticleAttr(Map<String, String> articleAttr) {
        this.articleAttr = articleAttr;
    }
    public String getVariantName() {
        return variantName;
    }
    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "Product [brandName=" + brandName + ", subCategory=" + subCategory + ", gender=" + gender
                + ", salePrice=" + salePrice + ", usage=" + usage + ", displayCat=" + displayCat + ", ageGroup="
                + ageGroup + ", colors=" + colors + ", articleType=" + articleType + ", price=" + price + ", name="
                + name + ", options=" + options + ", season=" + season + ", id=" + id + ", category=" + category + "]";
    }
    
    public static Product fromMap(Map<String, Object> map) {
        Product result = new Product();
        result.setBrandName(MapUtil.asString(map, "brandName"));
        result.setImages(MapUtil.asMap(map, "images"));
        result.setSubCategory(MapUtil.asString(map, "subCategory"));
        result.setGender(MapUtil.asString(map, "gender"));
        result.setSalePrice(MapUtil.asLong(map, "salePrice"));
        result.setAdded(MapUtil.asLong(map, "added"));
        result.setUsage(MapUtil.asString(map, "usage"));
        result.setDisplayCat(MapUtil.asList(map, "displayCat"));
        result.setAgeGroup(MapUtil.asString(map, "ageGroup"));
        result.setColors(MapUtil.asList(map, "colors"));
        result.setDescriptors(MapUtil.asMap(map, "descriptors"));
        result.setArticleType(MapUtil.asString(map, "articleType"));
        result.setPrice(MapUtil.asLong(map, "price"));
        result.setName(MapUtil.asString(map, "name"));
        result.setOptions(MapUtil.asList(map, "options"));
        result.setSeason(MapUtil.asString(map, "season"));
        result.setId(MapUtil.asString(map, "id"));
        result.setArticleAttr(MapUtil.asMap(map, "articleAttr"));
        result.setVariantName(MapUtil.asString(map, "variantName"));
        result.setCategory(MapUtil.asString(map, "category"));
        return result;
    }
    
    public static Map<String, Object> toMap(Product product) {
        if (product == null) {
            return null;
        }
        return MapUtil.buildMap()
                .add("brandName", product.getBrandName())
                .add("images", product.getImages())
                .add("subCategory", product.getSubCategory())
                .add("gender", product.getGender())
                .add("salePrice", product.getSalePrice())
                .add("added", product.getAdded())
                .add("usage", product.getUsage())
                .add("displayCat", product.getDisplayCat())
                .add("ageGroup", product.getAgeGroup())
                .add("colors", product.getColors())
                .add("descriptors", product.getDescriptors())
                .add("articleType", product.getArticleType())
                .add("price", product.getPrice())
                .add("name", product.getName())
                .add("options", product.getOptions())
                .add("season", product.getSeason())
                .add("id", product.getId())
                .add("articleAttr", product.getArticleAttr())
                .add("variantName", product.getVariantName())
                .add("category", product.getCategory())
                .done();
    }

}
