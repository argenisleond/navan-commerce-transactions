package com.navan.commerce.transactions.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateTransactionExceptionTest {

    @Test
    void identifiesTheExternalIdConstraintAnywhereInTheCauseChain() {
        var constraintViolation = new ConstraintViolationException(
                "duplicate", new SQLException(), "uk_transactions_external_id");
        var wrapped = new IllegalStateException(new RuntimeException(constraintViolation));

        assertThat(DuplicateTransactionException.isExternalIdConflict(wrapped)).isTrue();
    }

    @Test
    void doesNotClassifyOtherIntegrityFailuresAsDuplicateTransactions() {
        var otherConstraint = new ConstraintViolationException(
                "failure", new SQLException(), "fk_invoice_lines_transaction");

        assertThat(DuplicateTransactionException.isExternalIdConflict(otherConstraint)).isFalse();
        assertThat(DuplicateTransactionException.isExternalIdConflict(
                new IllegalStateException("failure"))).isFalse();
    }
}
