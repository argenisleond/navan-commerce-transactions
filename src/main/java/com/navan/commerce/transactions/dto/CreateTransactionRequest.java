package com.navan.commerce.transactions.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.navan.commerce.transactions.domain.NavanFeeKind;
import com.navan.commerce.transactions.domain.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record CreateTransactionRequest(
        @NotNull TransactionType type,
        @NotBlank @Size(max = 255) String externalId,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull Instant occurredAt,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal total,
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
            @NotBlank @Size(max = 255) String name,
            @DecimalMin("0.00") @Digits(integer = 3, fraction = 6) BigDecimal rate,
            @NotNull @Digits(integer = 17, fraction = 2) BigDecimal amount
    ) {
    }

    public record FeeLine(
            @NotBlank @Size(max = 255) String name,
            @NotNull @Digits(integer = 17, fraction = 2) BigDecimal amount
    ) {
    }

    public sealed interface Metadata
            permits FlightMetadata, HotelMetadata, RailMetadata, NavanFeeMetadata {
    }

    public record FlightMetadata(
            @NotBlank @Size(max = 255) String origin,
            @NotBlank @Size(max = 255) String destination,
            @NotNull @Digits(integer = 17, fraction = 2) BigDecimal fare,
            @NotEmpty List<@Valid FlightCoupon> coupons
    ) implements Metadata {
        public FlightMetadata {
            coupons = List.copyOf(Objects.requireNonNull(coupons));
        }
    }

    public record FlightCoupon(
            @NotBlank @Size(max = 64) String flightNumber,
            @Size(max = 64) String carrier,
            @NotBlank @Size(max = 255) String from,
            @NotBlank @Size(max = 255) String to,
            @NotNull Instant departure,
            @NotNull Instant arrival
    ) {
    }

    public record HotelMetadata(
            @NotBlank @Size(max = 255) String name,
            @Valid @NotNull Address address,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Min(1) int nights,
            @NotNull @Digits(integer = 17, fraction = 2) BigDecimal roomRatePerNight,
            List<@Valid HotelExtra> extras
    ) implements Metadata {
        public HotelMetadata {
            extras = extras == null ? List.of() : List.copyOf(extras);
        }
    }

    public record Address(
            @NotBlank @Size(max = 255) String street,
            @NotBlank @Size(max = 255) String city,
            @NotBlank @Size(max = 255) String country
    ) {
    }

    public record HotelExtra(
            @NotBlank @Size(max = 255) String name,
            @NotNull @Digits(integer = 17, fraction = 2) BigDecimal amount
    ) {
    }

    public record RailMetadata(@NotEmpty List<@Valid RailLeg> legs) implements Metadata {
        public RailMetadata {
            legs = List.copyOf(Objects.requireNonNull(legs));
        }
    }

    public record RailLeg(
            @NotBlank @Size(max = 255) String origin,
            @NotBlank @Size(max = 255) String destination,
            @NotNull Instant departure,
            @NotNull Instant arrival,
            @Size(max = 64) String trainNumber,
            @JsonProperty("class") @Size(max = 64) String travelClass
    ) {
    }

    public record NavanFeeMetadata(
            @NotNull NavanFeeKind feeKind,
            @Size(max = 1000) String description,
            @Size(max = 255) String relatedExternalId
    ) implements Metadata {
    }
}
