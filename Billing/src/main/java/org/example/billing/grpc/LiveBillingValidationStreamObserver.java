package org.example.billing.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billing.grpc.proto.GrpcLiveValidationItemRequest;
import org.example.billing.grpc.proto.GrpcLiveValidationItemResponse;
import org.example.billing.mapper.BillingGrpcMapper;
import org.example.billing.model.Invoice;
import org.example.billing.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class LiveBillingValidationStreamObserver implements StreamObserver<GrpcLiveValidationItemRequest> {

    private final StreamObserver<GrpcLiveValidationItemResponse> responseObserver;
    private final InvoiceRepository invoiceRepository;
    private final BillingGrpcMapper billingGrpcMapper;

    private BigDecimal runningTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Override
    public void onNext(GrpcLiveValidationItemRequest item) {
        log.info("[gRPC Server][BidiStreaming LiveBillingValidation] Validating stepId={}, invoiceId={}, serviceName={}, amount={}",
                item.getStepId(), item.getInvoiceId(), item.getServiceName(), item.getRequestedAmount());

        BigDecimal amount = billingGrpcMapper.parseAmount(item.getRequestedAmount());
        boolean valid = amount.compareTo(BigDecimal.ZERO) > 0;
        if (valid) {
            runningTotal = runningTotal.add(amount).setScale(2, RoundingMode.HALF_UP);
        }

        Optional<Invoice> matchedInvoice = findMatchedInvoice(item, valid);
        String status = resolveValidationStatus(valid, matchedInvoice.orElse(null));
        String message = resolveValidationMessage(item, valid, matchedInvoice.orElse(null));

        responseObserver.onNext(billingGrpcMapper.toLiveValidationResponse(
                item, valid, status, runningTotal, message));
    }

    @Override
    public void onError(Throwable throwable) {
        log.warn("[gRPC Server][BidiStreaming LiveBillingValidation] Stream cancelled by client: {}", throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        responseObserver.onCompleted();
    }

    private Optional<Invoice> findMatchedInvoice(GrpcLiveValidationItemRequest item, boolean valid) {
        if (!valid || item.getInvoiceId().isBlank() || invoiceRepository == null) {
            return Optional.empty();
        }
        return invoiceRepository.findById(item.getInvoiceId());
    }

    private String resolveValidationStatus(boolean valid, Invoice matchedInvoice) {
        if (!valid) {
            return "INVALID_AMOUNT";
        }
        if (matchedInvoice != null) {
            return "MATCHED_INVOICE_" + matchedInvoice.getPaymentStatus();
        }
        return "VALIDATED";
    }

    private String resolveValidationMessage(GrpcLiveValidationItemRequest item, boolean valid, Invoice matchedInvoice) {
        if (!valid) {
            return "Requested amount must be strictly positive";
        }
        if (matchedInvoice != null) {
            return "Matched existing invoice " + item.getInvoiceId() + " with status " + matchedInvoice.getPaymentStatus();
        }
        return "Service item verified and added to live checkout balance";
    }
}
