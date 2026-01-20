# Java Integration Testing Reference Examples

## Example 1: Complete Repository Test with Testcontainers

```java
package com.example.integration.repository;

import com.example.model.Product;
import com.example.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("productdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("29.99"));
        testProduct.setStockQuantity(100);
    }

    @Test
    @Order(1)
    void shouldSaveProduct_whenValidDataProvided() {
        // Act
        Product savedProduct = productRepository.save(testProduct);

        // Assert
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("Test Product");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(savedProduct.getStockQuantity()).isEqualTo(100);
    }

    @Test
    @Order(2)
    void shouldFindProductById_whenProductExists() {
        // Arrange
        Product savedProduct = productRepository.save(testProduct);

        // Act
        Optional<Product> foundProduct = productRepository.findById(savedProduct.getId());

        // Assert
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo("Test Product");
    }

    @Test
    @Order(3)
    void shouldFindProductsByPriceRange_whenProductsInRange() {
        // Arrange
        productRepository.save(testProduct);

        Product expensiveProduct = new Product();
        expensiveProduct.setName("Expensive Product");
        expensiveProduct.setPrice(new BigDecimal("99.99"));
        productRepository.save(expensiveProduct);

        // Act
        List<Product> productsInRange = productRepository.findByPriceBetween(
                new BigDecimal("20.00"), new BigDecimal("50.00"));

        // Assert
        assertThat(productsInRange).hasSize(1);
        assertThat(productsInRange.get(0).getName()).isEqualTo("Test Product");
    }

    @Test
    @Order(4)
    void shouldUpdateProductStock_whenProductExists() {
        // Arrange
        Product savedProduct = productRepository.save(testProduct);
        Long productId = savedProduct.getId();

        // Act
        savedProduct.setStockQuantity(50);
        productRepository.save(savedProduct);

        // Assert
        Optional<Product> updatedProduct = productRepository.findById(productId);
        assertThat(updatedProduct).isPresent();
        assertThat(updatedProduct.get().getStockQuantity()).isEqualTo(50);
    }

    @Test
    @Order(5)
    void shouldDeleteProduct_whenProductExists() {
        // Arrange
        Product savedProduct = productRepository.save(testProduct);
        Long productId = savedProduct.getId();

        // Act
        productRepository.deleteById(productId);

        // Assert
        Optional<Product> deletedProduct = productRepository.findById(productId);
        assertThat(deletedProduct).isEmpty();
    }
}
```

## Example 2: Full REST API Test with REST Assured

```java
package com.example.integration.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    private static String createdOrderId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "/api";
    }

    @Test
    @Order(1)
    void shouldCreateOrder_whenValidRequestProvided() {
        String requestBody = """
            {
                "customerId": "customer123",
                "items": [
                    {
                        "productId": "prod001",
                        "quantity": 2,
                        "price": 29.99
                    }
                ],
                "totalAmount": 59.98
            }
            """;

        Response response = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/orders")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("customerId", equalTo("customer123"))
            .body("items", hasSize(1))
            .body("totalAmount", equalTo(59.98f))
            .body("status", equalTo("PENDING"))
            .body("id", notNullValue())
        .extract()
            .response();

        createdOrderId = response.path("id");
    }

    @Test
    @Order(2)
    void shouldRetrieveOrder_whenOrderExists() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/orders/" + createdOrderId)
        .then()
            .statusCode(200)
            .body("id", equalTo(createdOrderId))
            .body("customerId", equalTo("customer123"))
            .body("status", equalTo("PENDING"));
    }

    @Test
    @Order(3)
    void shouldUpdateOrderStatus_whenValidStatusProvided() {
        String updateRequest = """
            {
                "status": "CONFIRMED"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .patch("/orders/" + createdOrderId + "/status")
        .then()
            .statusCode(200)
            .body("status", equalTo("CONFIRMED"));
    }

    @Test
    @Order(4)
    void shouldListOrders_whenOrdersExist() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/orders")
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)))
            .body("[0].id", notNullValue())
            .body("[0].customerId", notNullValue());
    }

    @Test
    @Order(5)
    void shouldFilterOrdersByStatus_whenStatusProvided() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("status", "CONFIRMED")
        .when()
            .get("/orders")
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)))
            .body("[0].status", equalTo("CONFIRMED"));
    }

    @Test
    @Order(6)
    void shouldReturnNotFound_whenOrderDoesNotExist() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/orders/99999")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(7)
    void shouldReturnBadRequest_whenInvalidDataProvided() {
        String invalidRequest = """
            {
                "customerId": "",
                "items": [],
                "totalAmount": -10.00
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
        .when()
            .post("/orders")
        .then()
            .statusCode(400);
    }
}
```

## Example 3: Testing with Multiple Containers

```java
package com.example.integration.fullstack;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FullStackIntegrationTest {

    private static Network network = Network.newNetwork();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withNetwork(network)
            .withNetworkAliases("postgres");

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.12-alpine")
            .withNetwork(network)
            .withNetworkAliases("rabbitmq");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // RabbitMQ
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);

        // Redis
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void shouldProcessOrderEndToEnd_withDatabaseMessageQueueAndCache() {
        // Step 1: Create an order (stored in PostgreSQL)
        String orderId = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "customerId": "cust001",
                    "items": [{"productId": "prod001", "quantity": 1, "price": 50.00}],
                    "totalAmount": 50.00
                }
                """)
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .extract().path("id");

        // Step 2: Verify order is cached (Redis)
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/orders/" + orderId)
        .then()
            .statusCode(200)
            .header("X-Cache-Hit", equalTo("true"));

        // Step 3: Verify order event was published (RabbitMQ)
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                given()
                    .contentType(ContentType.JSON)
                .when()
                    .get("/api/events/orders/" + orderId)
                .then()
                    .statusCode(200)
                    .body("eventType", equalTo("ORDER_CREATED"))
                    .body("processed", equalTo(true));
            });

        // Step 4: Update order status
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "status": "PROCESSING"
                }
                """)
        .when()
            .patch("/api/orders/" + orderId + "/status")
        .then()
            .statusCode(200)
            .body("status", equalTo("PROCESSING"));

        // Step 5: Verify cache was invalidated
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/orders/" + orderId)
        .then()
            .statusCode(200)
            .header("X-Cache-Hit", equalTo("false"))
            .body("status", equalTo("PROCESSING"));
    }
}
```

## Example 4: WireMock for External Service Mocking

```java
package com.example.integration.external;

import com.example.client.PaymentGatewayClient;
import com.example.dto.PaymentRequest;
import com.example.dto.PaymentResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PaymentGatewayIntegrationTest {

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(8089)
                .disableRequestJournal()
        );
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("payment.gateway.url", () -> "http://localhost:8089");
    }

    @Autowired
    private PaymentGatewayClient paymentGatewayClient;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }

    @Test
    void shouldProcessPayment_whenGatewayReturnsSuccess() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/payments"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Authorization", matching("Bearer .*"))
            .withRequestBody(matchingJsonPath("$.amount", equalTo("100.00")))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "transactionId": "txn_123456",
                        "status": "SUCCESS",
                        "amount": "100.00",
                        "currency": "USD",
                        "processedAt": "2024-01-20T10:00:00Z"
                    }
                    """)));

        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cardToken("tok_visa_4242")
                .build();

        // Act
        PaymentResponse response = paymentGatewayClient.processPayment(request);

        // Assert
        assertThat(response.getTransactionId()).isEqualTo("txn_123456");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        // Verify the request was made correctly
        verify(postRequestedFor(urlEqualTo("/api/v1/payments"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Authorization", matching("Bearer .*")));
    }

    @Test
    void shouldHandleDeclinedPayment_whenGatewayReturnsFailure() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/payments"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "transactionId": "txn_789012",
                        "status": "DECLINED",
                        "amount": "100.00",
                        "currency": "USD",
                        "errorCode": "INSUFFICIENT_FUNDS",
                        "errorMessage": "Insufficient funds"
                    }
                    """)));

        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cardToken("tok_visa_0002")
                .build();

        // Act
        PaymentResponse response = paymentGatewayClient.processPayment(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo("DECLINED");
        assertThat(response.getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void shouldRetryAndSucceed_whenGatewayInitiallyFails() {
        // Arrange - First call fails, second succeeds
        stubFor(post(urlEqualTo("/api/v1/payments"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(503)
                .withBody("Service temporarily unavailable"))
            .willSetStateTo("First Attempt Failed"));

        stubFor(post(urlEqualTo("/api/v1/payments"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("First Attempt Failed")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "transactionId": "txn_retry_success",
                        "status": "SUCCESS",
                        "amount": "100.00",
                        "currency": "USD"
                    }
                    """)));

        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cardToken("tok_visa_4242")
                .build();

        // Act
        PaymentResponse response = paymentGatewayClient.processPayment(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        verify(exactly(2), postRequestedFor(urlEqualTo("/api/v1/payments")));
    }

    @Test
    void shouldTimeout_whenGatewayResponseIsDelayed() {
        // Arrange
        stubFor(post(urlEqualTo("/api/v1/payments"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(6000)
                .withBody("{}")));

        PaymentRequest request = PaymentRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .cardToken("tok_visa_4242")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> paymentGatewayClient.processPayment(request))
            .hasMessageContaining("timeout");
    }
}
```

These examples demonstrate:
1. Complete repository testing with Testcontainers
2. Full REST API testing with REST Assured and ordered test execution
3. Multi-container integration testing (PostgreSQL, RabbitMQ, Redis)
4. External service mocking with WireMock including retry scenarios and timeout testing
