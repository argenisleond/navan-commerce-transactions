package com.navan.commerce.transactions.invoice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.navan.commerce.transactions.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String currency,
        BigDecimal total,
        List<Line> lines,
        List<TaxLine> taxRollup,
        List<FeeLine> feeRollup
) {

    public record Line(
            UUID transactionId,
            TransactionType type,
            String externalId,
            String currency,
            Instant occurredAt,
            BigDecimal total,
            List<TaxLine> taxLines,
            List<FeeLine> feeLines,
            JsonNode metadata
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaxLine(String name, BigDecimal rate, BigDecimal amount) {
    }

    public record FeeLine(String name, BigDecimal amount) {
    }
}
