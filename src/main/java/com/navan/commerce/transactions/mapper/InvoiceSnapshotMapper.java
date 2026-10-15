package com.navan.commerce.transactions.mapper;

import com.navan.commerce.transactions.domain.Invoice;
import com.navan.commerce.transactions.domain.InvoiceLine;
import com.navan.commerce.transactions.exception.MixedInvoiceCurrencyException;
import com.navan.commerce.transactions.domain.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InvoiceSnapshotMapper {

    private final InvoiceMetadataSnapshotMapper metadataSnapshotMapper;

    public Invoice.Snapshot toSnapshot(List<Transaction> transactions) {
        var currency = transactions.getFirst().getCurrency();
        if (transactions.stream().anyMatch(transaction ->
                !currency.equals(transaction.getCurrency()))) {
            throw new MixedInvoiceCurrencyException();
        }

        var total = transactions.stream()
                .map(Transaction::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var lines = transactions.stream().map(this::toLine).toList();
        return new Invoice.Snapshot(
                currency, total, lines, taxRollups(transactions), feeRollups(transactions));
    }

    private InvoiceLine.Values toLine(Transaction transaction) {
        var taxLines = transaction.getTaxLines().stream()
                .map(line -> new InvoiceLine.TaxValues(
                        line.getName(), line.getRate(), line.getAmount()))
                .toList();
        var feeLines = transaction.getFeeLines().stream()
                .map(line -> new InvoiceLine.FeeValues(line.getName(), line.getAmount()))
                .toList();
        return new InvoiceLine.Values(
                transaction.getId(),
                transaction.getType(),
                transaction.getExternalId(),
                transaction.getCurrency(),
                transaction.getOccurredAt(),
                transaction.getTotal(),
                metadataSnapshotMapper.toJson(transaction),
                taxLines,
                feeLines
        );
    }

    private List<Invoice.TaxRollupValues> taxRollups(List<Transaction> transactions) {
        var amounts = new LinkedHashMap<TaxKey, BigDecimal>();
        transactions.stream()
                .flatMap(transaction -> transaction.getTaxLines().stream())
                .forEach(line -> amounts.merge(
                        new TaxKey(line.getName(), line.getRate()),
                        line.getAmount(),
                        BigDecimal::add));
        return amounts.entrySet().stream()
                .map(entry -> new Invoice.TaxRollupValues(
                        entry.getKey().name(), entry.getKey().rate(), entry.getValue()))
                .toList();
    }

    private List<Invoice.FeeRollupValues> feeRollups(List<Transaction> transactions) {
        var amounts = new LinkedHashMap<String, BigDecimal>();
        transactions.stream()
                .flatMap(transaction -> transaction.getFeeLines().stream())
                .forEach(line -> amounts.merge(
                        line.getName(), line.getAmount(), BigDecimal::add));
        return amounts.entrySet().stream()
                .map(entry -> new Invoice.FeeRollupValues(entry.getKey(), entry.getValue()))
                .toList();
    }

    private record TaxKey(String name, BigDecimal rate) {
    }
}
