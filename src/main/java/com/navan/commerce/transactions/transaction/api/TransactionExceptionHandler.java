package com.navan.commerce.transactions.transaction.api;

import com.navan.commerce.transactions.transaction.dto.DuplicateTransactionResponse;
import com.navan.commerce.transactions.transaction.service.DuplicateTransactionException;
import com.navan.commerce.transactions.transaction.service.TransactionService;
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

    private UUID findExistingId(String externalId) {
        return transactionService.findIdByExternalId(externalId)
                .orElseThrow(() -> new IllegalStateException(
                        "Conflicting transaction was not found for external_id " + externalId));
    }
}
