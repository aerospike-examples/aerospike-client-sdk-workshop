package com.aerospikeworkshop.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.aerospikeworkshop.model.Product;

/**
 * Service to load product data from JSON files into Aerospike
 * Equivalent to the Python load_data.py module (simplified for key-value only)
 */
@Service
public class DataLoadingService {

    private final JsonParsingService jsonParsingService;
    private final KeyValueServiceInterface keyValueService;

    public DataLoadingService(JsonParsingService jsonParsingService, KeyValueServiceInterface keyValueService) {
        this.jsonParsingService = jsonParsingService;
        this.keyValueService = keyValueService;
    }

    /**
     * Load all product data from the specified data root directory
     * 
     * @param dataRootPath Root path to the data directory
     * @return LoadResult containing statistics about the loading process
     * @throws IOException if files cannot be read
     */
    public LoadResult loadAllData(String dataRootPath) throws IOException {
        // Create secondary indexes first
        createSecondaryIndexes();
        
        // Get all JSON files
        List<String> jsonFiles = jsonParsingService.getStyleFiles(dataRootPath);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        // Process each file
        for (String filePath : jsonFiles) {
            try {
                loadSingleProduct(filePath);
                successCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
                System.err.println("Error loading file " + filePath + ": " + e.getMessage());
            }
            
        }
        
        return new LoadResult(successCount.get(), errorCount.get(), jsonFiles.size());
    }

    /**
     * Load a single product from a JSON file
     * 
     * @param filePath Path to the product JSON file
     * @throws IOException if the file cannot be processed
     */
    public void loadSingleProduct(String filePath) throws IOException {
        // Parse the JSON file
        Map<String, Object> rawData = jsonParsingService.parseProductFile(filePath);
        
        // Extract product ID from filename
        String productId = jsonParsingService.extractProductId(filePath);
        
        // Format the product data
        Map<String, Object> productMap = jsonParsingService.formatProductData(rawData, productId);
        
        // Load categories metadata
        loadProductCategories(productMap);
        
        // Store the product in Aerospike
        keyValueService.storeProduct(Product.fromMap(productMap));
    }

    /**
     * Create the necessary secondary indexes
     */
    private void createSecondaryIndexes() {
        Map<String, String> indexes = new HashMap<>();
        indexes.put("category", "cat_idx");
        indexes.put("subCategory", "subCat_idx");
        indexes.put("usage", "usage_idx");
        indexes.put("brandName", "brand_idx");
        indexes.put("gender", "gender_idx");
        indexes.put("season", "season_idx");
        
        for (Map.Entry<String, String> entry : indexes.entrySet()) {
            keyValueService.createStringIndex(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Load category metadata for a product
     */
    private void loadProductCategories(Map<String, Object> product) {
        String category = (String) product.get("category");
        String subCategory = (String) product.get("subCategory");
        String articleType = (String) product.get("articleType");
        String usage = (String) product.get("usage");
        String brandName = (String) product.get("brandName");
        
        if (category != null && subCategory != null && articleType != null && usage != null && brandName != null) {
            keyValueService.loadCategories(category, subCategory, articleType, usage, brandName);
        }
    }

    /**
     * Get count of products currently in the database
     * 
     * @return Number of products
     */
    public long getProductCount() {
        return keyValueService.getProductCount();
    }

    /**
     * Create a single secondary index
     * 
     * @param binName Name of the bin to index
     * @param indexName Name of the index to create
     */
    public void createSingleIndex(String binName, String indexName) {
        keyValueService.createStringIndex(binName, indexName);
    }

    /**
     * Clear all product data (for testing purposes)
     * WARNING: This will delete all data in the products set
     */
    public void clearAllData() {
        keyValueService.clearAllData();
    }

    /**
     * Result class for data loading operations
     */
    public static class LoadResult {
        private final int successCount;
        private final int errorCount;
        private final int totalFiles;

        public LoadResult(int successCount, int errorCount, int totalFiles) {
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.totalFiles = totalFiles;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public int getTotalFiles() {
            return totalFiles;
        }

        public double getSuccessRate() {
            return totalFiles > 0 ? (double) successCount / totalFiles * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("LoadResult{total=%d, success=%d, errors=%d, successRate=%.2f%%}", 
                    totalFiles, successCount, errorCount, getSuccessRate());
        }
    }
}
