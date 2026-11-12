package com.universidad.mediconnect2.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cloud-readiness test: run the exact Flyway production schema on a fresh real PostgreSQL.
 * Hibernate validate must match V1__initial_schema.sql.
 */
@Testcontainers
@DataJpaTest(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlywayPostgresIT {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
        .withDatabaseName("mediconnect_flyway")
        .withUsername("integration")
        .withPassword("local_integration_only");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired SpecialtyRepository specialties;

    @Test
    void flywayInitializesEveryBusinessTableAndHibernateAcceptsTheSchema() {
        Integer count = jdbc.queryForObject(
            "select count(*) from information_schema.tables where table_schema='public' and table_name in " +
            "('app_users','specialties','doctors','offices','appointments','patient_profiles','prescriptions','prescription_items')",
            Integer.class);
        assertEquals(8, count);
        assertEquals(1L, jdbc.queryForObject("select count(*) from flyway_schema_history", Long.class));
        Specialty saved = specialties.saveAndFlush(new Specialty("Nueva especialidad"));
        assertNotNull(saved.getId());
    }
}
