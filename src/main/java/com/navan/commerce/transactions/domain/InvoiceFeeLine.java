package com.navan.commerce.transactions.domain;

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
@Table(name = "invoice_fee_lines")
@Getter
@Setter(AccessLevel.PROTECTED)
public class InvoiceFeeLine {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_line_id", nullable = false)
    private InvoiceLine invoiceLine;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    protected InvoiceFeeLine() {
    }

    InvoiceFeeLine(InvoiceLine invoiceLine, int lineOrder, String name, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.invoiceLine = invoiceLine;
        this.lineOrder = lineOrder;
        update(name, amount);
    }

    void update(String name, BigDecimal amount) {
        this.name = name;
        this.amount = amount;
    }
}
