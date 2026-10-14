package com.navan.commerce.transactions.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PatchTransactionRequest(
        Instant occurredAt,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal total,
        @Valid List<CreateTransactionRequest.TaxLine> taxLines,
        @Valid List<CreateTransactionRequest.FeeLine> feeLines,
        @Valid
        @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
        @JsonSubTypes({
                @JsonSubTypes.Type(CreateTransactionRequest.FlightMetadata.class),
                @JsonSubTypes.Type(CreateTransactionRequest.HotelMetadata.class),
                @JsonSubTypes.Type(CreateTransactionRequest.RailMetadata.class),
                @JsonSubTypes.Type(CreateTransactionRequest.NavanFeeMetadata.class)
        })
        CreateTransactionRequest.Metadata metadata
) {

    public PatchTransactionRequest {
        taxLines = taxLines == null ? null : List.copyOf(taxLines);
        feeLines = feeLines == null ? null : List.copyOf(feeLines);
    }
}
