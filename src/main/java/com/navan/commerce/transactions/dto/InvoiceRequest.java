package com.navan.commerce.transactions.dto;

import java.util.List;
import java.util.UUID;

public record InvoiceRequest(UUID transactionId, List<UUID> transactionIds) {

    public InvoiceRequest {
        transactionIds = transactionIds == null ? null : List.copyOf(transactionIds);
    }
}
