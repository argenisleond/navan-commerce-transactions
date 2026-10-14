package com.navan.commerce.transactions.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaxLineResponse(String name, BigDecimal rate, BigDecimal amount) {
}
