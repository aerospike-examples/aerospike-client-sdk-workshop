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
    private final String tlsName;
    private final String tlsCaFile;

    public ClientConfiguration(String hostname, int port, String userName, 
            String password, String tlsName, String tlsCaFile) {
        this.hostname = hostname;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.tlsName = tlsName;
        this.tlsCaFile = tlsCaFile;
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
    
    public String getTlsName() {
        return tlsName;
    }
    public String getTlsCaFile() {
        return tlsCaFile;
    }

    @Override
    public String toString() {
        return "ClientConfiguration [hostname=" + hostname + ", port=" + port + ", userName=" + userName + ", password="
                + password + ", tlsName=" + tlsName + ", tlsCaFile=" + tlsCaFile + "]";
    }
}
