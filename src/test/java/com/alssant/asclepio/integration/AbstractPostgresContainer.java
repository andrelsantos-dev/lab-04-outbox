package com.alssant.asclepio.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

public class AbstractPostgresContainer {
    protected static final String APP_USER = "app_user";
    protected static final String MIGRATION_USER = "migration_user";

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("dbRLSTest")
                    .withInitScript("db/init/00-test-init.sql")
                    // For testing purposes only
                    .withCommand(
                            "postgres",
                            "-c", "fsync=off",                // Turns off disk synchronization; drastically speeds up writes
                            "-c", "synchronous_commit=off",   // Does not wait for WAL disk writes before returning success
                            "-c", "full_page_writes=off",     // Disables page recovery protection (unnecessary for testing)
                            "-c", "shared_buffers=256MB",     // Increases dedicated RAM cache for database pages
                            "-c", "work_mem=32MB"             // Allocates more RAM for sorting and complex queries/joins
                    );

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = POSTGRES.getJdbcUrl();

        registry.add("spring.flyway.url", () -> jdbcUrl);
        registry.add("spring.flyway.user", () -> MIGRATION_USER);
        registry.add("spring.flyway.password", () -> "migration_password");

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> APP_USER);
        registry.add("spring.datasource.password", () -> "app_password");
    }
}
