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

    @Value("${aerospike.tls-name:#{null}}")
    private String tlsName;

    @Value("${aerospike.tls-cafile:#{null}}")
    private String tlsCaFile;


//    -U paradmin -P paradmin1234567890 -h 10.132.1.117 -p 4000 
//    --tls-enable 
//    --tls-name b3317dc5-c3b5-41ee-854c-bf6e9773cf77.aerospike.internal 
//    --tls-cafile /etc/aerospike/ssl/b3317dc5-c3b5-41ee-854c-bf6e9773cf77.aerospike.internal/cacert.pem
    /**
     * Aerospike client configuration containing connection details
     * Services will use this to create their own client instances
     */
    @Bean
    public ClientConfiguration clientConfiguration() {
        return new ClientConfiguration(aerospikeHost, aerospikePort, 
                aerospikeUserName, aerospikePassword,
                tlsName, tlsCaFile);
    }
} 