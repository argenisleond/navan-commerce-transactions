package com.navan.commerce.transactions.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.commerce.transactions.domain.Invoice;
import com.navan.commerce.transactions.domain.InvoiceLine;
import com.navan.commerce.transactions.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceResponseMapperTest {

    private final InvoiceResponseMapper mapper = new InvoiceResponseMapper(new ObjectMapper());

    @Test
    void mapsSnapshotLinesRollupsAndPageMetadata() {
        var invoice = invoice("{\"origin\":\"TXL\"}");

        var response = mapper.toResponse(invoice);
        var page = mapper.toPage(List.of(invoice), 10, 20, false);

        assertThat(response.id()).isEqualTo(invoice.getId());
        assertThat(response.total()).isEqualByComparingTo("220.00");
        assertThat(response.lines()).singleElement().satisfies(line -> {
            assertThat(line.externalId()).isEqualTo("flight-reference");
            assertThat(line.metadata().path("origin").textValue()).isEqualTo("TXL");
            assertThat(line.taxLines()).singleElement()
                    .satisfies(tax -> assertThat(tax.name()).isEqualTo("Airport tax"));
        });
        assertThat(response.taxRollup()).singleElement()
                .satisfies(tax -> assertThat(tax.amount()).isEqualByComparingTo("15.00"));
        assertThat(response.feeRollup()).singleElement()
                .satisfies(fee -> assertThat(fee.amount()).isEqualByComparingTo("25.00"));
        assertThat(page.items()).containsExactly(response);
        assertThat(page.limit()).isEqualTo(10);
        assertThat(page.offset()).isEqualTo(20);
    }

    @Test
    void rejectsAnUnreadableMetadataSnapshot() {
        var invoice = invoice("not-json");

        assertThatThrownBy(() -> mapper.toResponse(invoice))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not read invoice metadata snapshot");
    }

    private Invoice invoice(String metadataJson) {
        var line = new InvoiceLine.Values(
                java.util.UUID.randomUUID(),
                TransactionType.FLIGHT,
                "flight-reference",
                "EUR",
                MapperFixtures.OCCURRED_AT,
                new BigDecimal("220.00"),
                metadataJson,
                List.of(new InvoiceLine.TaxValues(
                        "Airport tax", null, new BigDecimal("15.00"))),
                List.of(new InvoiceLine.FeeValues("YQ", new BigDecimal("25.00"))));
        return new Invoice(new Invoice.Snapshot(
                "EUR",
                new BigDecimal("220.00"),
                List.of(line),
                List.of(new Invoice.TaxRollupValues(
                        "Airport tax", null, new BigDecimal("15.00"))),
                List.of(new Invoice.FeeRollupValues("YQ", new BigDecimal("25.00")))));
    }
}
