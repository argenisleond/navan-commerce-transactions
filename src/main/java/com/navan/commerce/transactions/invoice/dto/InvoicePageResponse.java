package com.navan.commerce.transactions.invoice.dto;

import java.util.List;

public record InvoicePageResponse(
        List<InvoiceResponse> items,
        int limit,
        long offset,
        boolean hasMore
) {
}
