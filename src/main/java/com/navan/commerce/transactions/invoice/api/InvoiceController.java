package com.navan.commerce.transactions.invoice.api;

import com.navan.commerce.transactions.invoice.dto.InvoicePageResponse;
import com.navan.commerce.transactions.invoice.dto.InvoiceRequest;
import com.navan.commerce.transactions.invoice.dto.InvoiceResponse;
import com.navan.commerce.transactions.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Validated
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody InvoiceRequest request) {
        return invoiceService.create(request);
    }

    @GetMapping("/{id}")
    public InvoiceResponse getById(@PathVariable UUID id) {
        return invoiceService.getById(id);
    }

    @GetMapping
    public InvoicePageResponse list(
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) long offset) {
        return invoiceService.list(limit, offset);
    }

    @PatchMapping("/{id}")
    public InvoiceResponse update(@PathVariable UUID id,
                                  @Valid @RequestBody InvoiceRequest request) {
        return invoiceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        invoiceService.delete(id);
    }
}
