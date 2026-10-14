package com.navan.commerce.transactions.invoice.exception;

import java.util.UUID;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(UUID id) {
        super("Invoice not found for id " + id);
    }
}
