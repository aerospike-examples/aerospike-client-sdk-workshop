# Aerospike Retail Demo

A demo retail website powered by Aerospike, showcasing Key-Value operations with a modern Spring Boot + React architecture.

## Project Structure

```
retail-demo/
├── spring-server/          # Spring Boot backend (Java 21)
├── website/                # React frontend (Vite)
├── external_jars/          # External JAR files (tracked in Git)
├── data/                   # Sample data files
├── config/                 # Configuration files
  ├── aerospike/              # Aerospike configuration
└── .gitignore              # Multi-module gitignore
```

## Technologies

- **Backend**: Spring Boot 3.2, Java 21, Aerospike Client 9.1.0
- **Frontend**: React 18, Vite 5, React Router DOM
- **Database**: Aerospike (Key-Value operations, Secondary Indexes)
- **Build**: Maven (Java), npm/yarn (React)

To run locally:

1. Download the [kaggle fashion dataset](https://www.kaggle.com/datasets/paramaggarwal/fashion-product-images-dataset)
    1. Place the contents of the `/images/` directory in the `data/images/` directory.
    2. Place the contents of the `/styles/` directory in the `data/styles/` directory.

2. Obtain the new [Fluent Aerospike client library](https://github.com/aerospike-community/aerospike-fluent-client-java)
    Clone this repository to your local machine:
    ```bash
    git clone https://github.com/aerospike-community/aerospike-fluent-client-java
    cd aerospike-fluent-client-java
    mvn clean install
    ```


2. Building the Java application
    First, the `external_jars` must be installed into the local Maven
    ```bash
    cd external_jars
    ./registerJars.sh
    ```
    Note: if you encounter issues using the jar you can reference the JAR file directly from the pow.xml
    ```xml
    <!-- Fluent Aerospike Java Client -->
    <dependency>
        <groupId>com.aerospike</groupId>
        <artifactId>aerospike-fluent-client</artifactId>
        <version>${aerospike-fluent.version}</version>
        <scope>system</scope>
        <systemPath>${project.basedir}/../external_jars/aerospike-fluent-client-0.8.0-jar-with-dependencies.jar</systemPath>
    </dependency>
    ```

3. __OPTIONAL__ Building the front end (should be built by default, so only do this step if /spring-server/src/main/resources/static/index.html does not exist)
    ```bash
    cd website
    npm install
    npm run build
    ```
    
4. Build the application: 
    ```bash
    cd spring-server
    mvn clean package -DskipTests
    ```

5. Run the application
    Once this is done, start the Java application against your database (ensure you're still in the spring-server directory)
    ```bash
    java -jar target/retail-demo-spring-1.0.0.jar
    ```

    If you have Aerospike running on a different port (default `3000`) or host (default `localhost`) then you can specify these arguments:
    ```bash
    java -jar target/retail-demo-spring-1.0.0.jar --aerospike.host=10.0.0.1 --aerospike.port=3100
    ```

6. Create the indexes and load the product data into Aerospike:
    ```
    cd data
    curl -X POST "http://localhost:8080/rest/v1/data/create-indexes"
    curl -X POST "http://localhost:8080/rest/v1/data/load?dataPath=`pwd`"
    ```

7. Point a browser at `localhost:8080` and you should be good to go! 
