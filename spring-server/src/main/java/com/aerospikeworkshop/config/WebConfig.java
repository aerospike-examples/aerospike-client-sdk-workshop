package com.aerospikeworkshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static files from /static/** paths
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/static/")
                .setCachePeriod(31536000); // 1 year cache for static assets

        // Serve other assets (favicon, manifest, etc.)
        registry.addResourceHandler("/favicon.ico", "/manifest.json", "/robots.txt")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400); // 1 day cache

        // Handle SPA routing - serve index.html for all non-API routes
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // If the requested resource exists, serve it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // For SPA routing, serve index.html for non-API routes
                        if (!resourcePath.startsWith("rest/")) {
                            return new ClassPathResource("/static/index.html");
                        }
                        
                        return null;
                    }
                });
    }
}
