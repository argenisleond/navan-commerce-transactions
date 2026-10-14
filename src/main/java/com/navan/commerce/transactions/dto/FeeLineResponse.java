package com.navan.commerce.transactions.dto;

import java.math.BigDecimal;

public record FeeLineResponse(String name, BigDecimal amount) {
}
