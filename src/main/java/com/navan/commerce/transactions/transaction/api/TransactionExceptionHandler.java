package com.navan.commerce.transactions.transaction.api;

import com.navan.commerce.transactions.transaction.dto.DuplicateTransactionResponse;
import com.navan.commerce.transactions.transaction.dto.TransactionErrorResponse;
import com.navan.commerce.transactions.transaction.exception.DuplicateTransactionException;
import com.navan.commerce.transactions.transaction.exception.InvalidTransactionMetadataException;
import com.navan.commerce.transactions.transaction.exception.TransactionNotFoundException;
import com.navan.commerce.transactions.transaction.service.TransactionService;
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
    public TransactionErrorResponse handleNotFound(TransactionNotFoundException exception) {
        return new TransactionErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(InvalidTransactionMetadataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public TransactionErrorResponse handleInvalidMetadata(
            InvalidTransactionMetadataException exception) {
        return new TransactionErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public TransactionErrorResponse handleValidation(ConstraintViolationException exception) {
        return new TransactionErrorResponse(exception.getMessage());
    }

    private UUID findExistingId(String externalId) {
        return transactionService.findIdByExternalId(externalId)
                .orElseThrow(() -> new IllegalStateException(
                        "Conflicting transaction was not found for external_id " + externalId));
    }
}
