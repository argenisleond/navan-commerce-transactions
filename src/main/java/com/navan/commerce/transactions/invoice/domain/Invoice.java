package com.navan.commerce.transactions.invoice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "invoices")
@Getter
@Setter(AccessLevel.PROTECTED)
public class Invoice {

    @Id
    private UUID id;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<InvoiceTaxRollup> taxRollups = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<InvoiceFeeRollup> feeRollups = new ArrayList<>();

    protected Invoice() {
    }

    public Invoice(Snapshot snapshot) {
        this.id = UUID.randomUUID();
        replace(snapshot);
    }

    public void replace(Snapshot snapshot) {
        this.currency = snapshot.currency();
        this.total = snapshot.total();
        replaceLines(snapshot.lines());
        replaceTaxRollups(snapshot.taxRollups());
        replaceFeeRollups(snapshot.feeRollups());
    }

    public List<InvoiceLine> getLines() {
        return List.copyOf(lines);
    }

    public List<InvoiceTaxRollup> getTaxRollups() {
        return List.copyOf(taxRollups);
    }

    public List<InvoiceFeeRollup> getFeeRollups() {
        return List.copyOf(feeRollups);
    }

    public List<UUID> getSourceTransactionIds() {
        return lines.stream().map(InvoiceLine::getSourceTransactionId).toList();
    }

    private void replaceLines(List<InvoiceLine.Values> replacements) {
        var retained = Math.min(lines.size(), replacements.size());
        for (var index = 0; index < retained; index++) {
            lines.get(index).replace(replacements.get(index));
        }
        while (lines.size() > replacements.size()) {
            lines.removeLast();
        }
        for (var index = retained; index < replacements.size(); index++) {
            lines.add(new InvoiceLine(this, index, replacements.get(index)));
        }
    }

    private void replaceTaxRollups(List<TaxRollupValues> replacements) {
        var retained = Math.min(taxRollups.size(), replacements.size());
        for (var index = 0; index < retained; index++) {
            var value = replacements.get(index);
            taxRollups.get(index).update(value.name(), value.rate(), value.amount());
        }
        while (taxRollups.size() > replacements.size()) {
            taxRollups.removeLast();
        }
        for (var index = retained; index < replacements.size(); index++) {
            var value = replacements.get(index);
            taxRollups.add(new InvoiceTaxRollup(
                    this, index, value.name(), value.rate(), value.amount()));
        }
    }

    private void replaceFeeRollups(List<FeeRollupValues> replacements) {
        var retained = Math.min(feeRollups.size(), replacements.size());
        for (var index = 0; index < retained; index++) {
            var value = replacements.get(index);
            feeRollups.get(index).update(value.name(), value.amount());
        }
        while (feeRollups.size() > replacements.size()) {
            feeRollups.removeLast();
        }
        for (var index = retained; index < replacements.size(); index++) {
            var value = replacements.get(index);
            feeRollups.add(new InvoiceFeeRollup(this, index, value.name(), value.amount()));
        }
    }

    public record Snapshot(
            String currency,
            BigDecimal total,
            List<InvoiceLine.Values> lines,
            List<TaxRollupValues> taxRollups,
            List<FeeRollupValues> feeRollups
    ) {
    }

    public record TaxRollupValues(String name, BigDecimal rate, BigDecimal amount) {
    }

    public record FeeRollupValues(String name, BigDecimal amount) {
    }
}
