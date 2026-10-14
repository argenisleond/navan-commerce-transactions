package com.navan.commerce.transactions.mapper;

import com.navan.commerce.transactions.domain.TransactionType;
import com.navan.commerce.transactions.dto.CreateTransactionRequest;
import com.navan.commerce.transactions.dto.PatchTransactionRequest;
import com.navan.commerce.transactions.exception.InvalidTransactionMetadataException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionRequestMapperTest {

    private final TransactionRequestMapper mapper = new TransactionRequestMapper();

    @Test
    void mapsARequestIncludingCollectionsAndTypedMetadata() {
        var request = flightRequest();

        var transaction = mapper.toTransaction(request);

        assertThat(transaction.getExternalId()).isEqualTo(request.externalId());
        assertThat(transaction.getType()).isEqualTo(TransactionType.FLIGHT);
        assertThat(transaction.getTotal()).isEqualByComparingTo("220.00");
        assertThat(transaction.getTaxLines()).singleElement().satisfies(line -> {
            assertThat(line.getName()).isEqualTo("Airport tax");
            assertThat(line.getAmount()).isEqualByComparingTo("15.00");
        });
        assertThat(transaction.getFeeLines()).singleElement()
                .satisfies(line -> assertThat(line.getName()).isEqualTo("YQ"));
        assertThat(transaction.getFlightDetails().getCoupons()).singleElement()
                .satisfies(coupon -> assertThat(coupon.getFlightNumber()).isEqualTo("BA982"));
    }

    @Test
    void updatesOnlySuppliedFieldsAndReplacesNestedCollections() {
        var transaction = mapper.toTransaction(flightRequest());
        var replacementMetadata = new CreateTransactionRequest.FlightMetadata(
                "LHR", "JFK", new BigDecimal("300.00"),
                List.of(new CreateTransactionRequest.FlightCoupon(
                        "BA117", "BA", "LHR", "JFK",
                        Instant.parse("2026-04-01T08:00:00Z"),
                        Instant.parse("2026-04-01T16:00:00Z"))));
        var patch = new PatchTransactionRequest(
                null,
                new BigDecimal("340.00"),
                List.of(),
                List.of(new CreateTransactionRequest.FeeLine(
                        "Booking fee", new BigDecimal("10.00"))),
                replacementMetadata);

        mapper.update(transaction, patch);

        assertThat(transaction.getOccurredAt()).isEqualTo(MapperFixtures.OCCURRED_AT);
        assertThat(transaction.getTotal()).isEqualByComparingTo("340.00");
        assertThat(transaction.getTaxLines()).isEmpty();
        assertThat(transaction.getFeeLines()).singleElement()
                .satisfies(line -> assertThat(line.getName()).isEqualTo("Booking fee"));
        assertThat(transaction.getFlightDetails().getOrigin()).isEqualTo("LHR");
        assertThat(transaction.getFlightDetails().getCoupons()).singleElement()
                .satisfies(coupon -> assertThat(coupon.getFlightNumber()).isEqualTo("BA117"));
    }

    @Test
    void rejectsMetadataThatDoesNotMatchTheStoredTransactionType() {
        var transaction = mapper.toTransaction(flightRequest());
        var railMetadata = new CreateTransactionRequest.RailMetadata(
                List.of(new CreateTransactionRequest.RailLeg(
                        "London", "Paris",
                        Instant.parse("2026-03-13T09:00:00Z"),
                        Instant.parse("2026-03-13T12:00:00Z"),
                        "EST9108", "standard")));
        var patch = new PatchTransactionRequest(null, null, null, null, railMetadata);

        assertThatThrownBy(() -> mapper.update(transaction, patch))
                .isInstanceOf(InvalidTransactionMetadataException.class)
                .hasMessage("Metadata does not match transaction type FLIGHT");
    }

    private CreateTransactionRequest flightRequest() {
        var metadata = new CreateTransactionRequest.FlightMetadata(
                "TXL", "LHR", new BigDecimal("180.00"),
                List.of(new CreateTransactionRequest.FlightCoupon(
                        "BA982", "BA", "TXL", "LHR",
                        Instant.parse("2026-03-11T07:00:00Z"),
                        Instant.parse("2026-03-11T08:20:00Z"))));
        return new CreateTransactionRequest(
                TransactionType.FLIGHT,
                "flight-reference",
                "EUR",
                MapperFixtures.OCCURRED_AT,
                new BigDecimal("220.00"),
                List.of(new CreateTransactionRequest.TaxLine(
                        "Airport tax", null, new BigDecimal("15.00"))),
                List.of(new CreateTransactionRequest.FeeLine("YQ", new BigDecimal("25.00"))),
                metadata);
    }
}
