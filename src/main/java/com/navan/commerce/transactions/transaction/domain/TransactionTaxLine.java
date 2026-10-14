package com.navan.commerce.transactions.transaction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transaction_tax_lines")
@Getter
@Setter(AccessLevel.PROTECTED)
public class TransactionTaxLine {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(nullable = false)
    private String name;

    @Column(precision = 9, scale = 6)
    private BigDecimal rate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    protected TransactionTaxLine() {
    }

    TransactionTaxLine(Transaction transaction, int lineOrder, String name,
                             BigDecimal rate, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.transaction = transaction;
        this.lineOrder = lineOrder;
        this.name = name;
        this.rate = rate;
        this.amount = amount;
    }

}
