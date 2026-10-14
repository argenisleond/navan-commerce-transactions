package com.navan.commerce.transactions.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.navan.commerce.transactions.transaction.domain.TransactionRepository;
import com.navan.commerce.transactions.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateTransactionIntegrationTest extends ApiIntegrationTest {

    private static final String FLIGHT_FIXTURE = "fixtures/transactions/flight.json";

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
        assertThat(transactionRepository.count()).isOne();
    }

    @Test
    @Timeout(15)
    void concurrentCreatesWithTheSameExternalIdStoreOneTransaction() throws Exception {
        var requestBody = readFixture(FLIGHT_FIXTURE);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var tasks = List.of(
                    executor.submit(() -> postWhenReleased(requestBody, ready, start)),
                    executor.submit(() -> postWhenReleased(requestBody, ready, start))
            );

            ready.await();
            start.countDown();

            var responses = tasks.stream().map(task -> {
                try {
                    return task.get();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }).toList();

            assertThat(responses).extracting(CreateResult::statusCode)
                    .containsExactlyInAnyOrder(
                            HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
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
