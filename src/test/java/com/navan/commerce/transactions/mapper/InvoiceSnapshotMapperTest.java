package com.navan.commerce.transactions.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.navan.commerce.transactions.exception.MixedInvoiceCurrencyException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceSnapshotMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final InvoiceSnapshotMapper mapper = new InvoiceSnapshotMapper(
            objectMapper, new TransactionResponseMapper());

    @Test
    void snapshotsLinesAndBuildsIndependentTaxAndFeeRollups() throws Exception {
        var first = MapperFixtures.flight("first-flight", "EUR", "220.00");
        var second = MapperFixtures.flight("second-flight", "EUR", "230.00");

        var snapshot = mapper.toSnapshot(List.of(first, second));

        assertThat(snapshot.currency()).isEqualTo("EUR");
        assertThat(snapshot.total()).isEqualByComparingTo("450.00");
        assertThat(snapshot.lines()).hasSize(2);
        assertThat(snapshot.taxRollups()).singleElement().satisfies(rollup -> {
            assertThat(rollup.name()).isEqualTo("Airport tax");
            assertThat(rollup.amount()).isEqualByComparingTo("30.00");
        });
        assertThat(snapshot.feeRollups()).singleElement().satisfies(rollup -> {
            assertThat(rollup.name()).isEqualTo("YQ");
            assertThat(rollup.amount()).isEqualByComparingTo("50.00");
        });
        var metadata = objectMapper.readTree(snapshot.lines().getFirst().metadataJson());
        assertThat(metadata.path("origin").textValue()).isEqualTo("TXL");
        assertThat(metadata.path("coupons").get(0).path("flight_number").textValue())
                .isEqualTo("BA982");
    }

    @Test
    void rejectsMixedCurrencies() {
        var eur = MapperFixtures.flight("eur-flight", "EUR", "220.00");
        var usd = MapperFixtures.hotel("usd-hotel", "USD", "331.70");

        assertThatThrownBy(() -> mapper.toSnapshot(List.of(eur, usd)))
                .isInstanceOf(MixedInvoiceCurrencyException.class);
    }
}
