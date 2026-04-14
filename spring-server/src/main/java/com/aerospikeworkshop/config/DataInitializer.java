package com.aerospikeworkshop.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.aerospikeworkshop.service.DataLoadingService;

/**
 * Automatically loads sample data on application startup if the database is empty.
 * Idempotent — skips loading if products already exist.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DataLoadingService dataLoadingService;

    public DataInitializer(DataLoadingService dataLoadingService) {
        this.dataLoadingService = dataLoadingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            long existingCount = dataLoadingService.getProductCount();
            if (existingCount > 0) {
                log.info("Database already contains {} products, skipping auto-load", existingCount);
                return;
            }

            Path dataPath = resolveDataPath();
            log.info("Auto-loading sample data from {}", dataPath);

            DataLoadingService.LoadResult result = dataLoadingService.loadAllData(dataPath.toString());
            log.info("Auto-load complete: {}", result);

        } catch (Exception e) {
            log.warn("Auto-load failed (database may not be ready): {}", e.getMessage());
            log.info("You can manually load data via: POST /rest/v1/data/load?dataPath=<path>");
        }
    }

    private Path resolveDataPath() {
        // Try relative path from working directory (typical for running from spring-server/)
        Path relative = Paths.get("../data");
        if (relative.resolve("styles").toFile().isDirectory()) {
            return relative.toAbsolutePath().normalize();
        }

        // Try from project root (typical for running from repo root)
        Path fromRoot = Paths.get("data");
        if (fromRoot.resolve("styles").toFile().isDirectory()) {
            return fromRoot.toAbsolutePath().normalize();
        }

        // Fallback
        return relative.toAbsolutePath().normalize();
    }
}
