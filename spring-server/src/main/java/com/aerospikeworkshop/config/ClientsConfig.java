package com.aerospikeworkshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up Aerospike client configuration
 * Provides connection details rather than the client instance itself
 */
@Configuration
public class ClientsConfig {

    @Value("${aerospike.host:localhost}")
    private String aerospikeHost;

    @Value("${aerospike.port:3000}")
    private int aerospikePort;

    @Value("${aerospike.user:#{null}}")
    private String aerospikeUserName;

    @Value("${aerospike.password:#{null}}")
    private String aerospikePassword;

    /**
     * Aerospike client configuration containing connection details
     * Services will use this to create their own client instances
     */
    @Bean
    public ClientConfiguration clientConfiguration() {
        return new ClientConfiguration(aerospikeHost, aerospikePort,
                aerospikeUserName, aerospikePassword);
    }
}
