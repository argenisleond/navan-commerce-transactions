package com.navan.commerce.transactions.invoice.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Invoice> findLockedById(UUID id);

    Slice<Invoice> findAllBy(Pageable pageable);
}
