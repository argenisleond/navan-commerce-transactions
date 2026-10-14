package com.navan.commerce.transactions.transaction;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.navan.commerce.transactions.transaction.domain.TransactionRepository;
import com.navan.commerce.transactions.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidTransactionRequestIntegrationTest extends ApiIntegrationTest {

    private static final String FIXTURE_PATH = "fixtures/transactions/";

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void rejectsAnUnknownTransactionType() throws Exception {
        var requestBody = flightRequest();
        requestBody.put("type", "car_rental");

        assertRejected(requestBody);
    }

    @Test
    void rejectsAnUnknownNavanFeeKind() throws Exception {
        var requestBody = (ObjectNode) readFixture(FIXTURE_PATH + "trip-fee.json");
        ((ObjectNode) requestBody.path("metadata")).put("fee_kind", "service_fee");

        assertRejected(requestBody);
    }

    @Test
    void rejectsMetadataThatDoesNotMatchTheTransactionType() throws Exception {
        var requestBody = (ObjectNode) readFixture(FIXTURE_PATH + "hotel.json");
        requestBody.put("type", "flight");

        assertRejected(requestBody);
    }

    @Test
    void rejectsMissingRequiredTaxAndFeeLists() throws Exception {
        var requestBody = flightRequest();
        requestBody.remove("tax_lines");
        requestBody.remove("fee_lines");

        assertRejected(requestBody);
    }

    @Test
    void rejectsTextThatExceedsTheDatabaseLimit() throws Exception {
        var requestBody = flightRequest();
        requestBody.put("external_id", "x".repeat(256));

        assertRejected(requestBody);
    }

    @Test
    void rejectsMoneyBeyondTheDocumentedScale() throws Exception {
        var requestBody = flightRequest();
        requestBody.put("total", 220.001);

        assertRejected(requestBody);
    }

    private ObjectNode flightRequest() throws Exception {
        return (ObjectNode) readFixture(FIXTURE_PATH + "flight.json");
    }

    private void assertRejected(ObjectNode requestBody) {
        request()
                .body(requestBody.toString())
                .post("/transactions")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());

        assertThat(transactionRepository.count()).isZero();
    }
}
