package com.navan.commerce.transactions.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoice_lines")
@Getter
@Setter(AccessLevel.PROTECTED)
public class InvoiceLine {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(name = "source_transaction_id", nullable = false)
    private UUID sourceTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionType type;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "metadata_json", nullable = false, columnDefinition = "CLOB")
    private String metadataJson;

    @OneToMany(mappedBy = "invoiceLine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<InvoiceTaxLine> taxLines = new ArrayList<>();

    @OneToMany(mappedBy = "invoiceLine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<InvoiceFeeLine> feeLines = new ArrayList<>();

    protected InvoiceLine() {
    }

    InvoiceLine(Invoice invoice, int lineOrder, Values values) {
        this.id = UUID.randomUUID();
        this.invoice = invoice;
        this.lineOrder = lineOrder;
        replace(values);
    }

    void replace(Values values) {
        this.sourceTransactionId = values.sourceTransactionId();
        this.type = values.type();
        this.externalId = values.externalId();
        this.currency = values.currency();
        this.occurredAt = values.occurredAt();
        this.total = values.total();
        this.metadataJson = values.metadataJson();
        replaceTaxLines(values.taxLines());
        replaceFeeLines(values.feeLines());
    }

    public List<InvoiceTaxLine> getTaxLines() {
        return List.copyOf(taxLines);
    }

    public List<InvoiceFeeLine> getFeeLines() {
        return List.copyOf(feeLines);
    }

    private void replaceTaxLines(List<TaxValues> replacements) {
        var retained = Math.min(taxLines.size(), replacements.size());
        for (var index = 0; index < retained; index++) {
            var value = replacements.get(index);
            taxLines.get(index).update(value.name(), value.rate(), value.amount());
        }
        while (taxLines.size() > replacements.size()) {
            taxLines.removeLast();
        }
        for (var index = retained; index < replacements.size(); index++) {
            var value = replacements.get(index);
            taxLines.add(new InvoiceTaxLine(
                    this, index, value.name(), value.rate(), value.amount()));
        }
    }

    private void replaceFeeLines(List<FeeValues> replacements) {
        var retained = Math.min(feeLines.size(), replacements.size());
        for (var index = 0; index < retained; index++) {
            var value = replacements.get(index);
            feeLines.get(index).update(value.name(), value.amount());
        }
        while (feeLines.size() > replacements.size()) {
            feeLines.removeLast();
        }
        for (var index = retained; index < replacements.size(); index++) {
            var value = replacements.get(index);
            feeLines.add(new InvoiceFeeLine(this, index, value.name(), value.amount()));
        }
    }

    public record Values(
            UUID sourceTransactionId,
            TransactionType type,
            String externalId,
            String currency,
            Instant occurredAt,
            BigDecimal total,
            String metadataJson,
            List<TaxValues> taxLines,
            List<FeeValues> feeLines
    ) {
    }

    public record TaxValues(String name, BigDecimal rate, BigDecimal amount) {
    }

    public record FeeValues(String name, BigDecimal amount) {
    }
}
