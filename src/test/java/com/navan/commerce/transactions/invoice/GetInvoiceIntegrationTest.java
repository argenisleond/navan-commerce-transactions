package com.navan.commerce.transactions.invoice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetInvoiceIntegrationTest extends InvoiceIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsTheCompleteStoredInvoice() throws Exception {
        var transactions = createTripTransactions();
        var created = createInvoice(transactions.ids());

        var stored = getInvoice(created.path("id").textValue(), HttpStatus.OK);

        assertThat(stored).isEqualTo(created);
    }

    @Test
    void invoiceRemainsFrozenWhenTheStoredTransactionChanges() throws Exception {
        var flight = createTransaction("flight.json");
        var invoice = createSingleTransactionInvoice(flight.id());

        jdbcTemplate.update("UPDATE transactions SET total = ? WHERE id = ?",
                999.99, UUID.fromString(flight.id()));

        var stored = getInvoice(invoice.path("id").textValue(), HttpStatus.OK);
        assertThat(stored.path("total").decimalValue()).isEqualByComparingTo("220.00");
        assertThat(stored.at("/lines/0/total").decimalValue()).isEqualByComparingTo("220.00");
    }

    @Test
    void returnsNotFoundForAnUnknownInvoice() throws Exception {
        var id = UUID.randomUUID();

        var response = getInvoice(id.toString(), HttpStatus.NOT_FOUND);

        assertThat(response.path("message").textValue())
                .isEqualTo("Invoice not found for id " + id);
    }
}
