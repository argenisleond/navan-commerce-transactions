package com.navan.commerce.transactions.integration;

import com.navan.commerce.transactions.domain.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteTransactionIntegrationTest extends ApiIntegrationTest {

    private static final String FIXTURE_PATH = "fixtures/transactions/";

    @Autowired
    private TransactionRepository transactionRepository;

    @ParameterizedTest
    @ValueSource(strings = {
            "flight.json",
            "hotel.json",
            "rail.json",
            "trip-fee.json",
            "agent-call-fee.json"
    })
    void deletesAnUnbilledTransactionAndItsChildren(String fixtureName) throws Exception {
        var requestBody = readFixture(FIXTURE_PATH + fixtureName);
        var id = request()
                .body(requestBody.toString())
                .post("/transactions")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().path("id");

        request()
                .delete("/transactions/{id}", id)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.NO_CONTENT.value());

        request()
                .get("/transactions/{id}", id)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void returnsNotFoundWhenDeletingAnUnknownTransaction() throws Exception {
        var id = UUID.randomUUID();

        var responseBody = objectMapper.readTree(request()
                .delete("/transactions/{id}", id)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .extract().asString());

        assertThat(responseBody.path("message").textValue())
                .isEqualTo("Transaction not found for id " + id);
    }

    @Test
    @Timeout(30)
    void overlappingUpdateAndDeleteLeaveNoPartialTransaction() throws Exception {
        var original = readFixture(FIXTURE_PATH + "flight.json");
        var patch = readFixture(FIXTURE_PATH + "patches/flight.json");
        var id = request()
                .body(original.toString())
                .post("/transactions")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().path("id");
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var update = executor.submit(() -> {
                ready.countDown();
                start.await();
                return request()
                        .body(patch.toString())
                        .patch("/transactions/{id}", id)
                        .statusCode();
            });
            var delete = executor.submit(() -> {
                ready.countDown();
                start.await();
                return request()
                        .delete("/transactions/{id}", id)
                        .statusCode();
            });

            ready.await();
            start.countDown();

            assertThat(update.get()).isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());
            assertThat(delete.get()).isEqualTo(HttpStatus.NO_CONTENT.value());
            assertThat(transactionRepository.count()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }
}
