# Java Integration Testing Skill

## Description
Comprehensive guide for writing effective integration tests in Java projects using modern testing frameworks and best practices.

## When to Use This Skill
- Setting up integration testing infrastructure for Java applications
- Writing tests that verify interactions between components, services, or systems
- Testing database interactions, REST APIs, message queues, and external services
- Implementing test containers for isolated integration testing
- Establishing integration testing patterns and conventions

## Prerequisites
- Java 11+ installed
- Maven or Gradle build tool
- Basic understanding of JUnit 5 and testing concepts
- Familiarity with Spring Framework (for Spring-based examples)

## Core Concepts

### What are Integration Tests?
Integration tests verify that multiple components or systems work together correctly. Unlike unit tests that test individual classes in isolation, integration tests:
- Test interactions between multiple components
- May involve real databases, file systems, or external services
- Run slower than unit tests but provide higher confidence
- Validate end-to-end workflows and business scenarios

### Integration Testing Layers
1. **Component Integration**: Testing interactions between classes/modules
2. **Service Integration**: Testing REST APIs, SOAP services, or RPC calls
3. **Data Integration**: Testing database operations and transactions
4. **System Integration**: Testing with external systems and third-party services

## Framework Selection

### JUnit 5 (Jupiter)
The modern standard for Java testing with enhanced features:
- Parameterized tests
- Dynamic tests
- Nested test classes
- Conditional test execution
- Extension model for custom behavior

### Spring Boot Test
For Spring applications, provides:
- `@SpringBootTest` for full application context
- `@WebMvcTest` for controller layer testing
- `@DataJpaTest` for repository layer testing
- Auto-configuration for test scenarios

### Testcontainers
Docker-based testing for:
- Databases (PostgreSQL, MySQL, MongoDB, etc.)
- Message brokers (Kafka, RabbitMQ)
- Cloud services (LocalStack for AWS)
- Any containerized dependency

### Other Frameworks
- **REST Assured**: For REST API testing
- **WireMock**: For mocking HTTP services
- **Mockito**: For mocking when needed in integration tests
- **Awaitility**: For asynchronous testing

## Project Structure

### Recommended Directory Layout
```
src/
├── main/
│   └── java/
│       └── com/example/
│           ├── controller/
│           ├── service/
│           ├── repository/
│           └── model/
└── test/
    ├── java/
    │   └── com/example/
    │       ├── integration/
    │       │   ├── controller/
    │       │   ├── service/
    │       │   └── repository/
    │       └── unit/
    └── resources/
        ├── application-test.properties
        └── test-data/
```

### Naming Conventions
- Integration test classes: `*IntegrationTest.java` or `*IT.java`
- Test methods: `should_<expected_behavior>_when_<condition>()`
- Example: `shouldReturnUser_whenValidIdProvided()`

## Maven Configuration

### pom.xml Dependencies
```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.1</version>
        <scope>test</scope>
    </dependency>

    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>

    <!-- REST Assured -->
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <version>5.4.0</version>
        <scope>test</scope>
    </dependency>

    <!-- Awaitility for async testing -->
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <version>4.2.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Surefire for unit tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.2</version>
            <configuration>
                <excludes>
                    <exclude>**/*IntegrationTest.java</exclude>
                    <exclude>**/*IT.java</exclude>
                </excludes>
            </configuration>
        </plugin>

        <!-- Failsafe for integration tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.2.2</version>
            <configuration>
                <includes>
                    <include>**/*IntegrationTest.java</include>
                    <include>**/*IT.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Gradle Configuration

### build.gradle
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

dependencies {
    // JUnit 5
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'

    // Spring Boot Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'

    // Testcontainers
    testImplementation 'org.testcontainers:testcontainers:1.19.3'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
    testImplementation 'org.testcontainers:postgresql:1.19.3'

    // REST Assured
    testImplementation 'io.rest-assured:rest-assured:5.4.0'

    // Awaitility
    testImplementation 'org.awaitility:awaitility:4.2.0'
}

test {
    useJUnitPlatform()
    // Exclude integration tests from unit test task
    exclude '**/*IntegrationTest.class'
    exclude '**/*IT.class'
}

task integrationTest(type: Test) {
    useJUnitPlatform()
    // Only run integration tests
    include '**/*IntegrationTest.class'
    include '**/*IT.class'

    shouldRunAfter test
}

check.dependsOn integrationTest
```

## Testing Patterns

### 1. Repository Integration Testing with Testcontainers

```java
package com.example.integration.repository;

import com.example.model.User;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndRetrieveUser_whenValidDataProvided() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        // Act
        User savedUser = userRepository.save(user);
        Optional<User> retrievedUser = userRepository.findById(savedUser.getId());

        // Assert
        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().getUsername()).isEqualTo("testuser");
        assertThat(retrievedUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldFindUserByEmail_whenEmailExists() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("unique@example.com");
        userRepository.save(user);

        // Act
        Optional<User> foundUser = userRepository.findByEmail("unique@example.com");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
    }
}
```

### 2. REST API Integration Testing

```java
package com.example.integration.controller;

import com.example.dto.UserRequest;
import com.example.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserControllerIntegrationTest {

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

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateUser_whenValidRequestProvided() {
        // Arrange
        UserRequest request = new UserRequest("newuser", "new@example.com");
        String url = "http://localhost:" + port + "/api/users";

        // Act
        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                url, request, UserResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("newuser");
        assertThat(response.getBody().getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void shouldReturnBadRequest_whenInvalidEmailProvided() {
        // Arrange
        UserRequest request = new UserRequest("newuser", "invalid-email");
        String url = "http://localhost:" + port + "/api/users";

        // Act
        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                url, request, UserResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
```

### 3. REST Assured for API Testing

```java
package com.example.integration.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserApiIntegrationTest {

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

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void shouldReturnUserList_whenGetRequestToUsersEndpoint() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/users")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasSize(greaterThanOrEqualTo(0)));
    }

    @Test
    void shouldCreateAndRetrieveUser_whenValidDataProvided() {
        // Create user
        String userId = given()
            .contentType(ContentType.JSON)
            .body("{\"username\":\"apiuser\",\"email\":\"api@example.com\"}")
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .body("username", equalTo("apiuser"))
            .body("email", equalTo("api@example.com"))
            .extract().path("id");

        // Retrieve user
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/users/" + userId)
        .then()
            .statusCode(200)
            .body("id", equalTo(userId))
            .body("username", equalTo("apiuser"));
    }
}
```

### 4. Service Layer Integration Testing

```java
package com.example.integration.service;

import com.example.dto.UserRequest;
import com.example.dto.UserResponse;
import com.example.exception.UserAlreadyExistsException;
import com.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Transactional
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserService userService;

    @Test
    void shouldCreateUser_whenValidRequestProvided() {
        // Arrange
        UserRequest request = new UserRequest("serviceuser", "service@example.com");

        // Act
        UserResponse response = userService.createUser(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getUsername()).isEqualTo("serviceuser");
        assertThat(response.getEmail()).isEqualTo("service@example.com");
    }

    @Test
    void shouldThrowException_whenDuplicateEmailProvided() {
        // Arrange
        UserRequest request1 = new UserRequest("user1", "duplicate@example.com");
        UserRequest request2 = new UserRequest("user2", "duplicate@example.com");
        userService.createUser(request1);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request2))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("duplicate@example.com");
    }
}
```

### 5. Message Queue Integration Testing

```java
package com.example.integration.messaging;

import com.example.event.UserCreatedEvent;
import com.example.messaging.UserEventPublisher;
import com.example.messaging.UserEventListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class UserEventIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.12-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    @Autowired
    private UserEventPublisher publisher;

    @SpyBean
    private UserEventListener listener;

    @Test
    void shouldReceiveEvent_whenEventPublished() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent("user123", "testuser", "test@example.com");

        // Act
        publisher.publishUserCreated(event);

        // Assert
        await()
            .atMost(5, SECONDS)
            .untilAsserted(() -> verify(listener).handleUserCreated(any(UserCreatedEvent.class)));
    }
}
```

### 6. External Service Integration with WireMock

```java
package com.example.integration.external;

import com.example.client.PaymentServiceClient;
import com.example.dto.PaymentRequest;
import com.example.dto.PaymentResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentServiceIntegrationTest {

    private static WireMockServer wireMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
        registry.add("payment.service.url", () -> "http://localhost:8089");
    }

    @Autowired
    private PaymentServiceClient paymentServiceClient;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldProcessPayment_whenValidRequestProvided() {
        // Arrange
        stubFor(post(urlEqualTo("/api/payments"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"transactionId\":\"txn123\",\"status\":\"SUCCESS\"}")));

        PaymentRequest request = new PaymentRequest("100.00", "USD", "card123");

        // Act
        PaymentResponse response = paymentServiceClient.processPayment(request);

        // Assert
        assertThat(response.getTransactionId()).isEqualTo("txn123");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");

        verify(postRequestedFor(urlEqualTo("/api/payments"))
            .withHeader("Content-Type", equalTo("application/json")));
    }
}
```

## Best Practices

### 1. Test Isolation
- Each test should be independent and not rely on other tests
- Use `@Transactional` or `@DirtiesContext` to reset state between tests
- Clean up test data after each test or use unique identifiers
- Don't share mutable state between tests

### 2. Test Data Management
```java
@BeforeEach
void setUp() {
    // Set up test data before each test
    testDataBuilder.createDefaultUsers();
}

@AfterEach
void tearDown() {
    // Clean up after each test
    testDataCleaner.clearAllData();
}
```

### 3. Use Test Fixtures and Builders
```java
public class UserTestDataBuilder {
    private String username = "defaultuser";
    private String email = "default@example.com";

    public UserTestDataBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public User build() {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }
}

// Usage in tests
User testUser = new UserTestDataBuilder()
    .withUsername("testuser")
    .withEmail("test@example.com")
    .build();
```

### 4. Shared Test Containers
For faster test execution, reuse containers across tests:

```java
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}

// Extend in your test classes
class UserRepositoryIntegrationTest extends AbstractIntegrationTest {
    // Test implementation
}
```

### 5. Meaningful Assertions
```java
// Good: Specific assertions
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
assertThat(response.getBody()).isNotNull();
assertThat(response.getBody().getId()).isNotNull();

// Avoid: Generic assertions
assertThat(response).isNotNull();
```

### 6. Test Performance
- Run integration tests separately from unit tests
- Use test profiles with minimal logging
- Implement parallel test execution when possible
- Monitor test execution times and optimize slow tests

### 7. Error Scenarios
Always test both success and failure paths:
```java
@Test
void shouldReturnNotFound_whenUserDoesNotExist() {
    // Act
    ResponseEntity<UserResponse> response = restTemplate.getForEntity(
            "/api/users/99999", UserResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
}

@Test
void shouldHandleTimeout_whenExternalServiceSlow() {
    // Arrange
    stubFor(get(urlEqualTo("/api/external"))
        .willReturn(aResponse()
            .withFixedDelay(5000)));

    // Act & Assert
    assertThatThrownBy(() -> externalClient.fetchData())
        .isInstanceOf(TimeoutException.class);
}
```

## Running Integration Tests

### Maven
```bash
# Run unit tests only
mvn test

# Run integration tests only
mvn verify -DskipUnitTests

# Run all tests
mvn verify

# Run specific integration test
mvn verify -Dit.test=UserRepositoryIntegrationTest
```

### Gradle
```bash
# Run unit tests only
./gradlew test

# Run integration tests only
./gradlew integrationTest

# Run all tests
./gradlew check

# Run specific integration test
./gradlew integrationTest --tests UserRepositoryIntegrationTest
```

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  integration-test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Run integration tests
        run: mvn verify

      - name: Publish test results
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Integration Test Results
          path: target/failsafe-reports/*.xml
          reporter: java-junit
```

## Troubleshooting

### Common Issues

1. **Testcontainers not starting**
   - Ensure Docker is running
   - Check Docker permissions
   - Verify port availability

2. **Tests failing intermittently**
   - Check for race conditions
   - Increase timeout values
   - Ensure proper test isolation

3. **Slow test execution**
   - Reuse containers across tests
   - Use lighter database images (alpine)
   - Run tests in parallel
   - Profile and optimize slow tests

4. **Connection refused errors**
   - Verify container is fully started before tests run
   - Check port mappings
   - Ensure correct connection strings

## Advanced Topics

### Testing Transactions
```java
@Test
@Transactional
void shouldRollbackTransaction_whenExceptionThrown() {
    // Arrange
    User user = new User("testuser", "test@example.com");

    // Act & Assert
    assertThatThrownBy(() -> userService.createUserWithError(user))
        .isInstanceOf(CustomException.class);

    // Verify rollback
    assertThat(userRepository.findByEmail("test@example.com")).isEmpty();
}
```

### Testing Async Operations
```java
@Test
void shouldCompleteAsync_whenTaskSubmitted() {
    // Arrange
    String taskId = "task123";

    // Act
    asyncService.processTask(taskId);

    // Assert
    await()
        .atMost(10, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(() -> {
            TaskStatus status = taskRepository.findStatus(taskId);
            assertThat(status).isEqualTo(TaskStatus.COMPLETED);
        });
}
```

### Testing Security
```java
@Test
@WithMockUser(roles = "ADMIN")
void shouldAccessAdminEndpoint_whenUserHasAdminRole() {
    ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/admin/users", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}

@Test
void shouldReturnUnauthorized_whenNoAuthentication() {
    ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/admin/users", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
}
```

## Additional Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [REST Assured Documentation](https://rest-assured.io/)
- [AssertJ Documentation](https://assertj.github.io/doc/)

## Summary

Integration testing in Java requires:
1. Proper separation from unit tests using naming conventions and build configuration
2. Use of Testcontainers for realistic test environments
3. Testing at multiple layers (repository, service, controller, API)
4. Comprehensive coverage of success and failure scenarios
5. Fast, isolated, and maintainable tests
6. Integration with CI/CD pipelines

Follow these patterns and best practices to create a robust integration testing suite that provides confidence in your application's behavior while maintaining reasonable execution times.
