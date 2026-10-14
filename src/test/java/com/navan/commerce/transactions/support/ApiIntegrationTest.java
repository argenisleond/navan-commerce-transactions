package com.navan.commerce.transactions.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.io.IOException;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(
        scripts = "/db/cleanup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
public abstract class ApiIntegrationTest {

    private static final String DATABASE_NAME = "integration-tests-" + UUID.randomUUID();

    @LocalServerPort
    private int port;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:file:./target/test-data/" + DATABASE_NAME);
        registry.add("spring.jpa.open-in-view", () -> false);
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
