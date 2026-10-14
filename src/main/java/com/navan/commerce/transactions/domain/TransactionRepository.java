package com.navan.commerce.transactions.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByExternalId(String externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Transaction> findLockedById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Transaction> findAllByIdInOrderByIdAsc(Collection<UUID> ids);

    Slice<Transaction> findAllBy(Pageable pageable);
}
