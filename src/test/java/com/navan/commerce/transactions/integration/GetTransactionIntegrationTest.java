package com.navan.commerce.transactions.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetTransactionIntegrationTest extends ApiIntegrationTest {

    private static final String FIXTURE_PATH = "fixtures/transactions/";

    @ParameterizedTest
    @ValueSource(strings = {
            "flight.json",
            "hotel.json",
            "rail.json",
            "trip-fee.json",
            "agent-call-fee.json"
    })
    void returnsTheCompleteStoredTransaction(String fixtureName) throws Exception {
        var requestBody = readFixture(FIXTURE_PATH + fixtureName);
        var id = createTransaction(requestBody);

        var response = request()
                .get("/transactions/{id}", id)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .extract().response();

        var responseBody = objectMapper.readTree(response.asString());
        assertThat(responseBody.path("id").textValue()).isEqualTo(id);
        JSONAssert.assertEquals(requestBody.toString(), responseBody.toString(),
                JSONCompareMode.STRICT_ORDER);
    }

    @Test
    void returnsNotFoundForAnUnknownTransaction() throws Exception {
        var id = UUID.randomUUID();

        var responseBody = objectMapper.readTree(request()
                .get("/transactions/{id}", id)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().asString());

        assertThat(responseBody.path("message").textValue())
                .isEqualTo("Transaction not found for id " + id);
    }

    private String createTransaction(JsonNode requestBody) {
        return request()
                .body(requestBody.toString())
                .post("/transactions")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().path("id");
    }
}
