package com.navan.commerce.transactions.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.navan.commerce.transactions.support.ApiIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.util.List;

@Sql(
        scripts = "/db/invoice-cleanup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
abstract class InvoiceIntegrationTestSupport extends ApiIntegrationTest {

    protected static final String TRANSACTION_FIXTURE_PATH = "fixtures/transactions/";

    protected TripTransactions createTripTransactions() throws Exception {
        return new TripTransactions(
                createTransaction("flight.json"),
                createTransaction("hotel.json"),
                createTransaction("rail.json"),
                createTransaction("trip-fee.json"),
                createTransaction("agent-call-fee.json")
        );
    }

    protected CreatedTransaction createTransaction(String fixtureName) throws Exception {
        return createTransaction(readFixture(TRANSACTION_FIXTURE_PATH + fixtureName));
    }

    protected CreatedTransaction createTransaction(JsonNode body) throws Exception {
        var response = request()
                .body(body.toString())
                .post("/transactions")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().response();
        var responseBody = objectMapper.readTree(response.asString());
        return new CreatedTransaction(responseBody.path("id").textValue(), body);
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
        List<String> ids() {
            return List.of(flight.id(), hotel.id(), rail.id(), tripFee.id(), agentCallFee.id());
        }
    }
}
