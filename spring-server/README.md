# Spring Boot Retail Demo Server - Key-Value Only

This is a Spring Boot Java implementation that provides key-value operations for the Aerospike retail demo. It focuses on Aerospike key-value and secondary index operations only.

## Overview

The Spring Boot server provides core functionality for the retail demo:
- **Key-Value Operations**: Product lookups and secondary index queries
- **Secondary Index Queries**: Filtering products by category, subcategory, and usage
- **Simple Text Search**: Basic search functionality using secondary indexes
- **CORS Support**: Full cross-origin support for React frontend
- **Static File Serving**: Serves React frontend from `src/main/resources/static`
- **SPA Routing**: Handles client-side routing for React application

## Project Structure

```
spring-server/
├── src/
│   ├── main/
│   │   ├── java/com/aerospike/
│   │   │   ├── RetailDemoApplication.java     # Main Spring Boot application
│   │   │   ├── config/
│   │   │   │   ├── ClientsConfig.java         # Aerospike client configuration
│   │   │   │   ├── CorsConfig.java            # CORS configuration
│   │   │   │   └── WebConfig.java             # Static file serving & SPA routing
│   │   │   ├── controller/
│   │   │   │   └── RetailController.java      # REST API endpoints
│   │   │   └── service/
│   │   │       ├── KeyValueServiceInterface.java     # Common interface for database operations
│   │   │       ├── KeyValueServiceOldClient.java     # Standard implementation (@Profile("old-client"))
│   │   │       ├── KeyValueServiceNewClient.java     # Enhanced implementation (@Profile("new-client"))
│   │   │       ├── DataLoadingService.java           # Data loading and index management
│   │   │       └── JsonParsingService.java           # JSON file parsing utilities
│   │   └── resources/
│   │       └── application.yml                # Application configuration
│   └── test/
│       └── java/com/aerospike/                # Test classes
├── pom.xml                                     # Maven dependencies
├── Dockerfile                                  # Container configuration
└── README.md                                   # This file
```

## API Endpoints

The server provides simplified REST API endpoints:

### Product Query Endpoints

#### 1. Health Check (`GET /rest/v1/health`)
Returns server status and health information.

#### 2. Home Page (`GET /rest/v1/home`)
Returns products organized by subcategories for the homepage.

#### 3. Product Details (`GET /rest/v1/get?prod={productId}`)
Returns detailed product information only (no recommendations).

#### 4. Search (`GET /rest/v1/search?q={query}`)
Performs basic text search using secondary indexes.

#### 5. Category Filter (`GET /rest/v1/category?idx={index}&filter_value={value}`)
Returns products filtered by category, subcategory, or usage.

### Data Loading Endpoints

#### 6. Data Loading Health Check (`GET /rest/v1/data/health`)
Returns status of the data loading service.

#### 7. Load All Data (`POST /rest/v1/data/load?dataPath={path}`)
Loads all product data from JSON files in the specified directory.
- **dataPath**: Root path to directory containing `styles/` subdirectory with JSON files
- Returns loading statistics (total files, success count, error count, success rate)

#### 8. Load Single Product (`POST /rest/v1/data/load-single?filePath={path}`)
Loads a single product from a specific JSON file.
- **filePath**: Path to a specific JSON file to load

#### 9. Get Product Count (`GET /rest/v1/data/count`)
Returns the current number of products in the database.

#### 10. Create Secondary Indexes (`POST /rest/v1/data/create-indexes`)
Creates the 4 required secondary indexes for the retail demo.
- Creates indexes for: `category`, `subCategory`, `usage`, `brandName`
- Returns creation status for each index

#### 11. Clear All Data (`DELETE /rest/v1/data/clear?confirm=yes-delete-all`)
**WARNING**: Deletes all product data from the database.
- **confirm**: Must be exactly "yes-delete-all" to proceed

## CORS Configuration

The server is configured to allow cross-origin requests from any origin, making it compatible with React frontends running on different ports or domains:

- **Allowed Origins**: All origins (`*`)
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS, HEAD, TRACE, CONNECT
- **Allowed Headers**: All headers
- **Credentials**: Enabled
- **Max Age**: 1 hour

This means your React frontend can run on any port (e.g., `http://localhost:5173`) and still access the Spring Boot server running on `http://localhost:8080` without CORS errors.

## Dependencies

- **Spring Boot 3.2.0**: Web framework and dependency injection
- **Aerospike Java Client 9.1.0**: For key-value and secondary index operations (JDK 21 version)
- **Jackson**: For JSON processing

## Data Loading Usage Examples

### Load All Data from Directory
```bash
# Load all products from the retail demo data directory
curl -X POST "http://localhost:8080/rest/v1/data/load?dataPath=/path/to/retail-demo/data"
```

Expected response:
```json
{
  "success": true,
  "totalFiles": 44446,
  "successCount": 44000,
  "errorCount": 446,
  "successRate": 98.96,
  "message": "Data loading completed"
}
```

### Load Single Product
```bash
# Load a specific product file
curl -X POST "http://localhost:8080/rest/v1/data/load-single?filePath=/path/to/data/styles/1001.json"
```

### Check Data Loading Status
```bash
# Get product count
curl "http://localhost:8080/rest/v1/data/count"

# Health check
curl "http://localhost:8080/rest/v1/data/health"
```

### Create Secondary Indexes
```bash
# Create all required secondary indexes
curl -X POST "http://localhost:8080/rest/v1/data/create-indexes"
```

Expected response:
```json
{
  "success": true,
  "totalIndexes": 4,
  "successCount": 4,
  "errorCount": 0,
  "indexResults": {
    "cat_idx": "Created successfully",
    "subCat_idx": "Created successfully", 
    "usage_idx": "Created successfully",
    "brand_idx": "Created successfully"
  },
  "message": "Secondary index creation completed"
}
```

### Clear All Data (Use with Caution)
```bash
# Clear all product data
curl -X DELETE "http://localhost:8080/rest/v1/data/clear?confirm=yes-delete-all"
```

## Configuration

The application can be configured via `application.yml`:

```yaml
server:
  port: 8080

aerospike:
  host: aerospike-cluster
  port: 3000
```

## Building and Running

### Prerequisites
- Java 21 or higher
- Maven 3.6 or higher
- Aerospike cluster running

### Local Development
```bash
cd spring-server
mvn clean compile
mvn spring-boot:run
```

### Building JAR
```bash
mvn clean package
java -jar target/retail-demo-spring-1.0.0.jar
```

### Docker
```bash
docker build -t retail-demo-spring .
docker run -p 8080:8080 retail-demo-spring
```

## Testing CORS

You can test the CORS configuration using curl or your browser:

```bash
# Test health endpoint
curl -H "Origin: http://localhost:5173" \
     -H "Access-Control-Request-Method: GET" \
     -H "Access-Control-Request-Headers: X-Requested-With" \
     -X OPTIONS \
     http://localhost:8080/rest/v1/health

# Test from browser console
fetch('http://localhost:8080/rest/v1/health')
  .then(response => response.json())
  .then(data => console.log(data));
```

## Key Features

### 1. **Simplified API**
- Focuses on core key-value operations
- Maintains API compatibility with frontend
- Clean and efficient implementation

### 2. **CORS Support**
- Global CORS configuration
- Supports all origins and methods
- No CORS errors when running React frontend separately

### 3. **Service Layer Architecture**
- Clean separation of concerns
- Dependency injection
- Easy to test and maintain

### 4. **Error Handling**
- Comprehensive exception handling
- Graceful degradation
- Proper HTTP status codes

### 5. **Performance**
- Connection pooling for Aerospike
- Efficient secondary index queries
- Optimized data structures

### 6. **Modern Java Features**
- Java 21 LTS for latest performance and features
- Latest Aerospike client (7.0.0) for optimal compatibility
- Spring Boot 3.2.0 for modern web development

## Implementation Notes

### Key-Value Operations
- Direct Aerospike key-value lookups
- Secondary index queries for filtering
- Proper error handling and null checks

### Search Functionality
- Basic text search using secondary indexes
- Simple string matching on product names
- Extensible for more complex search patterns

### API Compatibility
- Maintains same endpoint structure as original
- Returns empty arrays for removed features (related, also_bought)
- Frontend compatibility preserved

### CORS Implementation
- Global configuration via `CorsConfig` class
- Supports preflight requests
- Handles all HTTP methods and headers

## Testing

Run the tests with:
```bash
mvn test
```

## Deployment

The application is designed to run in a Docker environment with Aerospike:

```yaml
# Example docker-compose service
spring-server:
  build: ./spring-server
  ports:
    - "8080:8080"
  depends_on:
    - aerospike-cluster
```

## Frontend Integration

The Spring Boot server can serve the React frontend as static files, providing a single-server deployment.

### Building and Deploying Frontend

1. **Build React App**: From the `website/` directory:
   ```bash
   cd ../website
   yarn build
   ```
   This automatically copies the built files to `spring-server/src/main/resources/static/`

2. **Manual Copy** (if needed):
   ```bash
   cd ../website
   npm run copy-to-spring
   ```

3. **Start Spring Boot**: The server will serve both API and frontend:
   ```bash
   mvn spring-boot:run
   ```

4. **Access Application**: Open http://localhost:8080 to view the complete application

### Development Workflow

- **Frontend Development**: Use `yarn dev` in `website/` (runs on port 5173 with API proxy)
- **Backend Development**: Use `mvn spring-boot:run` in `spring-server/`
- **Production Build**: Use `yarn build` to build and deploy frontend to Spring Boot
- **Full Stack**: Access complete app at http://localhost:8080 after building frontend

### Static File Configuration

The `WebConfig.java` class handles:
- **Static Assets**: CSS, JS, images served with long cache headers
- **SPA Routing**: All non-API routes serve `index.html` for client-side routing
- **API Separation**: Routes starting with `/rest/` are handled by controllers

## Multiple Client Implementations

The application supports two different implementations of the core services that can be switched via Spring profiles:

### Available Implementations

#### **Old Client** (`old-client` profile)
- **Default implementation** with standard Aerospike client configuration
- **Active by default** - stable and proven
- **Use case**: Production environments requiring stability

#### **New Client** (`new-client` profile)  
- **Enhanced implementation** with optimized Aerospike client settings
- **Features**:
  - 5-second timeout (vs default)
  - 100 max connections per node (vs default)
  - 2 connection pools per node (vs default)
- **Use case**: High-performance environments or A/B testing

### Switching Between Implementations

#### **Option 1: Configuration File**
Edit `src/main/resources/application.yml`:
```yaml
spring:
  profiles:
    active: new-client  # or old-client
```

#### **Option 2: Environment Variable**
```bash
export SPRING_PROFILES_ACTIVE=new-client
mvn spring-boot:run
```

#### **Option 3: Command Line**
```bash
mvn spring-boot:run -Dspring.profiles.active=new-client
```

#### **Option 4: JVM Argument**
```bash
java -Dspring.profiles.active=new-client -jar target/retail-demo-spring.jar
```

### Implementation Details

Both implementations provide identical APIs but with different internal configurations:

- **`KeyValueServiceInterface`** - Unified interface for all database operations (products + cart)
- **`KeyValueServiceOldClient`** / **`KeyValueServiceNewClient`** - Different implementations with all database functionality

### **Unified Service Architecture**

The service consolidates all database operations into a single interface:
- **Product Operations**: CRUD, search, categories, brand names
- **Cart Operations**: Add, update, remove, clear cart items
- **Data Loading**: Index creation, product storage (via DataLoadingService)

This simplifies dependency injection and ensures all database calls go through one optimized service.

The controllers automatically use whichever implementation is active based on the profile.

## Limitations

This simplified version does not include:
- **Vector Search**: No cosine similarity search
- **Graph Operations**: No Gremlin graph traversal
- **Text Embedding**: No ML-based text processing
- **Advanced Recommendations**: No "also bought" or similar product recommendations

## Future Enhancements

1. **Advanced Search**: Implement full-text search capabilities
2. **Caching**: Add Redis or in-memory caching
3. **Metrics**: Add Prometheus metrics and monitoring
4. **Health Checks**: Implement comprehensive health checks
5. **Security**: Add authentication and authorization

## Troubleshooting

### Common Issues

1. **Connection Errors**: Check Aerospike server connectivity
2. **Port Conflicts**: Ensure port 8080 is available
3. **Memory Issues**: Adjust JVM heap size if needed
4. **CORS Errors**: Verify the CORS configuration is loaded
5. **Java Version**: Ensure Java 21 is installed and configured

### Logs
The server uses SLF4J with Spring Boot’s Logback binding. Tune Aerospike SDK categories in `src/main/resources/logback-spring.xml` (several named loggers are commented out). You can also set levels in `application.yml`:
```yaml
logging:
  level:
    com.aerospike.client.sdk: DEBUG
    com.aerospike.client.sdk.tend: DEBUG
``` 