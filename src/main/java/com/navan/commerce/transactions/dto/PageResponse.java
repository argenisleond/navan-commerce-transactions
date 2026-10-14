package com.navan.commerce.transactions.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int limit,
        long offset,
        boolean hasMore
) {

    public PageResponse {
        items = List.copyOf(items);
    }
}
