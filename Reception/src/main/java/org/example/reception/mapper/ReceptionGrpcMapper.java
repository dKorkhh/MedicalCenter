package org.example.reception.mapper;

import org.example.reception.dto.GrpcBillingHistoryEventDto;
import org.example.reception.dto.GrpcInvoiceDto;
import org.example.reception.dto.GrpcLiveValidationItemRequestDto;
import org.example.reception.dto.GrpcLiveValidationItemResponseDto;
import org.example.reception.dto.GrpcServiceQuoteItemDto;
import org.example.reception.dto.GrpcServicesQuoteSummaryDto;
import org.example.reception.dto.GrpcUnaryInvoiceResultDto;
import org.example.reception.grpc.proto.GrpcBillingHistoryEvent;
import org.example.reception.grpc.proto.GrpcInvoiceLookupRequest;
import org.example.reception.grpc.proto.GrpcInvoiceLookupResponse;
import org.example.reception.grpc.proto.GrpcLiveValidationItemRequest;
import org.example.reception.grpc.proto.GrpcLiveValidationItemResponse;
import org.example.reception.grpc.proto.GrpcPatientBillingStreamRequest;
import org.example.reception.grpc.proto.GrpcServiceQuoteItemRequest;
import org.example.reception.grpc.proto.GrpcServicesQuoteSummaryResponse;
import org.example.reception.model.ClinicServiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        imports = {List.class, Math.class}
)
public interface ReceptionGrpcMapper {

    String DEFAULT_CURRENCY = "UAH";

    @Mapping(target = "requestId", expression = "java(nullSafe(requestId))")
    @Mapping(target = "invoiceId", expression = "java(nullSafe(invoiceId))")
    @Mapping(target = "failFirstAttempts", expression = "java(Math.max(0, failFirstAttempts))")
    GrpcInvoiceLookupRequest toInvoiceLookupRequest(String requestId, String invoiceId, int failFirstAttempts);

    @Mapping(target = "patientId", expression = "java(nullSafe(patientId))")
    GrpcPatientBillingStreamRequest toPatientBillingStreamRequest(String patientId);

    GrpcInvoiceDto toInvoiceDto(GrpcInvoiceLookupResponse response);

    GrpcBillingHistoryEventDto toHistoryEventDto(GrpcBillingHistoryEvent event);

    @Mapping(target = "includedServices", expression = "java(List.copyOf(summary.getIncludedServicesList()))")
    GrpcServicesQuoteSummaryDto toQuoteSummaryDto(GrpcServicesQuoteSummaryResponse summary);

    GrpcLiveValidationItemResponseDto toLiveValidationResponseDto(GrpcLiveValidationItemResponse response);

    @Mapping(target = "patientId", expression = "java(defaultText(dto.patientId(), fallbackPatientId))")
    @Mapping(target = "serviceId", expression = "java(nullSafe(dto.serviceId()))")
    @Mapping(target = "serviceName", expression = "java(nullSafe(dto.serviceName()))")
    @Mapping(target = "unitPrice", expression = "java(nullSafe(dto.unitPrice()))")
    @Mapping(target = "quantity", expression = "java(dto.quantity() > 0 ? dto.quantity() : 1)")
    @Mapping(target = "currency", expression = "java(defaultCurrency(dto.currency()))")
    GrpcServiceQuoteItemRequest toServiceQuoteItemProto(GrpcServiceQuoteItemDto dto, String fallbackPatientId);

    @Mapping(target = "patientId", expression = "java(nullSafe(patientId))")
    @Mapping(target = "serviceId", expression = "java(nullSafe(serviceItem.getId()))")
    @Mapping(target = "serviceName", expression = "java(nullSafe(serviceItem.getName()))")
    @Mapping(target = "unitPrice", expression = "java(formatAmount(serviceItem.getPrice()))")
    @Mapping(target = "quantity", constant = "1")
    @Mapping(target = "currency", expression = "java(defaultCurrency(serviceItem.getCurrency()))")
    GrpcServiceQuoteItemRequest toServiceQuoteItemProto(ClinicServiceItem serviceItem, String patientId);

    @Mapping(target = "stepId", expression = "java(nullSafe(dto.stepId()))")
    @Mapping(target = "invoiceId", expression = "java(nullSafe(dto.invoiceId()))")
    @Mapping(target = "serviceName", expression = "java(nullSafe(dto.serviceName()))")
    @Mapping(target = "requestedAmount", expression = "java(nullSafe(dto.requestedAmount()))")
    @Mapping(target = "currency", expression = "java(defaultCurrency(dto.currency()))")
    GrpcLiveValidationItemRequest toLiveValidationProto(GrpcLiveValidationItemRequestDto dto);

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "invoice", source = "invoice")
    @Mapping(target = "requestedSimulatedFailures", source = "requestedSimulatedFailures")
    @Mapping(target = "totalAttemptsExecuted", expression = "java(invoice.serverAttemptsUsed())")
    @Mapping(target = "retryPolicyConfiguration", source = "retryPolicyConfiguration")
    @Mapping(target = "retryExecutionLogs", source = "retryExecutionLogs")
    GrpcUnaryInvoiceResultDto toUnaryResultDto(
            String requestId,
            GrpcInvoiceDto invoice,
            int requestedSimulatedFailures,
            Map<String, Object> retryPolicyConfiguration,
            List<String> retryExecutionLogs);

    default String nullSafe(String value) {
        return value != null ? value : "";
    }

    default String defaultText(String value, String fallback) {
        return value != null && !value.isBlank() ? value : nullSafe(fallback);
    }

    @Named("defaultCurrency")
    default String defaultCurrency(String currency) {
        return currency != null && !currency.isBlank() ? currency : DEFAULT_CURRENCY;
    }

    default String formatAmount(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "0.00";
    }
}
