package com.navan.commerce.transactions.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.navan.commerce.transactions.domain.TransactionRepository;
import com.navan.commerce.transactions.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateTransactionIntegrationTest extends ApiIntegrationTest {

    private static final String FLIGHT_FIXTURE = "fixtures/transactions/flight.json";
    private static final int CONCURRENT_REQUEST_COUNT = 15;
    private static final String DUPLICATE_MESSAGE =
            "Transaction already exists for external_id flight-TXL-LHR-2026-03-11";

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void rejectsARepeatedExternalIdAndReturnsTheExistingId() throws Exception {
        var requestBody = readFixture(FLIGHT_FIXTURE);

        var created = postTransaction(requestBody);
        var duplicate = postTransaction(requestBody);

        assertThat(created.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(duplicate.statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(duplicate.body().path("id").textValue())
                .isEqualTo(created.body().path("id").textValue());
        assertThat(duplicate.body().path("message").textValue()).isEqualTo(DUPLICATE_MESSAGE);
        assertThat(transactionRepository.count()).isOne();
    }

    @Test
    @Timeout(30)
    void concurrentCreatesWithTheSameExternalIdStoreOneTransaction() throws Exception {
        var requestBody = readFixture(FLIGHT_FIXTURE);
        var ready = new CountDownLatch(CONCURRENT_REQUEST_COUNT);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(CONCURRENT_REQUEST_COUNT);

        try {
            var tasks = IntStream.range(0, CONCURRENT_REQUEST_COUNT)
                    .mapToObj(ignored -> executor.submit(
                            () -> postWhenReleased(requestBody, ready, start)))
                    .toList();

            ready.await();
            start.countDown();

            var responses = tasks.stream().map(task -> {
                try {
                    return task.get();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }).toList();

            assertThat(responses)
                    .filteredOn(result -> result.statusCode() == HttpStatus.CREATED.value())
                    .hasSize(1);
            var conflicts = responses.stream()
                    .filter(result -> result.statusCode() == HttpStatus.CONFLICT.value())
                    .toList();
            assertThat(conflicts).hasSize(CONCURRENT_REQUEST_COUNT - 1);
            assertThat(conflicts)
                    .extracting(result -> result.body().path("message").textValue())
                    .containsOnly(DUPLICATE_MESSAGE);
            var responseIds = responses.stream()
                    .map(result -> result.body().path("id").textValue())
                    .collect(Collectors.toSet());
            assertThat(responseIds).hasSize(1).doesNotContainNull();
            assertThat(transactionRepository.count()).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    private CreateResult postWhenReleased(JsonNode requestBody, CountDownLatch ready,
                                          CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return postTransaction(requestBody);
    }

    private CreateResult postTransaction(JsonNode requestBody) throws Exception {
        var response = request()
                .body(requestBody.toString())
                .post("/transactions");
        return new CreateResult(response.statusCode(), objectMapper.readTree(response.asString()));
    }

    private record CreateResult(int statusCode, JsonNode body) {
    }
}
