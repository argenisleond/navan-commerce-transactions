package com.navan.commerce.transactions.service;

import com.navan.commerce.transactions.domain.InvoiceLineRepository;
import com.navan.commerce.transactions.domain.Transaction;
import com.navan.commerce.transactions.domain.TransactionRepository;
import com.navan.commerce.transactions.dto.CreateTransactionRequest;
import com.navan.commerce.transactions.dto.PatchTransactionRequest;
import com.navan.commerce.transactions.dto.TransactionPageResponse;
import com.navan.commerce.transactions.dto.TransactionResponse;
import com.navan.commerce.transactions.exception.DuplicateTransactionException;
import com.navan.commerce.transactions.exception.TransactionNotFoundException;
import com.navan.commerce.transactions.exception.TransactionAlreadyInvoicedException;
import com.navan.commerce.transactions.mapper.TransactionRequestMapper;
import com.navan.commerce.transactions.mapper.TransactionResponseMapper;
import com.navan.commerce.transactions.pagination.OffsetPageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final TransactionRequestMapper requestMapper;
    private final TransactionResponseMapper responseMapper;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        var transaction = requestMapper.toTransaction(request);
        try {
            repository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException exception) {
            if (DuplicateTransactionException.isExternalIdConflict(exception)) {
                throw new DuplicateTransactionException(request.externalId(), exception);
            }
            throw exception;
        }

        return responseMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findIdByExternalId(String externalId) {
        return repository.findByExternalId(externalId).map(Transaction::getId);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID id) {
        return responseMapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse list(int limit, long offset) {
        var sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
        var page = repository.findAllBy(new OffsetPageRequest(offset, limit, sort));
        return responseMapper.toPage(page.getContent(), limit, offset, page.hasNext());
    }

    @Transactional
    public TransactionResponse update(UUID id, PatchTransactionRequest request) {
        var transaction = repository.findLockedById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        ensureNotInvoiced(id);

        requestMapper.update(transaction, request);

        repository.flush();
        return responseMapper.toResponse(transaction);
    }

    @Transactional
    public void delete(UUID id) {
        var transaction = repository.findLockedById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        ensureNotInvoiced(id);
        repository.delete(transaction);
        repository.flush();
    }

    private void ensureNotInvoiced(UUID id) {
        if (invoiceLineRepository.existsBySourceTransactionId(id)) {
            throw new TransactionAlreadyInvoicedException(id);
        }
    }

}
