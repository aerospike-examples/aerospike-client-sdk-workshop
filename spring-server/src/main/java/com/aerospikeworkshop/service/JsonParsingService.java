package com.aerospikeworkshop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Service to parse product JSON files
 * Equivalent to JSON parsing functionality in Python load_data.py
 */
@Service
public class JsonParsingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse a single product JSON file
     * 
     * @param filePath Path to the JSON file
     * @return Map containing product data
     * @throws IOException if file cannot be read or parsed
     */
    public Map<String, Object> parseProductFile(String filePath) throws IOException {
        JsonNode rootNode = objectMapper.readTree(new File(filePath));
        JsonNode dataNode = rootNode.get("data");
        
        if (dataNode == null) {
            throw new IOException("Invalid JSON structure - missing 'data' node");
        }
        
        return convertJsonNodeToMap(dataNode);
    }

    /**
     * Get all JSON files from the styles directory
     * 
     * @param dataRootPath Root path to the data directory
     * @return List of JSON file paths
     * @throws IOException if directory cannot be read
     */
    public List<String> getStyleFiles(String dataRootPath) throws IOException {
        Path stylesPath = Paths.get(dataRootPath, "styles");
        
        if (!Files.exists(stylesPath) || !Files.isDirectory(stylesPath)) {
            throw new IOException("Styles directory not found: " + stylesPath);
        }

        List<String> jsonFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(stylesPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".json"))
                 .forEach(path -> jsonFiles.add(path.toString()));
        }
        
        return jsonFiles;
    }

    /**
     * Extract product ID from file path
     * 
     * @param filePath Path to the JSON file
     * @return Product ID (filename without extension)
     */
    public String extractProductId(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    /**
     * Convert product data to the format expected by Aerospike
     * 
     * @param data Raw product data from JSON
     * @param productId Product ID
     * @return Formatted product map
     */
    public Map<String, Object> formatProductData(Map<String, Object> data, String productId) {
        Map<String, Object> product = new HashMap<>();
        
        product.put("id", productId);
        product.put("price", data.get("price"));
        product.put("salePrice", data.get("discountedPrice"));
        product.put("name", data.get("productDisplayName"));
        product.put("descriptors", data.get("productDescriptors"));
        product.put("variantName", data.get("variantName"));
        product.put("added", data.get("catalogAddDate"));
        product.put("brandName", data.get("brandName"));
        product.put("brandProfile", data.get("brandUserProfile"));
        product.put("ageGroup", data.get("ageGroup"));
        product.put("gender", data.get("gender"));
        
        // Handle colors array
        List<String> colors = Arrays.asList(
            (String) data.get("baseColour"),
            (String) data.get("colour1"),
            (String) data.get("colour2")
        );
        product.put("colors", colors);
        
        product.put("season", data.get("season"));
        product.put("usage", data.get("usage"));
        product.put("articleAttr", data.get("articleAttributes"));
        product.put("images", data.get("styleImages"));
        
        // Handle display categories
        String displayCategories = (String) data.get("displayCategories");
        if (displayCategories != null && !displayCategories.isEmpty()) {
            product.put("displayCat", Arrays.asList(displayCategories.split(",")));
        } else {
            product.put("displayCat", Arrays.asList("NA"));
        }
        
        // Handle nested category data
        Map<String, Object> masterCategory = (Map<String, Object>) data.get("masterCategory");
        if (masterCategory != null) {
            product.put("category", masterCategory.get("typeName"));
        }
        
        Map<String, Object> subCategory = (Map<String, Object>) data.get("subCategory");
        if (subCategory != null) {
            product.put("subCategory", subCategory.get("typeName"));
        }
        
        Map<String, Object> articleType = (Map<String, Object>) data.get("articleType");
        if (articleType != null) {
            product.put("articleType", articleType.get("typeName"));
        }
        
        product.put("options", data.get("styleOptions"));
        
        // Handle nested styles data
        Map<String, Object> colours = (Map<String, Object>) data.get("colours");
        if (colours != null) {
            Object styles = colours.get("colors");
            if (styles != null) {
                product.put("styles", styles);
            }
        }
        
        return product;
    }

    /**
     * Convert JsonNode to Map recursively
     */
    private Map<String, Object> convertJsonNodeToMap(JsonNode node) {
        Map<String, Object> result = new HashMap<>();
        
        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            
            if (value.isObject()) {
                result.put(key, convertJsonNodeToMap(value));
            } else if (value.isArray()) {
                List<Object> list = new ArrayList<>();
                for (JsonNode arrayItem : value) {
                    if (arrayItem.isObject()) {
                        list.add(convertJsonNodeToMap(arrayItem));
                    } else {
                        list.add(getNodeValue(arrayItem));
                    }
                }
                result.put(key, list);
            } else {
                result.put(key, getNodeValue(value));
            }
        });
        
        return result;
    }

    /**
     * Extract primitive value from JsonNode
     */
    private Object getNodeValue(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isInt()) {
            return node.intValue();
        } else if (node.isLong()) {
            return node.longValue();
        } else if (node.isDouble()) {
            return node.doubleValue();
        } else {
            return node.textValue();
        }
    }
}
