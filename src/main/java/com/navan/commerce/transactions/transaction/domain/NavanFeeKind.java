package com.navan.commerce.transactions.transaction.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum NavanFeeKind {
    @JsonProperty("trip_fee")
    TRIP_FEE,
    @JsonProperty("agent_call_fee")
    AGENT_CALL_FEE
}
