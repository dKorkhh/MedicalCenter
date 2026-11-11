package org.example.billing.mapper;

import org.example.billing.grpc.proto.GrpcBillingHistoryEvent;
import org.example.billing.grpc.proto.GrpcInvoiceLookupResponse;
import org.example.billing.grpc.proto.GrpcLiveValidationItemRequest;
import org.example.billing.grpc.proto.GrpcLiveValidationItemResponse;
import org.example.billing.grpc.proto.GrpcServicesQuoteSummaryResponse;
import org.example.billing.model.Invoice;
import org.example.billing.model.Refund;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        imports = {LocalDateTime.class}
)
public interface BillingGrpcMapper {

    String DEFAULT_CURRENCY = "UAH";

    @Mapping(target = "invoiceId", expression = "java(nullSafe(invoice.getId()))")
    @Mapping(target = "appointmentId", expression = "java(nullSafe(invoice.getAppointmentId()))")
    @Mapping(target = "patientId", expression = "java(nullSafe(invoice.getPatientId()))")
    @Mapping(target = "amount", expression = "java(formatAmount(invoice.getAmount()))")
    @Mapping(target = "currency", expression = "java(defaultCurrency(invoice.getCurrency()))")
    @Mapping(target = "paymentStatus", expression = "java(nullSafe(invoice.getPaymentStatus()))")
    @Mapping(target = "serverAttemptsUsed", source = "serverAttemptsUsed")
    @Mapping(target = "processedAt", expression = "java(LocalDateTime.now().toString())")
    GrpcInvoiceLookupResponse toInvoiceLookupResponse(Invoice invoice, int serverAttemptsUsed);

    @Mapping(target = "invoiceId", expression = "java(nullSafe(invoiceId))")
    @Mapping(target = "appointmentId", expression = "java(\"appt-verified-\" + nullSafe(invoiceId))")
    @Mapping(target = "patientId", constant = "patient-verified")
    @Mapping(target = "amount", constant = "500.00")
    @Mapping(target = "currency", constant = DEFAULT_CURRENCY)
    @Mapping(target = "paymentStatus", constant = "PENDING")
    @Mapping(target = "serverAttemptsUsed", source = "serverAttemptsUsed")
    @Mapping(target = "processedAt", expression = "java(LocalDateTime.now().toString())")
    GrpcInvoiceLookupResponse toFallbackInvoiceLookupResponse(String invoiceId, int serverAttemptsUsed);

    @Mapping(target = "recordType", constant = "INVOICE")
    @Mapping(target = "recordId", expression = "java(nullSafe(invoice.getId()))")
    @Mapping(target = "appointmentId", expression = "java(nullSafe(invoice.getAppointmentId()))")
    @Mapping(target = "patientId", expression = "java(nullSafe(invoice.getPatientId()))")
    @Mapping(target = "amount", expression = "java(formatAmount(invoice.getAmount()))")
    @Mapping(target = "currency", expression = "java(defaultCurrency(invoice.getCurrency()))")
    @Mapping(target = "status", expression = "java(nullSafe(invoice.getPaymentStatus()))")
    @Mapping(target = "description", constant = "Medical appointment invoice")
    @Mapping(target = "timestamp", expression = "java(formatTimestamp(invoice.getIssuedAt()))")
    GrpcBillingHistoryEvent toInvoiceHistoryEvent(Invoice invoice);

    @Mapping(target = "recordType", constant = "REFUND")
    @Mapping(target = "recordId", expression = "java(nullSafe(refund.getId()))")
    @Mapping(target = "appointmentId", expression = "java(nullSafe(refund.getAppointmentId()))")
    @Mapping(target = "patientId", expression = "java(nullSafe(refund.getPatientId()))")
    @Mapping(target = "amount", expression = "java(formatAmount(refund.getAmount()))")
    @Mapping(target = "currency", expression = "java(defaultCurrency(refund.getCurrency()))")
    @Mapping(target = "status", expression = "java(nullSafe(refund.getStatus()))")
    @Mapping(target = "description", expression = "java(defaultText(refund.getReason(), \"Appointment cancellation refund\"))")
    @Mapping(target = "timestamp", expression = "java(formatTimestamp(refund.getProcessedAt()))")
    GrpcBillingHistoryEvent toRefundHistoryEvent(Refund refund);

    @Mapping(target = "recordType", constant = "ACCOUNT_SNAPSHOT")
    @Mapping(target = "recordId", expression = "java(\"snapshot-\" + nullSafe(patientId))")
    @Mapping(target = "appointmentId", constant = "N/A")
    @Mapping(target = "patientId", expression = "java(nullSafe(patientId))")
    @Mapping(target = "amount", constant = "0.00")
    @Mapping(target = "currency", constant = DEFAULT_CURRENCY)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "description", constant = "No persisted invoices or refunds found; billing account is active")
    @Mapping(target = "timestamp", expression = "java(LocalDateTime.now().toString())")
    GrpcBillingHistoryEvent toEmptyAccountSnapshotEvent(String patientId);

    @Mapping(target = "stepId", expression = "java(nullSafe(item.getStepId()))")
    @Mapping(target = "invoiceId", expression = "java(nullSafe(item.getInvoiceId()))")
    @Mapping(target = "serviceName", expression = "java(nullSafe(item.getServiceName()))")
    @Mapping(target = "valid", source = "valid")
    @Mapping(target = "validationStatus", expression = "java(nullSafe(validationStatus))")
    @Mapping(target = "runningTotal", expression = "java(formatAmount(runningTotal))")
    @Mapping(target = "currency", expression = "java(defaultCurrency(item.getCurrency()))")
    @Mapping(target = "message", expression = "java(nullSafe(message))")
    GrpcLiveValidationItemResponse toLiveValidationResponse(
            GrpcLiveValidationItemRequest item,
            boolean valid,
            String validationStatus,
            BigDecimal runningTotal,
            String message);

    default GrpcServicesQuoteSummaryResponse toQuoteSummaryResponse(
            String patientId,
            int totalItemsCount,
            int totalUnitsCount,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal finalTotal,
            String currency,
            List<String> includedServices) {
        return GrpcServicesQuoteSummaryResponse.newBuilder()
                .setPatientId(nullSafe(patientId))
                .setTotalItemsCount(totalItemsCount)
                .setTotalUnitsCount(totalUnitsCount)
                .setSubtotalAmount(formatAmount(subtotal))
                .setDiscountAmount(formatAmount(discount))
                .setFinalTotalAmount(formatAmount(finalTotal))
                .setCurrency(defaultCurrency(currency))
                .addAllIncludedServices(includedServices != null ? includedServices : List.of())
                .build();
    }

    default String nullSafe(String value) {
        return value != null ? value : "";
    }

    @Named("defaultCurrency")
    default String defaultCurrency(String currency) {
        return currency != null && !currency.isBlank() ? currency : DEFAULT_CURRENCY;
    }

    default String defaultText(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    default String formatAmount(BigDecimal amount) {
        return amount != null ? amount.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
    }

    default String formatTimestamp(LocalDateTime timestamp) {
        return (timestamp != null ? timestamp : LocalDateTime.now()).toString();
    }

    default BigDecimal parseAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(rawAmount);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
