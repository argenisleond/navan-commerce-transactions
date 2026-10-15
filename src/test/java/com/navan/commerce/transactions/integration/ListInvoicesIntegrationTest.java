package com.navan.commerce.transactions.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListInvoicesIntegrationTest extends ApiIntegrationTest {

    @Test
    void usesADefaultPageSizeOfFifty() throws Exception {
        var flight = createStoredTransaction("flight.json");
        createInvoices(flight.id(), 51);
        var statistics = resetDatabaseStatistics();

        var response = listInvoices(null, null, HttpStatus.OK);

        assertThat(response.path("items")).hasSize(50);
        assertThat(response.path("limit").intValue()).isEqualTo(50);
        assertThat(response.path("offset").longValue()).isZero();
        assertThat(response.path("has_more").booleanValue()).isTrue();
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(10);
    }

    @Test
    void returnsConsecutiveDatabasePagesWithoutOverlap() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var createdIds = createInvoices(flight.id(), 11);

        assertPage(createdIds, 3, 0, true, 0, 3);
        assertPage(createdIds, 3, 3, true, 3, 6);
        assertPage(createdIds, 3, 6, true, 6, 9);
        assertPage(createdIds, 3, 9, false, 9, 11);
    }

    @Test
    void acceptsTheMaximumPageSize() throws Exception {
        var response = listInvoices(100, null, HttpStatus.OK);

        assertThat(response.path("items")).isEmpty();
        assertThat(response.path("limit").intValue()).isEqualTo(100);
        assertThat(response.path("has_more").booleanValue()).isFalse();
    }

    @Test
    void rejectsALimitAboveOneHundred() throws Exception {
        listInvoices(1000, null, HttpStatus.BAD_REQUEST);
    }

    private List<String> createInvoices(String transactionId, int count) throws Exception {
        var ids = new ArrayList<String>();
        for (var index = 0; index < count; index++) {
            ids.add(createSingleTransactionInvoice(transactionId).path("id").textValue());
        }
        return ids;
    }

    private void assertPage(List<String> createdIds, int limit, long offset,
                            boolean hasMore, int fromIndex, int toIndex) throws Exception {
        var response = listInvoices(limit, offset, HttpStatus.OK);
        assertThat(response.path("items").findValuesAsText("id"))
                .containsExactlyElementsOf(createdIds.subList(fromIndex, toIndex));
        assertThat(response.path("limit").intValue()).isEqualTo(limit);
        assertThat(response.path("offset").longValue()).isEqualTo(offset);
        assertThat(response.path("has_more").booleanValue()).isEqualTo(hasMore);
    }

    private com.fasterxml.jackson.databind.JsonNode listInvoices(
            Integer limit, Long offset, HttpStatus expectedStatus) throws Exception {
        var specification = request();
        if (limit != null) {
            specification.queryParam("limit", limit);
        }
        if (offset != null) {
            specification.queryParam("offset", offset);
        }
        var body = specification.get("/invoices")
                .then()
                .log().ifValidationFails()
                .statusCode(expectedStatus.value())
                .extract().asString();
        return body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
    }
}
