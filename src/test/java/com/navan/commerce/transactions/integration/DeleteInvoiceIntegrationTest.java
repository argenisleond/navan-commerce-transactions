package com.navan.commerce.transactions.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteInvoiceIntegrationTest extends ApiIntegrationTest {

    @Test
    void deletesTheInvoiceWithoutDeletingItsTransactions() throws Exception {
        var trip = createTripTransactions();
        var invoice = createInvoice(trip.ids());

        request()
                .delete("/invoices/{id}", invoice.path("id").textValue())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        getInvoice(invoice.path("id").textValue(), HttpStatus.NOT_FOUND);
        for (var transactionId : trip.ids()) {
            request().get("/transactions/{id}", transactionId)
                    .then().statusCode(HttpStatus.OK.value());
        }
    }

    @Test
    void returnsNotFoundWhenDeletingAnUnknownInvoice() {
        request()
                .delete("/invoices/{id}", UUID.randomUUID())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @Timeout(30)
    void overlappingReplacementAndDeleteLeaveNoPartialInvoice() throws Exception {
        var trip = createTripTransactions();
        var invoice = createSingleTransactionInvoice(trip.flight().id());
        var invoiceId = invoice.path("id").textValue();
        var replacement = multipleTransactionBody(List.of(trip.hotel().id(), trip.rail().id()));
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var update = executor.submit(() -> {
                ready.countDown();
                start.await();
                return request().body(replacement.toString())
                        .patch("/invoices/{id}", invoiceId).statusCode();
            });
            var delete = executor.submit(() -> {
                ready.countDown();
                start.await();
                return request().delete("/invoices/{id}", invoiceId).statusCode();
            });

            ready.await();
            start.countDown();

            assertThat(update.get()).isIn(HttpStatus.OK.value(), HttpStatus.NOT_FOUND.value());
            assertThat(delete.get()).isEqualTo(HttpStatus.NO_CONTENT.value());
            getInvoice(invoiceId, HttpStatus.NOT_FOUND);
            for (var transactionId : trip.ids()) {
                request().get("/transactions/{id}", transactionId)
                        .then().statusCode(HttpStatus.OK.value());
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
