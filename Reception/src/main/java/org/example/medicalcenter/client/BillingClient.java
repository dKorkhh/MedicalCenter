package org.example.medicalcenter.client;

import org.example.medicalcenter.client.dto.InvoiceDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

@HttpExchange("/api/invoices")
public interface BillingClient {

    @GetExchange("/{id}")
    InvoiceDto getInvoiceById(@PathVariable("id") String id,
                              @RequestParam(value = "delay", required = false, defaultValue = "0") long delay);

    @PostExchange("/batch")
    List<InvoiceDto> getInvoicesBatch(@RequestBody List<String> ids);
}
