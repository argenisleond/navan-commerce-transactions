package com.navan.commerce.transactions.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {

    boolean existsBySourceTransactionId(UUID sourceTransactionId);
}
