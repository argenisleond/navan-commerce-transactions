package com.navan.commerce.transactions.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(
        scripts = "/db/cleanup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
public abstract class ApiIntegrationTest {

    protected static final String TRANSACTION_FIXTURE_PATH = "fixtures/transactions/";

    private static final String DATABASE_NAME = "integration-tests-" + UUID.randomUUID();

    @LocalServerPort
    private int port;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:file:./target/test-data/" + DATABASE_NAME);
        registry.add("spring.jpa.open-in-view", () -> false);
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> true);
        registry.add("spring.jpa.properties.hibernate.session.events.log", () -> false);
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

    protected Statistics resetDatabaseStatistics() {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }

    protected CreatedTransaction createStoredTransaction(String fixtureName) throws Exception {
        return createStoredTransaction(readFixture(TRANSACTION_FIXTURE_PATH + fixtureName));
    }

    protected CreatedTransaction createStoredTransaction(JsonNode body) throws Exception {
        var response = request()
                .body(body.toString())
                .post("/transactions")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().response();
        var responseBody = objectMapper.readTree(response.asString());
        return new CreatedTransaction(responseBody.path("id").textValue(), body);
    }

    protected TripTransactions createTripTransactions() throws Exception {
        return new TripTransactions(
                createStoredTransaction("flight.json"),
                createStoredTransaction("hotel.json"),
                createStoredTransaction("rail.json"),
                createStoredTransaction("trip-fee.json"),
                createStoredTransaction("agent-call-fee.json")
        );
    }

    protected JsonNode createInvoice(List<String> transactionIds) throws Exception {
        return writeInvoice("POST", null, multipleTransactionBody(transactionIds),
                HttpStatus.CREATED);
    }

    protected JsonNode createSingleTransactionInvoice(String transactionId) throws Exception {
        var body = objectMapper.createObjectNode().put("transaction_id", transactionId);
        return writeInvoice("POST", null, body, HttpStatus.CREATED);
    }

    protected JsonNode patchInvoice(String invoiceId, List<String> transactionIds,
                                    HttpStatus expectedStatus) throws Exception {
        return writeInvoice("PATCH", invoiceId, multipleTransactionBody(transactionIds),
                expectedStatus);
    }

    protected JsonNode postInvoice(JsonNode body, HttpStatus expectedStatus) throws Exception {
        return writeInvoice("POST", null, body, expectedStatus);
    }

    protected JsonNode getInvoice(String invoiceId, HttpStatus expectedStatus) throws Exception {
        var response = request()
                .get("/invoices/{id}", invoiceId)
                .then()
                .log().ifValidationFails()
                .statusCode(expectedStatus.value())
                .extract().asString();
        return response.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response);
    }

    protected ObjectNode multipleTransactionBody(List<String> transactionIds) {
        var body = objectMapper.createObjectNode();
        var ids = body.putArray("transaction_ids");
        transactionIds.forEach(ids::add);
        return body;
    }

    protected List<String> lineTransactionIds(JsonNode invoice) {
        return invoice.path("lines").findValuesAsText("transaction_id");
    }

    protected JsonNode findLine(JsonNode invoice, String transactionId) {
        for (var line : invoice.path("lines")) {
            if (transactionId.equals(line.path("transaction_id").textValue())) {
                return line;
            }
        }
        throw new AssertionError("Invoice line not found for transaction " + transactionId);
    }

    private JsonNode writeInvoice(String method, String invoiceId, JsonNode body,
                                  HttpStatus expectedStatus) throws Exception {
        var specification = request().body(body.toString());
        var response = switch (method) {
            case "POST" -> specification.post("/invoices");
            case "PATCH" -> specification.patch("/invoices/{id}", invoiceId);
            default -> throw new IllegalArgumentException("Unsupported method " + method);
        };
        var responseBody = response.then()
                .log().ifValidationFails()
                .statusCode(expectedStatus.value())
                .extract().asString();
        return responseBody.isBlank()
                ? objectMapper.createObjectNode()
                : objectMapper.readTree(responseBody);
    }

    protected record CreatedTransaction(String id, JsonNode requestBody) {
    }

    protected record TripTransactions(
            CreatedTransaction flight,
            CreatedTransaction hotel,
            CreatedTransaction rail,
            CreatedTransaction tripFee,
            CreatedTransaction agentCallFee
    ) {
        public List<String> ids() {
            return List.of(flight.id(), hotel.id(), rail.id(), tripFee.id(), agentCallFee.id());
        }
    }
}
