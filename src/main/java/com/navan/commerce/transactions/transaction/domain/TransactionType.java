package com.navan.commerce.transactions.transaction.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TransactionType {
    @JsonProperty("flight")
    FLIGHT,
    @JsonProperty("hotel")
    HOTEL,
    @JsonProperty("rail")
    RAIL,
    @JsonProperty("navan_fee")
    NAVAN_FEE
}
