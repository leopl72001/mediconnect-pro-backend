package com.universidad.mediconnect2.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/** Real repository integration against a disposable PostgreSQL 17 instance. */
@Testcontainers
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.postgresql.Driver"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SpecialtyPostgresIT {
    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("mediconnect_integration")
                    .withUsername("test_user")
                    .withPassword("local_integration_only");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired SpecialtyRepository specialties;

    @Test
    void persistsAndQueriesRealPostgresqlRows() {
        String name = "Prueba PostgreSQL " + java.util.UUID.randomUUID();
        Specialty saved = specialties.saveAndFlush(new Specialty(name));
        assertNotNull(saved.getId());
        assertEquals(name, specialties.findById(saved.getId()).orElseThrow().getName());
    }

    @Test
    void databaseEnforcesUniqueSpecialtyNames() {
        String name = "Unique " + java.util.UUID.randomUUID();
        specialties.saveAndFlush(new Specialty(name));
        assertThrows(Exception.class, () -> specialties.saveAndFlush(new Specialty(name)));
    }
}
