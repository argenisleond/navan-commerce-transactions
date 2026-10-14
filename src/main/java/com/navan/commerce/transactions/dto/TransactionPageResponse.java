package com.navan.commerce.transactions.dto;

import java.util.List;

public record TransactionPageResponse(
        List<TransactionResponse> items,
        int limit,
        long offset,
        boolean hasMore
) {

    public TransactionPageResponse {
        items = List.copyOf(items);
    }
}
