package com.navan.commerce.transactions.service;

import com.navan.commerce.transactions.domain.Invoice;
import com.navan.commerce.transactions.domain.InvoiceRepository;
import com.navan.commerce.transactions.dto.InvoicePageResponse;
import com.navan.commerce.transactions.dto.InvoiceRequest;
import com.navan.commerce.transactions.dto.InvoiceResponse;
import com.navan.commerce.transactions.exception.DuplicateInvoiceTransactionException;
import com.navan.commerce.transactions.exception.InvalidInvoiceRequestException;
import com.navan.commerce.transactions.exception.InvoiceNotFoundException;
import com.navan.commerce.transactions.mapper.InvoiceResponseMapper;
import com.navan.commerce.transactions.mapper.InvoiceSnapshotMapper;
import com.navan.commerce.transactions.pagination.OffsetPageRequest;
import com.navan.commerce.transactions.domain.Transaction;
import com.navan.commerce.transactions.domain.TransactionRepository;
import com.navan.commerce.transactions.exception.TransactionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceSnapshotMapper snapshotMapper;
    private final InvoiceResponseMapper responseMapper;

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        var transactionIds = transactionIds(request);
        var transactions = loadLockedTransactions(transactionIds);
        var invoice = new Invoice(snapshotMapper.toSnapshot(transactions));
        invoiceRepository.saveAndFlush(invoice);
        return responseMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(UUID id) {
        return responseMapper.toResponse(invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public InvoicePageResponse list(int limit, long offset) {
        var sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
        var page = invoiceRepository.findAllBy(new OffsetPageRequest(offset, limit, sort));
        return responseMapper.toPage(page.getContent(), limit, offset, page.hasNext());
    }

    @Transactional
    public InvoiceResponse update(UUID id, InvoiceRequest request) {
        var invoice = invoiceRepository.findLockedById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));
        var transactionIds = transactionIds(request);
        var lockIds = new LinkedHashSet<>(invoice.getSourceTransactionIds());
        lockIds.addAll(transactionIds);
        var lockedTransactions = loadLockedTransactions(List.copyOf(lockIds));
        var transactionsById = lockedTransactions.stream()
                .collect(Collectors.toMap(Transaction::getId, Function.identity()));
        var replacementTransactions = transactionIds.stream()
                .map(transactionId -> transaction(transactionsById, transactionId))
                .toList();

        invoice.replace(snapshotMapper.toSnapshot(replacementTransactions));
        invoiceRepository.flush();
        return responseMapper.toResponse(invoice);
    }

    @Transactional
    public void delete(UUID id) {
        var invoice = invoiceRepository.findLockedById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));
        invoiceRepository.delete(invoice);
        invoiceRepository.flush();
    }

    private List<UUID> transactionIds(InvoiceRequest request) {
        var hasSingleId = request.transactionId() != null;
        var hasMultipleIds = request.transactionIds() != null;
        if (hasSingleId == hasMultipleIds) {
            throw new InvalidInvoiceRequestException(
                    "Provide either transaction_id or transaction_ids");
        }

        var transactionIds = hasSingleId
                ? List.of(request.transactionId())
                : request.transactionIds();

        if (transactionIds.isEmpty()) {
            throw new InvalidInvoiceRequestException(
                    "At least one transaction id is required");
        }

        if (new LinkedHashSet<>(transactionIds).size() != transactionIds.size()) {
            throw new DuplicateInvoiceTransactionException();
        }
        return transactionIds;
    }

    private List<Transaction> loadLockedTransactions(List<UUID> transactionIds) {
        var transactions = transactionRepository.findAllByIdInOrderByIdAsc(transactionIds);
        var transactionsById = transactions.stream()
                .collect(Collectors.toMap(Transaction::getId, Function.identity()));
        return transactionIds.stream()
                .map(transactionId -> transaction(transactionsById, transactionId))
                .toList();
    }

    private Transaction transaction(Map<UUID, Transaction> transactionsById, UUID id) {
        var transaction = transactionsById.get(id);
        if (transaction == null) {
            throw new TransactionNotFoundException(id);
        }
        return transaction;
    }
}
