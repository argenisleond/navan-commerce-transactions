package com.navan.commerce.transactions.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.navan.commerce.transactions.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateTransactionIntegrationTest extends ApiIntegrationTest {

    private static final String FLIGHT_FIXTURE = "fixtures/transactions/flight.json";
    private static final String FIXTURE_PATH = "fixtures/transactions/";
    private static final String PATCH_PATH = FIXTURE_PATH + "patches/";

    @Test
    void partiallyUpdatesATransactionWithoutClearingOmittedFields() throws Exception {
        var original = readFixture(FLIGHT_FIXTURE);
        var id = createTransaction(original);
        var requestBody = objectMapper.createObjectNode().put("total", 225.50);

        var updated = patchTransaction(id, requestBody, HttpStatus.OK);

        assertThat(updated.path("id").textValue()).isEqualTo(id);
        assertThat(updated.path("total").decimalValue()).isEqualByComparingTo("225.50");
        assertThat(updated.path("metadata")).isEqualTo(original.path("metadata"));
        assertThat(updated.path("tax_lines")).isEqualTo(original.path("tax_lines"));
        assertThat(updated.path("fee_lines")).isEqualTo(original.path("fee_lines"));
    }

    @ParameterizedTest
    @CsvSource({
            "flight.json, flight.json",
            "hotel.json, hotel.json",
            "rail.json, rail.json",
            "trip-fee.json, navan-fee.json"
    })
    void replacesMutableFieldsAndNestedCollections(String originalFixture,
                                                   String patchFixture) throws Exception {
        var original = readFixture(FIXTURE_PATH + originalFixture);
        var patch = readFixture(PATCH_PATH + patchFixture);
        var expected = merge(original, patch);
        var id = createTransaction(original);

        var patchResponse = patchTransaction(id, patch, HttpStatus.OK);
        var getResponse = getTransaction(id, HttpStatus.OK);

        assertThat(patchResponse.path("id").textValue()).isEqualTo(id);
        JSONAssert.assertEquals(expected.toString(), patchResponse.toString(),
                JSONCompareMode.STRICT_ORDER);
        JSONAssert.assertEquals(expected.toString(), getResponse.toString(),
                JSONCompareMode.STRICT_ORDER);
    }

    @Test
    void rejectsAnInvalidPatchWithoutPersistingAnyChanges() throws Exception {
        var original = readFixture(FLIGHT_FIXTURE);
        var id = createTransaction(original);
        var invalidPatch = objectMapper.createObjectNode();
        invalidPatch.put("occurred_at", "2026-04-01T10:00:00Z");
        invalidPatch.put("total", 225.501);

        patchTransaction(id, invalidPatch, HttpStatus.BAD_REQUEST);

        var stored = getTransaction(id, HttpStatus.OK);
        JSONAssert.assertEquals(original.toString(), stored.toString(),
                JSONCompareMode.STRICT_ORDER);
    }

    @Test
    void returnsNotFoundWhenUpdatingAnUnknownTransaction() throws Exception {
        var id = UUID.randomUUID().toString();

        var response = patchTransaction(id, readFixture(PATCH_PATH + "flight.json"),
                HttpStatus.NOT_FOUND);

        assertThat(response.path("message").textValue())
                .isEqualTo("Transaction not found for id " + id);
    }

    @Test
    @Timeout(30)
    void concurrentUpdatesNeverMixNestedCollections() throws Exception {
        var id = createTransaction(readFixture(FLIGHT_FIXTURE));
        var original = readFixture(FLIGHT_FIXTURE);
        var firstPatch = readFixture(PATCH_PATH + "flight.json");
        var secondPatch = readFixture(PATCH_PATH + "flight-alternative.json");
        var ready = new CountDownLatch(10);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(10);

        try {
            var tasks = IntStream.range(0, 10)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return request()
                                .body(index % 2 == 0
                                        ? firstPatch.toString()
                                        : secondPatch.toString())
                                .patch("/transactions/{id}", id)
                                .statusCode();
                    }))
                    .toList();

            ready.await();
            start.countDown();

            var statuses = tasks.stream().map(task -> {
                try {
                    return task.get();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }).toList();
            assertThat(statuses).containsOnly(HttpStatus.OK.value());

            var stored = getTransaction(id, HttpStatus.OK);
            var firstExpected = merge(original, firstPatch);
            var secondExpected = merge(original, secondPatch);
            assertThat(matches(stored, firstExpected) || matches(stored, secondExpected)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    private String createTransaction(JsonNode requestBody) {
        return request()
                .body(requestBody.toString())
                .post("/transactions")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().path("id");
    }

    private JsonNode patchTransaction(String id, JsonNode requestBody, HttpStatus status)
            throws Exception {
        var response = request()
                .body(requestBody.toString())
                .patch("/transactions/{id}", id)
                .then()
                .log().ifValidationFails()
                .statusCode(status.value())
                .extract().asString();
        return response.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response);
    }

    private JsonNode getTransaction(String id, HttpStatus status) throws Exception {
        var response = request()
                .get("/transactions/{id}", id)
                .then()
                .log().ifValidationFails()
                .statusCode(status.value())
                .extract().asString();
        return response.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response);
    }

    private boolean matches(JsonNode actual, JsonNode expected) throws Exception {
        return JSONCompare.compareJSON(expected.toString(), actual.toString(),
                JSONCompareMode.STRICT_ORDER).passed();
    }

    private ObjectNode merge(JsonNode original, JsonNode patch) {
        var expected = (ObjectNode) original.deepCopy();
        patch.properties().forEach(entry -> expected.set(entry.getKey(), entry.getValue()));
        return expected;
    }
}
