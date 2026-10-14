package com.navan.commerce.transactions.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiIntegrationTest {

    private static final String DATABASE_NAME = "integration-tests-" + UUID.randomUUID();

    @LocalServerPort
    private int port;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:file:./target/test-data/" + DATABASE_NAME);
        registry.add("spring.jpa.open-in-view", () -> false);
    }

    @BeforeEach
    void cleanDatabase() {
        var tables = jdbc.queryForList("""
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_TYPE = 'BASE TABLE'
                  AND LOWER(TABLE_NAME) <> 'flyway_schema_history'
                """, String.class);

        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            tables.forEach(table -> jdbc.execute("TRUNCATE TABLE \"" + table + "\""));
        } finally {
            jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    protected JsonNode readFixture(String path) throws IOException {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readTree(input);
        }
    }

    protected RequestSpecification request() {
        return given()
                .baseUri("http://localhost")
                .port(port)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }
}
