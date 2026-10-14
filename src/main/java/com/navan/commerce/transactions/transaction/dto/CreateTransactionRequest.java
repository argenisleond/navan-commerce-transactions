package com.navan.commerce.transactions.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.navan.commerce.transactions.transaction.domain.NavanFeeKind;
import com.navan.commerce.transactions.transaction.domain.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record CreateTransactionRequest(
        @NotNull TransactionType type,
        @NotBlank String externalId,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull Instant occurredAt,
        @NotNull @DecimalMin("0.00") BigDecimal total,
        @NotNull List<@Valid TaxLine> taxLines,
        @NotNull List<@Valid FeeLine> feeLines,
        @Valid @NotNull
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = FlightMetadata.class, name = "flight"),
                @JsonSubTypes.Type(value = HotelMetadata.class, name = "hotel"),
                @JsonSubTypes.Type(value = RailMetadata.class, name = "rail"),
                @JsonSubTypes.Type(value = NavanFeeMetadata.class, name = "navan_fee")
        })
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

    public record TaxLine(
            @NotBlank String name,
            @DecimalMin("0.00") BigDecimal rate,
            @NotNull BigDecimal amount
    ) {
    }

    public record FeeLine(@NotBlank String name, @NotNull BigDecimal amount) {
    }

    public sealed interface Metadata
            permits FlightMetadata, HotelMetadata, RailMetadata, NavanFeeMetadata {
    }

    public record FlightMetadata(
            @NotBlank String origin,
            @NotBlank String destination,
            @NotNull BigDecimal fare,
            @NotEmpty List<@Valid FlightCoupon> coupons
    ) implements Metadata {
        public FlightMetadata {
            coupons = List.copyOf(Objects.requireNonNull(coupons));
        }
    }

    public record FlightCoupon(
            @NotBlank String flightNumber,
            String carrier,
            @NotBlank String from,
            @NotBlank String to,
            @NotNull Instant departure,
            @NotNull Instant arrival
    ) {
    }

    public record HotelMetadata(
            @NotBlank String name,
            @Valid @NotNull Address address,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Min(1) int nights,
            @NotNull BigDecimal roomRatePerNight,
            List<@Valid HotelExtra> extras
    ) implements Metadata {
        public HotelMetadata {
            extras = extras == null ? List.of() : List.copyOf(extras);
        }
    }

    public record Address(
            @NotBlank String street,
            @NotBlank String city,
            @NotBlank String country
    ) {
    }

    public record HotelExtra(@NotBlank String name, @NotNull BigDecimal amount) {
    }

    public record RailMetadata(@NotEmpty List<@Valid RailLeg> legs) implements Metadata {
        public RailMetadata {
            legs = List.copyOf(Objects.requireNonNull(legs));
        }
    }

    public record RailLeg(
            @NotBlank String origin,
            @NotBlank String destination,
            @NotNull Instant departure,
            @NotNull Instant arrival,
            String trainNumber,
            @JsonProperty("class") String travelClass
    ) {
    }

    public record NavanFeeMetadata(
            @NotNull NavanFeeKind feeKind,
            String description,
            String relatedExternalId
    ) implements Metadata {
    }
}
