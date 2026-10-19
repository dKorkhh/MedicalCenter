package org.example.billing.mapper;

import org.example.billing.dto.CreateInvoiceRequest;
import org.example.billing.dto.InvoiceResponse;
import org.example.billing.model.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.LocalDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class})
public interface InvoiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentStatus", constant = "PENDING")
    @Mapping(target = "issuedAt", expression = "java(LocalDateTime.now())")
    Invoice toEntity(CreateInvoiceRequest request);

    InvoiceResponse toResponse(Invoice invoice);
}