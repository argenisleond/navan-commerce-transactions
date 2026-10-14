package com.navan.commerce.transactions.dto;

import java.util.UUID;

public record DuplicateTransactionResponse(UUID id, String message) {
}
