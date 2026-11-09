package org.example.billing.mapper;

import org.example.billing.dto.RefundResponse;
import org.example.billing.event.AppointmentCancelledEvent;
import org.example.billing.model.Refund;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class})
public interface RefundMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventId", source = "event.eventId")
    @Mapping(target = "appointmentId", source = "event.appointmentId")
    @Mapping(target = "invoiceId", source = "resolvedInvoiceId")
    @Mapping(target = "patientId", source = "event.patientId")
    @Mapping(target = "amount", source = "resolvedAmount")
    @Mapping(target = "currency", source = "resolvedCurrency")
    @Mapping(target = "status", constant = "PROCESSED")
    @Mapping(target = "cancelledBy", source = "event.cancelledBy")
    @Mapping(target = "reason", source = "event.cancellationReason")
    @Mapping(target = "processedAt", expression = "java(LocalDateTime.now())")
    Refund toEntity(AppointmentCancelledEvent event, String resolvedInvoiceId, BigDecimal resolvedAmount, String resolvedCurrency);

    RefundResponse toResponse(Refund refund);

    List<RefundResponse> toResponseList(List<Refund> refunds);
}
