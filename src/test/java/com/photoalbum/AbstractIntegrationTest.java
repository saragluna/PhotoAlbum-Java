package com.photoalbum;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests using Testcontainers with PostgreSQL.
 * This class sets up a PostgreSQL container that will be shared across all integration tests.
 */
@SpringBootTest
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> postgresContainer;

    static {
        postgresContainer = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
        postgresContainer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }
}
