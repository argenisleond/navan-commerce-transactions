package com.navan.commerce.transactions.transaction.service;

public class DuplicateTransactionException extends RuntimeException {

    private final String externalId;

    public DuplicateTransactionException(String externalId, Throwable cause) {
        super("Transaction already exists for external_id " + externalId, cause);
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }

}
