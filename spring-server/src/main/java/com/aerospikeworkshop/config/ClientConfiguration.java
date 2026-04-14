package com.aerospikeworkshop.config;

/**
 * Configuration class for Aerospike client connection details
 * Contains only the essential connection parameters
 */
public class ClientConfiguration {
    private final String hostname;
    private final int port;
    private final String userName;
    private final String password;

    public ClientConfiguration(String hostname, int port, String userName, String password) {
        this.hostname = hostname;
        this.port = port;
        this.userName = userName;
        this.password = password;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "ClientConfiguration [hostname=" + hostname + ", port=" + port + ", userName=" + userName + "]";
    }
}
