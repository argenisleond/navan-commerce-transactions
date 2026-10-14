package com.navan.commerce.transactions.api;

import com.navan.commerce.transactions.dto.DuplicateTransactionResponse;
import com.navan.commerce.transactions.dto.ErrorResponse;
import com.navan.commerce.transactions.exception.DuplicateTransactionException;
import com.navan.commerce.transactions.exception.InvalidTransactionMetadataException;
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

@RestControllerAdvice(assignableTypes = TransactionController.class)
@RequiredArgsConstructor
public class TransactionExceptionHandler {

    private final TransactionService transactionService;

    @ExceptionHandler(DuplicateTransactionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public DuplicateTransactionResponse handleDuplicate(DuplicateTransactionException exception) {
        var existingId = findExistingId(exception.getExternalId());
        return new DuplicateTransactionResponse(existingId, exception.getMessage());
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(TransactionNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(InvalidTransactionMetadataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidMetadata(
            InvalidTransactionMetadataException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(TransactionAlreadyInvoicedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleAlreadyInvoiced(
            TransactionAlreadyInvoicedException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(ConstraintViolationException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    private UUID findExistingId(String externalId) {
        return transactionService.findIdByExternalId(externalId)
                .orElseThrow(() -> new IllegalStateException(
                        "Conflicting transaction was not found for external_id " + externalId));
    }
}
