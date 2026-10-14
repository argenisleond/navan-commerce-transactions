package com.navan.commerce.transactions.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionInvoiceGuardIntegrationTest extends ApiIntegrationTest {

    @Test
    void rejectsUpdatingAnInvoicedTransactionAndPreservesBothRecords() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var invoice = createSingleTransactionInvoice(flight.id());
        var patch = objectMapper.createObjectNode().put("total", 999.99);

        var response = objectMapper.readTree(request()
                .body(patch.toString())
                .patch("/transactions/{id}", flight.id())
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .extract().asString());

        assertThat(response.path("message").textValue())
                .isEqualTo("Transaction " + flight.id() + " is already included in an invoice");
        var transaction = objectMapper.readTree(request()
                .get("/transactions/{id}", flight.id()).asString());
        assertThat(transaction.path("total").decimalValue()).isEqualByComparingTo("220.00");
        assertThat(getInvoice(invoice.path("id").textValue(), HttpStatus.OK)).isEqualTo(invoice);
    }

    @Test
    void rejectsDeletingAnInvoicedTransactionAndPreservesBothRecords() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var invoice = createSingleTransactionInvoice(flight.id());

        var response = objectMapper.readTree(request()
                .delete("/transactions/{id}", flight.id())
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .extract().asString());

        assertThat(response.path("message").textValue())
                .isEqualTo("Transaction " + flight.id() + " is already included in an invoice");
        request().get("/transactions/{id}", flight.id())
                .then().statusCode(HttpStatus.OK.value());
        assertThat(getInvoice(invoice.path("id").textValue(), HttpStatus.OK)).isEqualTo(invoice);
    }

    @Test
    void removedTransactionCanBeUpdatedAfterInvoiceReplacement() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var hotel = createStoredTransaction("hotel.json");
        var invoice = createSingleTransactionInvoice(flight.id());
        patchInvoice(invoice.path("id").textValue(), java.util.List.of(hotel.id()), HttpStatus.OK);

        request()
                .body(objectMapper.createObjectNode().put("total", 225.00).toString())
                .patch("/transactions/{id}", flight.id())
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void transactionsCanBeChangedAfterTheirOnlyInvoiceIsDeleted() throws Exception {
        var flight = createStoredTransaction("flight.json");
        var invoice = createSingleTransactionInvoice(flight.id());
        request().delete("/invoices/{id}", invoice.path("id").textValue())
                .then().statusCode(HttpStatus.NO_CONTENT.value());

        request()
                .body(objectMapper.createObjectNode().put("total", 225.00).toString())
                .patch("/transactions/{id}", flight.id())
                .then()
                .statusCode(HttpStatus.OK.value());
    }
}
