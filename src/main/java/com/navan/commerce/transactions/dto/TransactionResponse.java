package com.navan.commerce.transactions.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.commerce.transactions.domain.NavanFeeKind;
import com.navan.commerce.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        String externalId,
        String currency,
        Instant occurredAt,
        BigDecimal total,
        List<TaxLineResponse> taxLines,
        List<FeeLineResponse> feeLines,
        Metadata metadata
){

    public sealed interface Metadata
            permits FlightMetadata, HotelMetadata, RailMetadata, NavanFeeMetadata {
    }

    public record FlightMetadata(
            String origin,
            String destination,
            BigDecimal fare,
            List<FlightCoupon> coupons
    ) implements Metadata {
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
    }

    public record Address(String street, String city, String country) {
    }

    public record HotelExtra(String name, BigDecimal amount) {
    }

    public record RailMetadata(List<RailLeg> legs) implements Metadata {
    }

    public record RailLeg(
            String origin,
            String destination,
            Instant departure,
            Instant arrival,
            String trainNumber,
            @JsonProperty("class") String travelClass
    ) {
    }

    public record NavanFeeMetadata(
            NavanFeeKind feeKind,
            String description,
            String relatedExternalId
    ) implements Metadata {
    }
}
