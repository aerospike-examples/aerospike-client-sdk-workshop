package com.aerospikeworkshop.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aerospikeworkshop.model.Product;
import com.aerospikeworkshop.service.KeyValueServiceInterface;

/**
 * REST controller for the retail demo API - Key-Value operations only
 * Simplified version that focuses on Aerospike key-value and secondary index operations
 */
@RestController
@RequestMapping("/rest/v1")
public class RetailController {

    private final KeyValueServiceInterface keyValueService;

    @Autowired
    public RetailController(KeyValueServiceInterface keyValueService) {
        this.keyValueService = keyValueService;
    }

    /**
     * Health check endpoint to verify CORS and server functionality
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Spring Boot server is running");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Called when navigating to the homepage
     * Performs secondary index queries on multiple "subCategory" values
     * Returns an object containing the result of each query
     */
    @GetMapping("/home")
    public ResponseEntity<Map<String, List<Product>>> getHome() {
        try {
            KeyValueServiceInterface.QueryResult shoes = keyValueService.query("subCategory", "Shoes", 10);
            KeyValueServiceInterface.QueryResult bags = keyValueService.query("subCategory", "Bags", 10);
            KeyValueServiceInterface.QueryResult wallets = keyValueService.query("subCategory", "Wallets", 10);
            KeyValueServiceInterface.QueryResult watches = keyValueService.query("subCategory", "Watches", 10);
            KeyValueServiceInterface.QueryResult headwear = keyValueService.query("subCategory", "Headwear", 10);

            Map<String, List<Product>> result = new HashMap<>();
            result.put("Shoes", shoes.getProducts());
            result.put("Bags", bags.getProducts());
            result.put("Wallets", wallets.getProducts());
            result.put("Watches", watches.getProducts());
            result.put("Headwear", headwear.getProducts());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Called for product details
     * Performs a key-value lookup on the specified product
     * Returns product information only (no vector search or graph recommendations)
     */
    @GetMapping("/get")
    public ResponseEntity<Map<String, Object>> getProduct(@RequestParam("prod") String productId) {
        try {
            // Get product data through a key-value lookup
            Optional<Product> productOptional = keyValueService.getProduct(productId);
            
            if (productOptional.isEmpty()) {
                return ResponseEntity.ok(Map.of("error", "Product not found"));
            }

            // Return product data only - no vector search or graph recommendations
            Map<String, Object> response = new HashMap<>();
            response.put("error", null);
            response.put("product", productOptional.get());
            response.put("related", new ArrayList<>()); // Empty list for compatibility
            response.put("also_bought", new ArrayList<>()); // Empty list for compatibility

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("error", "Product not found"));
        }
    }

    /**
     * Called when searching products
     * Performs advanced search with multiple filters
     * Returns products matching the search criteria
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "articleType", required = false) String articleType,
            @RequestParam(value = "usage", required = false) String usage,
            @RequestParam(value = "brandName", required = false) String brandName) {
        try {
            // Perform advanced search with multiple filters
            KeyValueServiceInterface.QueryResult queryResult = keyValueService.advancedSearch(category, articleType, usage, brandName, query, 20);

            Map<String, Object> response = new HashMap<>();
            response.put("products", queryResult.getProducts());
            response.put("time", queryResult.getTimeMs());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all available categories
     * Returns list of category names for dropdown population
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        try {
            List<String> categories = keyValueService.getCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("categories", categories);
            response.put("count", categories.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all available article types
     * Returns list of article type names for dropdown population
     */
    @GetMapping("/article-types")
    public ResponseEntity<Map<String, Object>> getArticleTypes() {
        try {
            List<String> articleTypes = keyValueService.getArticleTypes();

            Map<String, Object> response = new HashMap<>();
            response.put("articleTypes", articleTypes);
            response.put("count", articleTypes.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all available usage types
     * Returns list of usage names for dropdown population
     */
    @GetMapping("/usage-types")
    public ResponseEntity<Map<String, Object>> getUsageTypes() {
        try {
            List<String> usageTypes = keyValueService.getUsage();

            Map<String, Object> response = new HashMap<>();
            response.put("usageTypes", usageTypes);
            response.put("count", usageTypes.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all available brand names
     * Returns list of brand names for dropdown population
     */
    @GetMapping("/brand-names")
    public ResponseEntity<Map<String, Object>> getBrandNames() {
        try {
            List<String> brandNames = keyValueService.getBrandNames();

            Map<String, Object> response = new HashMap<>();
            response.put("brandNames", brandNames);
            response.put("count", brandNames.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Called when looking at specific "category", "subCategory", or "usage" pages
     * Performs a secondary index query on the provided index using the provided filter value
     * Returns the first 20 results, along with execution time
     */
    @GetMapping("/category")
    public ResponseEntity<Map<String, Object>> getCategory(@RequestParam("idx") String index, 
                                                          @RequestParam("filter_value") String filterValue) {
        try {
            // Get the results of the secondary index query
            KeyValueServiceInterface.QueryResult queryResult = keyValueService.query(index, filterValue, 20);

            Map<String, Object> response = new HashMap<>();
            response.put("products", queryResult.getProducts());
            response.put("time", queryResult.getTimeMs());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
} 