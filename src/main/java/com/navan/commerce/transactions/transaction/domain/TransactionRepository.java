package com.navan.commerce.transactions.transaction.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByExternalId(String externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Transaction> findLockedById(UUID id);

    Slice<Transaction> findAllBy(Pageable pageable);
}
