package com.navan.commerce.transactions.exception;

import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;

public class DuplicateTransactionException extends RuntimeException {

    private static final String EXTERNAL_ID_CONSTRAINT = "UK_TRANSACTIONS_EXTERNAL_ID";

    private final String externalId;

    public DuplicateTransactionException(String externalId, Throwable cause) {
        super("Transaction already exists for external_id " + externalId, cause);
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public static boolean isExternalIdConflict(Throwable exception) {
        var cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                var constraintName = constraintViolation.getConstraintName();
                return constraintName != null
                        && constraintName.toUpperCase(Locale.ROOT)
                        .contains(EXTERNAL_ID_CONSTRAINT);
            }
            cause = cause.getCause();
        }
        return false;
    }

}
