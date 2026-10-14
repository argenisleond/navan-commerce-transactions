package com.navan.commerce.transactions.exception;

public class MixedInvoiceCurrencyException extends RuntimeException {

    public MixedInvoiceCurrencyException() {
        super("All transactions on an invoice must use the same currency");
    }
}
