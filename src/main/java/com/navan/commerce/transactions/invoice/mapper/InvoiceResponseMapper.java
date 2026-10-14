package com.navan.commerce.transactions.invoice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.commerce.transactions.invoice.domain.Invoice;
import com.navan.commerce.transactions.invoice.domain.InvoiceLine;
import com.navan.commerce.transactions.invoice.dto.InvoicePageResponse;
import com.navan.commerce.transactions.invoice.dto.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InvoiceResponseMapper {

    private final ObjectMapper objectMapper;

    public InvoicePageResponse toPage(List<Invoice> invoices, int limit,
                                      long offset, boolean hasMore) {
        return new InvoicePageResponse(
                invoices.stream().map(this::toResponse).toList(), limit, offset, hasMore);
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        var lines = invoice.getLines().stream().map(this::toLine).toList();
        var taxRollups = invoice.getTaxRollups().stream()
                .map(rollup -> new InvoiceResponse.TaxLine(
                        rollup.getName(), rollup.getRate(), rollup.getAmount()))
                .toList();
        var feeRollups = invoice.getFeeRollups().stream()
                .map(rollup -> new InvoiceResponse.FeeLine(
                        rollup.getName(), rollup.getAmount()))
                .toList();
        return new InvoiceResponse(
                invoice.getId(), invoice.getCurrency(), invoice.getTotal(),
                lines, taxRollups, feeRollups);
    }

    private InvoiceResponse.Line toLine(InvoiceLine line) {
        var taxLines = line.getTaxLines().stream()
                .map(tax -> new InvoiceResponse.TaxLine(
                        tax.getName(), tax.getRate(), tax.getAmount()))
                .toList();
        var feeLines = line.getFeeLines().stream()
                .map(fee -> new InvoiceResponse.FeeLine(fee.getName(), fee.getAmount()))
                .toList();
        try {
            return new InvoiceResponse.Line(
                    line.getSourceTransactionId(),
                    line.getType(),
                    line.getExternalId(),
                    line.getCurrency(),
                    line.getOccurredAt(),
                    line.getTotal(),
                    taxLines,
                    feeLines,
                    objectMapper.readTree(line.getMetadataJson())
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read invoice metadata snapshot", exception);
        }
    }
}
