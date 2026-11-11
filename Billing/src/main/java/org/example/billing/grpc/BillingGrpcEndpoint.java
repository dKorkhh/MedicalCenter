package org.example.billing.grpc;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billing.grpc.proto.BillingGrpcServiceGrpc;
import org.example.billing.grpc.proto.GrpcBillingHistoryEvent;
import org.example.billing.grpc.proto.GrpcInvoiceLookupRequest;
import org.example.billing.grpc.proto.GrpcInvoiceLookupResponse;
import org.example.billing.grpc.proto.GrpcLiveValidationItemRequest;
import org.example.billing.grpc.proto.GrpcLiveValidationItemResponse;
import org.example.billing.grpc.proto.GrpcPatientBillingStreamRequest;
import org.example.billing.grpc.proto.GrpcServiceQuoteItemRequest;
import org.example.billing.grpc.proto.GrpcServicesQuoteSummaryResponse;
import org.example.billing.mapper.BillingGrpcMapper;
import org.example.billing.model.Invoice;
import org.example.billing.model.Refund;
import org.example.billing.repository.InvoiceRepository;
import org.example.billing.repository.RefundRepository;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class BillingGrpcEndpoint extends BillingGrpcServiceGrpc.BillingGrpcServiceImplBase {

    private final InvoiceRepository invoiceRepository;
    private final RefundRepository refundRepository;
    private final BillingGrpcMapper billingGrpcMapper;
    private final ConcurrentHashMap<String, AtomicInteger> attemptCountersByRequest = new ConcurrentHashMap<>();

    @Override
    public void getInvoiceWithRetry(
            GrpcInvoiceLookupRequest request,
            StreamObserver<GrpcInvoiceLookupResponse> responseObserver) {
        String trackingKey = resolveTrackingKey(request);
        int currentAttempt = incrementAttempt(trackingKey);
        log.info("[gRPC Server][Unary GetInvoiceWithRetry] requestId={}, invoiceId={}, attempt={}, failFirstAttempts={}",
                trackingKey, request.getInvoiceId(), currentAttempt, request.getFailFirstAttempts());

        if (currentAttempt <= request.getFailFirstAttempts()) {
            failWithSimulatedUnavailable(responseObserver, trackingKey, currentAttempt, request.getFailFirstAttempts());
            return;
        }

        attemptCountersByRequest.remove(trackingKey);
        responseObserver.onNext(resolveInvoiceLookupResponse(request.getInvoiceId(), currentAttempt));
        responseObserver.onCompleted();
    }

    @Override
    public void streamPatientBillingHistory(
            GrpcPatientBillingStreamRequest request,
            StreamObserver<GrpcBillingHistoryEvent> responseObserver) {
        String patientId = request.getPatientId();
        log.info("[gRPC Server][ServerStreaming StreamPatientBillingHistory] Streaming billing history for patientId={}", patientId);

        List<Invoice> invoices = invoiceRepository != null ? invoiceRepository.findByPatientId(patientId) : List.of();
        List<Refund> refunds = refundRepository != null ? refundRepository.findByPatientId(patientId) : List.of();

        streamEventsUnlessCancelled(invoices.stream().map(billingGrpcMapper::toInvoiceHistoryEvent).toList(), responseObserver);
        streamEventsUnlessCancelled(refunds.stream().map(billingGrpcMapper::toRefundHistoryEvent).toList(), responseObserver);

        if (invoices.isEmpty() && refunds.isEmpty() && !Context.current().isCancelled()) {
            responseObserver.onNext(billingGrpcMapper.toEmptyAccountSnapshotEvent(patientId));
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<GrpcServiceQuoteItemRequest> calculateServicesQuote(
            StreamObserver<GrpcServicesQuoteSummaryResponse> responseObserver) {
        return new ServicesQuoteStreamObserver(responseObserver, billingGrpcMapper);
    }

    @Override
    public StreamObserver<GrpcLiveValidationItemRequest> liveBillingValidation(
            StreamObserver<GrpcLiveValidationItemResponse> responseObserver) {
        return new LiveBillingValidationStreamObserver(responseObserver, invoiceRepository, billingGrpcMapper);
    }

    private void streamEventsUnlessCancelled(
            List<GrpcBillingHistoryEvent> events,
            StreamObserver<GrpcBillingHistoryEvent> responseObserver) {
        for (GrpcBillingHistoryEvent event : events) {
            if (Context.current().isCancelled()) {
                return;
            }
            responseObserver.onNext(event);
        }
    }

    private int incrementAttempt(String trackingKey) {
        return attemptCountersByRequest
                .computeIfAbsent(trackingKey, key -> new AtomicInteger(0))
                .incrementAndGet();
    }

    private void failWithSimulatedUnavailable(
            StreamObserver<GrpcInvoiceLookupResponse> responseObserver,
            String trackingKey,
            int currentAttempt,
            int failFirstAttempts) {
        log.warn("[gRPC Server][Unary GetInvoiceWithRetry] Simulating UNAVAILABLE on attempt {} of {} for requestId={}",
                currentAttempt, failFirstAttempts, trackingKey);
        responseObserver.onError(Status.UNAVAILABLE
                .withDescription("Simulated temporary Billing gRPC unavailability on attempt " + currentAttempt)
                .asRuntimeException());
    }

    private GrpcInvoiceLookupResponse resolveInvoiceLookupResponse(String invoiceId, int attemptsUsed) {
        Optional<Invoice> foundInvoice = (invoiceRepository != null && !invoiceId.isBlank())
                ? invoiceRepository.findById(invoiceId)
                : Optional.empty();
        return foundInvoice
                .map(invoice -> billingGrpcMapper.toInvoiceLookupResponse(invoice, attemptsUsed))
                .orElseGet(() -> billingGrpcMapper.toFallbackInvoiceLookupResponse(invoiceId, attemptsUsed));
    }

    private String resolveTrackingKey(GrpcInvoiceLookupRequest request) {
        if (!request.getRequestId().isBlank()) {
            return request.getRequestId();
        }
        return !request.getInvoiceId().isBlank() ? request.getInvoiceId() : "default-request";
    }
}
