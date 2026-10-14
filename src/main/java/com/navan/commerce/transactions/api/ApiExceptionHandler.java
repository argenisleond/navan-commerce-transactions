package com.navan.commerce.transactions.api;

import com.navan.commerce.transactions.dto.DuplicateTransactionResponse;
import com.navan.commerce.transactions.dto.ErrorResponse;
import com.navan.commerce.transactions.exception.DuplicateInvoiceTransactionException;
import com.navan.commerce.transactions.exception.DuplicateTransactionException;
import com.navan.commerce.transactions.exception.InvalidInvoiceRequestException;
import com.navan.commerce.transactions.exception.InvalidTransactionMetadataException;
import com.navan.commerce.transactions.exception.InvoiceNotFoundException;
import com.navan.commerce.transactions.exception.MixedInvoiceCurrencyException;
import com.navan.commerce.transactions.exception.TransactionAlreadyInvoicedException;
import com.navan.commerce.transactions.exception.TransactionNotFoundException;
import com.navan.commerce.transactions.service.TransactionService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final TransactionService transactionService;

    @ExceptionHandler(DuplicateTransactionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public DuplicateTransactionResponse handleDuplicateTransaction(
            DuplicateTransactionException exception) {
        var existingId = findExistingId(exception.getExternalId());
        return new DuplicateTransactionResponse(existingId, exception.getMessage());
    }

    @ExceptionHandler({InvoiceNotFoundException.class, TransactionNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(RuntimeException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler({DuplicateInvoiceTransactionException.class,
            MixedInvoiceCurrencyException.class, TransactionAlreadyInvoicedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(RuntimeException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler({InvalidInvoiceRequestException.class,
            InvalidTransactionMetadataException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidRequest(RuntimeException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    private UUID findExistingId(String externalId) {
        return transactionService.findIdByExternalId(externalId)
                .orElseThrow(() -> new IllegalStateException(
                        "Conflicting transaction was not found for external_id " + externalId));
    }
}
