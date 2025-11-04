package com.aerospikeworkshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the Aerospike Retail Demo
 * Replaces the Python FastAPI server with a Java Spring Boot implementation
 */
@SpringBootApplication
public class RetailDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetailDemoApplication.class, args);
    }
} 