package com.aerospikeworkshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aerospikeworkshop.service.DataLoadingService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for data loading operations
 * Exposes the functionality of the Python load_data.py and key_value.py modules
 */
@RestController
@RequestMapping("/rest/v1/data")
public class DataLoadingController {

    private final DataLoadingService dataLoadingService;

    @Autowired
    public DataLoadingController(DataLoadingService dataLoadingService) {
        this.dataLoadingService = dataLoadingService;
    }

    /**
     * Load all product data from the specified directory
     * 
     * @param dataPath Root path to the data directory containing styles and images folders
     * @return Load result with statistics
     */
    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> loadData(@RequestParam("dataPath") String dataPath) {
        try {
            // Validate that the path exists and is a directory
            if (!Files.exists(Paths.get(dataPath)) || !Files.isDirectory(Paths.get(dataPath))) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid data path: " + dataPath);
                errorResponse.put("message", "Path does not exist or is not a directory");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Check for required subdirectories
            if (!Files.exists(Paths.get(dataPath, "styles"))) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing styles directory");
                errorResponse.put("message", "The data path must contain a 'styles' subdirectory with JSON files");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Perform the data loading
            DataLoadingService.LoadResult result = dataLoadingService.loadAllData(dataPath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalFiles", result.getTotalFiles());
            response.put("successCount", result.getSuccessCount());
            response.put("errorCount", result.getErrorCount());
            response.put("successRate", result.getSuccessRate());
            response.put("message", "Data loading completed");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "IO Error during data loading");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unexpected error during data loading");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Load a single product from a JSON file
     * 
     * @param filePath Path to the specific JSON file to load
     * @return Success or error response
     */
    @PostMapping("/load-single")
    public ResponseEntity<Map<String, Object>> loadSingleProduct(@RequestParam("filePath") String filePath) {
        try {
            // Validate that the file exists
            if (!Files.exists(Paths.get(filePath)) || !Files.isRegularFile(Paths.get(filePath))) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid file path: " + filePath);
                errorResponse.put("message", "File does not exist or is not a regular file");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Load the single product
            dataLoadingService.loadSingleProduct(filePath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", filePath);
            response.put("message", "Product loaded successfully");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "IO Error loading product");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("filePath", filePath);
            return ResponseEntity.internalServerError().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unexpected error loading product");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("filePath", filePath);
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get the current product count in the database
     * 
     * @return Product count information
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getProductCount() {
        try {
            long count = dataLoadingService.getProductCount();

            Map<String, Object> response = new HashMap<>();
            response.put("productCount", count);
            response.put("message", "Product count retrieved successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error retrieving product count");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Clear all product data (for testing purposes)
     * WARNING: This will delete all data
     * 
     * @param confirm Confirmation parameter (must be "yes-delete-all")
     * @return Success or error response
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAllData(@RequestParam("confirm") String confirm) {
        if (!"yes-delete-all".equals(confirm)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Missing or invalid confirmation");
            errorResponse.put("message", "To clear all data, provide confirm=yes-delete-all parameter");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            dataLoadingService.clearAllData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All product data cleared");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error clearing data");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Create secondary indexes required for the retail demo
     * Creates indexes for category, subCategory, usage, and brandName
     * 
     * @return Index creation result
     */
    @PostMapping("/create-indexes")
    public ResponseEntity<Map<String, Object>> createSecondaryIndexes() {
        try {
            Map<String, String> indexes = new HashMap<>();
            indexes.put("category", "cat_idx");
            indexes.put("subCategory", "subCat_idx");
            indexes.put("usage", "usage_idx");
            indexes.put("brandName", "brand_idx");
            indexes.put("articleType", "article_idx");

            Map<String, String> results = new HashMap<>();
            int successCount = 0;
            int errorCount = 0;

            for (Map.Entry<String, String> entry : indexes.entrySet()) {
                try {
                    dataLoadingService.createSingleIndex(entry.getKey(), entry.getValue());
                    results.put(entry.getValue(), "Created successfully");
                    successCount++;
                } catch (Exception e) {
                    results.put(entry.getValue(), "Error: " + e.getMessage());
                    errorCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", errorCount == 0);
            response.put("totalIndexes", indexes.size());
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("indexResults", results);
            response.put("message", "Secondary index creation completed");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error creating secondary indexes");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Health check for the data loading service
     * 
     * @return Service status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Data Loading Service");
        response.put("message", "Data loading service is operational");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}
