package com.navan.commerce.transactions.api;

import com.navan.commerce.transactions.dto.InvoiceErrorResponse;
import com.navan.commerce.transactions.exception.DuplicateInvoiceTransactionException;
import com.navan.commerce.transactions.exception.InvalidInvoiceRequestException;
import com.navan.commerce.transactions.exception.InvoiceNotFoundException;
import com.navan.commerce.transactions.exception.MixedInvoiceCurrencyException;
import com.navan.commerce.transactions.exception.TransactionNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = InvoiceController.class)
public class InvoiceExceptionHandler {

    @ExceptionHandler({InvoiceNotFoundException.class, TransactionNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public InvoiceErrorResponse handleNotFound(RuntimeException exception) {
        return new InvoiceErrorResponse(exception.getMessage());
    }

    @ExceptionHandler({DuplicateInvoiceTransactionException.class,
            MixedInvoiceCurrencyException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public InvoiceErrorResponse handleConflict(RuntimeException exception) {
        return new InvoiceErrorResponse(exception.getMessage());
    }

    @ExceptionHandler({InvalidInvoiceRequestException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public InvoiceErrorResponse handleInvalidRequest(RuntimeException exception) {
        return new InvoiceErrorResponse(exception.getMessage());
    }
}
