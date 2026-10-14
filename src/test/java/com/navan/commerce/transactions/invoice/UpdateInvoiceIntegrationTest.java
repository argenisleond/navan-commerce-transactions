package com.navan.commerce.transactions.invoice;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.navan.commerce.transactions.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateInvoiceIntegrationTest extends ApiIntegrationTest {

    @Test
    void replacesTheTransactionSetAndRebuildsTheSnapshotAndRollups() throws Exception {
        var trip = createTripTransactions();
        var invoice = createSingleTransactionInvoice(trip.flight().id());

        var updated = patchInvoice(invoice.path("id").textValue(),
                List.of(trip.hotel().id(), trip.rail().id()), HttpStatus.OK);

        assertThat(updated.path("total").decimalValue()).isEqualByComparingTo("425.70");
        assertThat(lineTransactionIds(updated))
                .containsExactly(trip.hotel().id(), trip.rail().id());
        assertThat(updated.path("tax_rollup").findValuesAsText("name"))
                .containsExactly("VAT");
        assertThat(updated.path("fee_rollup").findValuesAsText("name"))
                .containsExactly("Booking fee");
        assertThat(getInvoice(invoice.path("id").textValue(), HttpStatus.OK)).isEqualTo(updated);
    }

    @Test
    void returnsNotFoundForAnUnknownInvoice() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var invoiceId = UUID.randomUUID();

        var response = patchInvoice(invoiceId.toString(), List.of(flight.id()),
                HttpStatus.NOT_FOUND);

        assertThat(response.path("message").textValue())
                .isEqualTo("Invoice not found for id " + invoiceId);
    }

    @Test
    void missingReplacementTransactionLeavesTheInvoiceUnchanged() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var invoice = createSingleTransactionInvoice(flight.id());
        var missingId = UUID.randomUUID().toString();

        patchInvoice(invoice.path("id").textValue(), List.of(missingId), HttpStatus.NOT_FOUND);

        assertThat(getInvoice(invoice.path("id").textValue(), HttpStatus.OK)).isEqualTo(invoice);
    }

    @Test
    void mixedCurrencyReplacementLeavesTheInvoiceUnchanged() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var invoice = createSingleTransactionInvoice(flight.id());
        var hotelBody = (ObjectNode) readFixture(TRANSACTION_FIXTURE_PATH + "hotel.json");
        hotelBody.put("currency", "USD");
        hotelBody.put("external_id", "hotel-usd");
        var hotel = createStoredTransaction(hotelBody);

        patchInvoice(invoice.path("id").textValue(), List.of(flight.id(), hotel.id()),
                HttpStatus.CONFLICT);

        assertThat(getInvoice(invoice.path("id").textValue(), HttpStatus.OK)).isEqualTo(invoice);
    }

    @Test
    void repeatedReplacementIdLeavesTheInvoiceUnchanged() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var invoice = createSingleTransactionInvoice(flight.id());

        patchInvoice(invoice.path("id").textValue(), List.of(flight.id(), flight.id()),
                HttpStatus.CONFLICT);

        assertThat(getInvoice(invoice.path("id").textValue(), HttpStatus.OK)).isEqualTo(invoice);
    }

    @Test
    void acceptsTheSingleTransactionRequestShape() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var hotel = createStoredTransaction("hotel.json");
        var invoice = createSingleTransactionInvoice(flight.id());
        var requestBody = objectMapper.createObjectNode().put("transaction_id", hotel.id());

        var response = request().body(requestBody.toString())
                .patch("/invoices/{id}", invoice.path("id").textValue())
                .then().statusCode(HttpStatus.OK.value()).extract().asString();
        var updated = objectMapper.readTree(response);

        assertThat(lineTransactionIds(updated)).containsExactly(hotel.id());
        assertThat(updated.path("total").decimalValue()).isEqualByComparingTo("331.70");
    }

    @Test
    @Timeout(30)
    void concurrentReplacementsNeverMixSnapshots() throws Exception {
        var trip = createTripTransactions();
        var invoice = createSingleTransactionInvoice(trip.flight().id());
        var firstIds = List.of(trip.flight().id(), trip.hotel().id());
        var secondIds = List.of(trip.rail().id(), trip.tripFee().id(), trip.agentCallFee().id());
        var ready = new CountDownLatch(10);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(10);

        try {
            var tasks = IntStream.range(0, 10)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return request()
                                .body(multipleTransactionBody(index % 2 == 0 ? firstIds : secondIds)
                                        .toString())
                                .patch("/invoices/{id}", invoice.path("id").textValue())
                                .statusCode();
                    }))
                    .toList();

            ready.await();
            start.countDown();
            assertThat(tasks).allSatisfy(task -> assertThat(task.get())
                    .isEqualTo(HttpStatus.OK.value()));

            var stored = getInvoice(invoice.path("id").textValue(), HttpStatus.OK);
            var storedIds = lineTransactionIds(stored);
            assertThat(storedIds.equals(firstIds) || storedIds.equals(secondIds)).isTrue();
            assertThat(stored.path("total").decimalValue())
                    .isIn(new java.math.BigDecimal("551.70"), new java.math.BigDecimal("134.00"));
        } finally {
            executor.shutdownNow();
        }
    }
}
