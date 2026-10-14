package com.navan.commerce.transactions.transaction.dto;

import java.util.UUID;

public record DuplicateTransactionResponse(UUID id, String message) {
}
