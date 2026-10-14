package com.navan.commerce.transactions.exception;

public class DuplicateInvoiceTransactionException extends RuntimeException {

    public DuplicateInvoiceTransactionException() {
        super("Transaction ids must be unique within an invoice");
    }
}
