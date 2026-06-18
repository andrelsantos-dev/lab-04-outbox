package com.alssant.asclepio.support;

import com.alssant.asclepio.tenant.TenantContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.function.Supplier;

@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {
    protected static final String APP_USER = "app_user";
    protected static final String MIGRATION_USER = "migration_user";
    protected static final String TENANT_A = "11111111-1111-1111-1111-111111111111";
    protected static final String TENANT_B = "22222222-2222-2222-2222-222222222222";

    @Container
    @SuppressWarnings("resource")//managed by testcontainers
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("dbRLSTest")
            .withInitScript("db/init/00-test-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = postgres.getJdbcUrl();

        registry.add("spring.flyway.url", () -> jdbcUrl);
        registry.add("spring.flyway.user", () -> MIGRATION_USER);
        registry.add("spring.flyway.password", () -> "migration_password");

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> APP_USER);
        registry.add("spring.datasource.password", () -> "app_password");
    }

    protected  <T> T executeAsTenant(String tenantId, Supplier<T> action) {
        try {
            TenantContext.setTenantId(tenantId);
            return action.get();
        } finally {
            TenantContext.clear();
        }
    }

}
