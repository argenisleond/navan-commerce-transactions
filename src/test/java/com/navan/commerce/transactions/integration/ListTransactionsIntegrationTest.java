package com.navan.commerce.transactions.integration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ListTransactionsIntegrationTest extends ApiIntegrationTest {

    private static final String FLIGHT_FIXTURE = "fixtures/transactions/flight.json";

    @Test
    void usesADefaultPageSizeOfFifty() throws Exception {
        createTransactions(51);

        var responseBody = objectMapper.readTree(request()
                .get("/transactions")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .extract().asString());

        assertThat(responseBody.path("items")).hasSize(50);
        assertThat(responseBody.path("limit").intValue()).isEqualTo(50);
        assertThat(responseBody.path("offset").intValue()).isZero();
        assertThat(responseBody.path("has_more").booleanValue()).isTrue();
    }

    @Test
    void returnsConsecutiveDatabasePagesWithoutOverlap() throws Exception {
        var createdIds = createTransactions(11);

        assertPage(createdIds, 3, 0, true, 0, 3);
        assertPage(createdIds, 3, 3, true, 3, 6);
        assertPage(createdIds, 3, 6, true, 6, 9);
        assertPage(createdIds, 3, 9, false, 9, 11);
    }

    @Test
    void acceptsTheMaximumPageSize() throws Exception {
        var responseBody = objectMapper.readTree(request()
                .queryParam("limit", 100)
                .get("/transactions")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .extract().asString());

        assertThat(responseBody.path("items")).isEmpty();
        assertThat(responseBody.path("limit").intValue()).isEqualTo(100);
        assertThat(responseBody.path("has_more").booleanValue()).isFalse();
    }

    @Test
    void rejectsALimitAboveOneHundred() {
        request()
                .queryParam("limit", 1000)
                .get("/transactions")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    private List<String> createTransactions(int count) throws Exception {
        var template = (ObjectNode) readFixture(FLIGHT_FIXTURE);
        return IntStream.range(0, count).mapToObj(index -> {
            var requestBody = template.deepCopy();
            requestBody.put("external_id", "list-flight-" + index);
            return request()
                    .body(requestBody.toString())
                    .post("/transactions")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .extract().<String>path("id");
        }).toList();
    }

    private void assertPage(List<String> createdIds, int limit, int offset,
                            boolean hasMore, int fromIndex, int toIndex) throws Exception {
        var responseBody = objectMapper.readTree(request()
                .queryParam("limit", limit)
                .queryParam("offset", offset)
                .get("/transactions")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.OK.value())
                .extract().asString());

        assertThat(responseBody.path("items").findValuesAsText("id"))
                .containsExactlyElementsOf(createdIds.subList(fromIndex, toIndex));
        assertThat(responseBody.path("limit").intValue()).isEqualTo(limit);
        assertThat(responseBody.path("offset").intValue()).isEqualTo(offset);
        assertThat(responseBody.path("has_more").booleanValue()).isEqualTo(hasMore);
    }
}
