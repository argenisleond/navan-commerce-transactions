package com.navan.commerce.transactions.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.navan.commerce.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String currency,
        BigDecimal total,
        List<Line> lines,
        List<TaxLineResponse> taxRollup,
        List<FeeLineResponse> feeRollup
) {

    public record Line(
            UUID transactionId,
            TransactionType type,
            String externalId,
            String currency,
            Instant occurredAt,
            BigDecimal total,
            List<TaxLineResponse> taxLines,
            List<FeeLineResponse> feeLines,
            JsonNode metadata
    ) {
    }
}
