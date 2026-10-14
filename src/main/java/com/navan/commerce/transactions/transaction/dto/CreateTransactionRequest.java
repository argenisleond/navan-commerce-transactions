package com.navan.commerce.transactions.transaction.dto;

import com.navan.commerce.transactions.transaction.domain.NavanFeeKind;
import com.navan.commerce.transactions.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record CreateTransactionRequest(
        TransactionType type,
        String externalId,
        String currency,
        Instant occurredAt,
        BigDecimal total,
        List<TaxLine> taxLines,
        List<FeeLine> feeLines,
        Metadata metadata
) {

    public CreateTransactionRequest {
        Objects.requireNonNull(type);
        Objects.requireNonNull(externalId);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(total);
        taxLines = List.copyOf(Objects.requireNonNull(taxLines));
        feeLines = List.copyOf(Objects.requireNonNull(feeLines));
        Objects.requireNonNull(metadata);
    }

    public record TaxLine(String name, BigDecimal rate, BigDecimal amount) {
    }

    public record FeeLine(String name, BigDecimal amount) {
    }

    public sealed interface Metadata
            permits FlightMetadata, HotelMetadata, RailMetadata, NavanFeeMetadata {
    }

    public record FlightMetadata(
            String origin,
            String destination,
            BigDecimal fare,
            List<FlightCoupon> coupons
    ) implements Metadata {
        public FlightMetadata {
            coupons = List.copyOf(Objects.requireNonNull(coupons));
        }
    }

    public record FlightCoupon(
            String flightNumber,
            String carrier,
            String from,
            String to,
            Instant departure,
            Instant arrival
    ) {
    }

    public record HotelMetadata(
            String name,
            Address address,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal roomRatePerNight,
            List<HotelExtra> extras
    ) implements Metadata {
        public HotelMetadata {
            extras = extras == null ? List.of() : List.copyOf(extras);
        }
    }

    public record Address(String street, String city, String country) {
    }

    public record HotelExtra(String name, BigDecimal amount) {
    }

    public record RailMetadata(List<RailLeg> legs) implements Metadata {
        public RailMetadata {
            legs = List.copyOf(Objects.requireNonNull(legs));
        }
    }

    public record RailLeg(
            String origin,
            String destination,
            Instant departure,
            Instant arrival,
            String trainNumber,
            String travelClass
    ) {
    }

    public record NavanFeeMetadata(
            NavanFeeKind feeKind,
            String description,
            String relatedExternalId
    ) implements Metadata {
    }
}
