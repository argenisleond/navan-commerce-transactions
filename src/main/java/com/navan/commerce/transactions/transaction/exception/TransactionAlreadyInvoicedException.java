package com.navan.commerce.transactions.transaction.exception;

import java.util.UUID;

public class TransactionAlreadyInvoicedException extends RuntimeException {

    public TransactionAlreadyInvoicedException(UUID id) {
        super("Transaction " + id + " is already included in an invoice");
    }
}
