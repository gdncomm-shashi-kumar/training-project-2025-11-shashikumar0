# Product Service

A robust, production-ready Spring Boot microservice for managing e-commerce products and variants. This service handles product creation, retrieval, searching, and internal SKU lookups, following strict industry standards.

## 🚀 Features

-   **Product Management**: Create, retrieve, and search products.
-   **Variant Support**: Products support multiple variants (size, color) with unique SKUs.
-   **Industry Standard Architecture**:
    -   **Layered Architecture**: Controller, Service, Repository, Model.
    -   **DTO Pattern**: strict separation between API contracts (`Request`/`Response` DTOs) and Database Entities.
    -   **Mapper Pattern**: Dedicated mappers for object conversion.
-   **Performance**:
    -   **Caching**: In-memory caching using **Caffeine** for high-performance lookups.
-   **Reliability & Observability**:
    -   **Global Exception Handling**: Standardized error responses with `GdnResponseData` wrapper.
    -   **Validation**: Request payload validation using Jakarta Validation.
    -   **Logging**: Comprehensive SLF4J logging.
    -   **Actuator**: Health checks and metrics.
-   **Documentation**:
    -   **OpenAPI / Swagger UI**: Interactive API documentation.
-   **Containerization**: Docker support included.

## 🛠️ Tech Stack

-   **Java**: 21
-   **Framework**: Spring Boot 3.2.0
-   **Database**: MongoDB
-   **Build Tool**: Maven
-   **Documentation**: SpringDoc OpenAPI (Swagger)
-   **Caching**: Caffeine
-   **Utilities**: Lombok, JavaFaker (for data seeding)

## 📦 API Documentation

Once the application is running, access the interactive Swagger UI documentation at:

```
http://localhost:8083/swagger-ui.html
```

### Key Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/products` | Create a new product (Requires `X-User-Role: ROLE_ADMIN`) |
| `GET` | `/api/v1/products/{id}` | Get product details by ID |
| `GET` | `/api/v1/products` | Search products by name and category (Pagination supported) |
| `GET` | `/api/v1/internal/products/sku/{sku}` | **Internal**: Lookup product by Variant SKU |

## 🏃‍♂️ Getting Started

### Prerequisites

-   Java 21
-   Maven
-   MongoDB (running on `localhost:27017`)

### Installation

1.  **Clone the repository**
    ```bash
    git clone <repository-url>
    cd productService
    ```

2.  **Build the project**
    ```bash
    ./mvnw clean install
    ```

3.  **Run the application**
    ```bash
    ./mvnw spring-boot:run
    ```

The application will start on port **8083**.

### Data Seeding

On the first run, the application will automatically seed **50,000** mock products into the MongoDB database for testing purposes.

## 🐳 Docker Support

1.  **Build the Docker image**
    ```bash
    docker build -t product-service .
    ```

2.  **Run the container**
    ```bash
    docker run -p 8083:8083 product-service
    ```

## 📂 Project Structure

```
src/main/java/com/blibli/gdn/productService
├── config          # Configuration classes (OpenAPI, DataSeeder)
├── controller      # REST Controllers (Public & Internal)
├── dto             # Data Transfer Objects
│   ├── request     # Request payloads
│   └── response    # Response payloads
├── exception       # Global Exception Handling
├── mapper          # Entity <-> DTO Mappers
├── model           # MongoDB Entities (Product, Variant)
├── repository      # MongoDB Repositories
└── service         # Business Logic Interfaces & Implementations
```

## 🧪 Testing

Run unit and integration tests using Maven:

```bash
./mvnw test
```
