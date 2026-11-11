package org.example.billing.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billing.grpc.proto.GrpcServiceQuoteItemRequest;
import org.example.billing.grpc.proto.GrpcServicesQuoteSummaryResponse;
import org.example.billing.mapper.BillingGrpcMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ServicesQuoteStreamObserver implements StreamObserver<GrpcServiceQuoteItemRequest> {

    private static final BigDecimal PACKAGE_DISCOUNT_RATE = new BigDecimal("0.10");

    private final StreamObserver<GrpcServicesQuoteSummaryResponse> responseObserver;
    private final BillingGrpcMapper billingGrpcMapper;

    private String patientId = "anonymous";
    private String currency = BillingGrpcMapper.DEFAULT_CURRENCY;
    private int totalItemsCount = 0;
    private int totalUnitsCount = 0;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private final List<String> includedServices = new ArrayList<>();

    @Override
    public void onNext(GrpcServiceQuoteItemRequest item) {
        log.info("[gRPC Server][ClientStreaming CalculateServicesQuote] Received item serviceName={}, price={}, qty={}",
                item.getServiceName(), item.getUnitPrice(), item.getQuantity());
        updateMetadataFromItem(item);
        accumulateItemCost(item);
    }

    @Override
    public void onError(Throwable throwable) {
        log.warn("[gRPC Server][ClientStreaming CalculateServicesQuote] Stream cancelled by client: {}", throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        BigDecimal discount = calculatePackageDiscount();
        BigDecimal finalTotal = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        GrpcServicesQuoteSummaryResponse summary = billingGrpcMapper.toQuoteSummaryResponse(
                patientId, totalItemsCount, totalUnitsCount, subtotal, discount, finalTotal, currency, includedServices);
        responseObserver.onNext(summary);
        responseObserver.onCompleted();
    }

    private void updateMetadataFromItem(GrpcServiceQuoteItemRequest item) {
        if (!item.getPatientId().isBlank()) {
            patientId = item.getPatientId();
        }
        if (!item.getCurrency().isBlank()) {
            currency = item.getCurrency();
        }
    }

    private void accumulateItemCost(GrpcServiceQuoteItemRequest item) {
        int quantity = Math.max(1, item.getQuantity());
        BigDecimal unitPrice = billingGrpcMapper.parseAmount(item.getUnitPrice());
        subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        totalItemsCount++;
        totalUnitsCount += quantity;
        includedServices.add(item.getServiceName() + " x" + quantity);
    }

    private BigDecimal calculatePackageDiscount() {
        if (totalUnitsCount < 3) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return subtotal.multiply(PACKAGE_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
